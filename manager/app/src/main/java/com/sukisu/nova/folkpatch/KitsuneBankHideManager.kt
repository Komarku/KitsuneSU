package com.sukisu.nova.folkpatch

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.sukisu.nova.Natives
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class BankAppItem(
    val packageName: String,
    val label: String,
    val uid: Int,
    val packageInfo: PackageInfo,
    val isBankingApp: Boolean,
    val isHidden: Boolean,
)

/**
 * Менеджер скрытия root и разблокированного загрузчика от банковских приложений без перезагрузки.
 *
 * Механизмы:
 * 1. Изоляция пространства монтирования (Umount modules & su) через штатный AppProfile ядра (SukiSU / KernelSU)
 * 2. Исключение UID из хуков ядра через ApNatives (KernelPatch / APatch)
 * 3. RAM-спуфинг статуса Verified Boot (green / locked) через resetprop -n без записи на диск
 * 4. Мгновенный сброс кэша приложения (am force-stop + cache clear) для применения защиты БЕЗ перезагрузки телефона
 */
object KitsuneBankHideManager {
    private const val TAG = "KitsuneBankHide"
    private const val PREFS_NAME = "kitsune_bank_hide_prefs"
    private const val KEY_HIDDEN_PACKAGES = "hidden_packages"

    private val KNOWN_BANK_PATTERNS = listOf(
        "ru.sberbank", "ru.sber", "ru.nspk.mirpay", "ru.nspk.sbpay",
        "tinkoff", "tcsbank", "tbank", "ru.alfabank", "ru.vtb",
        "gazprombank", "sovcomcard", "sovcombank", "psbank", "raiffeisen",
        "ozon.fintech", "ru.ozon.app", "yoomoney", "qiwi", "rshb",
        "pochtabank", "mts.money", "rosbank", "uralsib", "openbank",
        "akbars", "finshell.wallet", "alipay", "unionpay",
        "wallet", "rostel", "gosuslugi", "koronapay", "avito",
        "integritycheck", "duckdetector", "rootbeer", "detector"
    )

    private val BANK_KEYWORDS = listOf(
        "банк", "bank", "сбер", "тинькоф", "т-банк", "альфа", "втб",
        "мир pay", "mir pay", "деньги", "кошелек", "wallet", "pay",
        "финанс", "finance", "инвест", "invest", "кредит", "credit",
        "займ", "госуслуг", "налог", "crypto", "крипт", "детектор", "detector"
    )

    fun getHiddenPackages(context: Context): Set<String> {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getStringSet(KEY_HIDDEN_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    private fun saveHiddenPackages(context: Context, set: Set<String>) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putStringSet(KEY_HIDDEN_PACKAGES, set).apply()
    }

    fun isAppHidden(context: Context, packageName: String): Boolean {
        return getHiddenPackages(context).contains(packageName)
    }

    fun getHiddenCount(context: Context): Int {
        return getHiddenPackages(context).size
    }

    fun isBankingApp(pkg: String, label: String, appInfo: ApplicationInfo): Boolean {
        val p = pkg.lowercase(Locale.ROOT)
        val l = label.lowercase(Locale.ROOT)

        if (KNOWN_BANK_PATTERNS.any { p.contains(it) }) return true
        if (BANK_KEYWORDS.any { l.contains(it) }) return true
        return false
    }

    suspend fun loadApps(context: Context): List<BankAppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val hiddenSet = getHiddenPackages(context)

        packages.mapNotNull { pkgInfo ->
            val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
            if (pkgInfo.packageName == context.packageName) return@mapNotNull null

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val hasLaunchIntent = pm.getLaunchIntentForPackage(pkgInfo.packageName) != null
            val isKnownBank = KNOWN_BANK_PATTERNS.any { pkgInfo.packageName.contains(it) }

            // Показываем сторонние приложения или системные с иконкой лаунчера / известные банки
            if (isSystem && !isUpdatedSystem && !hasLaunchIntent && !isKnownBank) {
                return@mapNotNull null
            }

            val label = runCatching { appInfo.loadLabel(pm).toString() }.getOrDefault(pkgInfo.packageName)
            val isBank = isBankingApp(pkgInfo.packageName, label, appInfo)
            val isHidden = hiddenSet.contains(pkgInfo.packageName)

            BankAppItem(
                packageName = pkgInfo.packageName,
                label = label,
                uid = appInfo.uid,
                packageInfo = pkgInfo,
                isBankingApp = isBank,
                isHidden = isHidden,
            )
        }.sortedWith(
            compareByDescending<BankAppItem> { it.isHidden }
                .thenByDescending { it.isBankingApp }
                .thenBy { it.label.lowercase(Locale.ROOT) }
        )
    }

    /**
     * Скрывает root и разблокированный загрузчик от приложения БЕЗ перезагрузки телефона.
     */
    suspend fun setAppHidden(
        context: Context,
        packageName: String,
        uid: Int,
        enable: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val currentSet = getHiddenPackages(context).toMutableSet()
        if (enable) {
            currentSet.add(packageName)
        } else {
            currentSet.remove(packageName)
        }
        saveHiddenPackages(context, currentSet)

        if (enable) {
            // 1. Устанавливаем изолированный профиль в ядре (SukiSU / KernelSU)
            runCatching {
                val profile = Natives.Profile(
                    name = packageName,
                    currentUid = uid,
                    allowSu = false,
                    nonRootUseDefault = false,
                    umountModules = true,
                )
                Natives.setAppProfile(profile)
            }

            // 2. Включаем глобальные флаги скрытия ядра
            runCatching {
                Natives.setKernelUmountEnabled(true)
                Natives.setMountHideEnabled(true)
                Natives.setSelinuxHideEnabled(true)
                Natives.setAvcSpoofEnabled(true)
            }

            // 3. Исключаем приложение из хуков KernelPatch (если активен)
            runCatching {
                ApNatives.setUidExclude(uid, 1)
                ApNatives.setPathHideEnabled(true)
            }

            // 4. Подменяем статус загрузчика в RAM (green/locked) без перезагрузки
            applyRamBootloaderSpoof(context)

            // 5. Завершаем процесс приложения для мгновенного вступления в силу без перезагрузки
            val cmds = arrayOf(
                "am force-stop $packageName 2>/dev/null || true",
                "rm -rf /data/data/$packageName/cache/* 2>/dev/null || true",
                "rm -rf /data/user_de/0/$packageName/cache/* 2>/dev/null || true",
            )
            runCatching { Shell.cmd(*cmds).exec() }
        } else {
            // Возврат к стандартному поведению
            runCatching {
                val defaultProfile = Natives.Profile(
                    name = packageName,
                    currentUid = uid,
                    allowSu = false,
                    nonRootUseDefault = true,
                    umountModules = false,
                )
                Natives.setAppProfile(defaultProfile)
            }
            runCatching { ApNatives.setUidExclude(uid, 0) }
            runCatching { Shell.cmd("am force-stop $packageName 2>/dev/null || true").exec() }
        }

        Log.i(TAG, "setAppHidden: package=$packageName uid=$uid enable=$enable")
        true
    }

    /**
     * Скрывает root и загрузчик от всех найденных банковских приложений в 1 клик.
     */
    suspend fun hideAllBanks(context: Context): Int = withContext(Dispatchers.IO) {
        val apps = loadApps(context)
        val banksToHide = apps.filter { it.isBankingApp && !it.isHidden }
        for (bank in banksToHide) {
            setAppHidden(context, bank.packageName, bank.uid, true)
        }
        banksToHide.size
    }

    /**
     * Подменяет свойства Verified Boot в оперативной памяти (RAM) через resetprop -n.
     * Не меняет диск и не требует перезагрузки!
     */
    fun applyRamBootloaderSpoof(context: Context) {
        val props = listOf(
            "ro.boot.vbmeta.device_state" to "locked",
            "ro.boot.verifiedbootstate" to "green",
            "ro.boot.flash.locked" to "1",
            "ro.boot.veritymode" to "enforcing",
            "ro.boot.warranty_bit" to "0",
            "ro.warranty_bit" to "0",
            "sys.oem_unlock_allowed" to "0",
            "ro.secure" to "1",
            "ro.debuggable" to "0",
        )

        val resetpropBin = when {
            File("/data/adb/ksu/bin/resetprop").canExecute() -> "/data/adb/ksu/bin/resetprop"
            else -> {
                val bundled = File(context.applicationInfo.nativeLibraryDir, "libresetprop.so")
                if (bundled.exists()) "\"${bundled.absolutePath}\"" else "resetprop"
            }
        }

        val commands = props.map { (k, v) ->
            "$resetpropBin -n $k $v 2>/dev/null || true"
        }
        runCatching { Shell.cmd(*commands.toTypedArray()).exec() }
    }

    /**
     * Применяет скрытие для всех ранее выбранных приложений при запуске менеджера.
     */
    suspend fun applyAllOnStartup(context: Context) = withContext(Dispatchers.IO) {
        val hiddenSet = getHiddenPackages(context)
        if (hiddenSet.isEmpty()) return@withContext

        applyRamBootloaderSpoof(context)
        val pm = context.packageManager
        for (pkg in hiddenSet) {
            val uid = runCatching { pm.getApplicationInfo(pkg, 0).uid }.getOrNull() ?: continue
            runCatching {
                val profile = Natives.Profile(
                    name = pkg,
                    currentUid = uid,
                    allowSu = false,
                    nonRootUseDefault = false,
                    umountModules = true,
                )
                Natives.setAppProfile(profile)
            }
            runCatching { ApNatives.setUidExclude(uid, 1) }
        }
        runCatching {
            Natives.setKernelUmountEnabled(true)
            Natives.setMountHideEnabled(true)
            Natives.setSelinuxHideEnabled(true)
            Natives.setAvcSpoofEnabled(true)
            ApNatives.setPathHideEnabled(true)
        }
    }
}

