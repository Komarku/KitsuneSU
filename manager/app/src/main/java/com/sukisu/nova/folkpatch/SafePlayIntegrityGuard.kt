package com.sukisu.nova.folkpatch

import android.content.Context
import android.util.Log
import com.sukisu.nova.Natives
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PlayIntegrityHideResult(
    val appliedLayers: List<String>,
    val rootActive: Boolean,
)

/**
 * 100% безопасное скрытие рута от Play Integrity / GMS.
 *
 * Гарантия отсутствия бутлупа / брика / софт-брика:
 *  - НЕ модифицирует разделы /system, /vendor, boot или init_boot.
 *  - НЕ записывает скрипты в post-fs-data.d и не меняет build.prop на диске.
 *  - Работает только через штатные runtime-флаги ядра (SukiSU / KernelPatch)
 *    и resetprop -n исключительно в оперативной памяти (RAM).
 */
object SafePlayIntegrityGuard {
    private const val TAG = "KitsuneSafePI"
    private const val PREF_KEY = "safe_play_integrity_hide"

    private val GMS_PACKAGES = listOf(
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
    )

    // Безопасные runtime-свойства Verified Boot (только RAM, никогда не вызывают бутлуп)
    private val SAFE_RAM_PROPS = listOf(
        "ro.boot.vbmeta.device_state" to "locked",
        "ro.boot.verifiedbootstate" to "green",
        "ro.boot.flash.locked" to "1",
        "ro.boot.veritymode" to "enforcing",
        "ro.boot.warranty_bit" to "0",
        "ro.warranty_bit" to "0",
        "sys.oem_unlock_allowed" to "0",
        "ro.debuggable" to "0",
        "ro.secure" to "1",
    )

    fun isEnabled(context: Context): Boolean {
        val sp = context.applicationContext.getSharedPreferences("kitsune_folkpatch_prefs", Context.MODE_PRIVATE)
        return sp.getBoolean(PREF_KEY, false)
    }

    private fun setEnabledPref(context: Context, enabled: Boolean) {
        val sp = context.applicationContext.getSharedPreferences("kitsune_folkpatch_prefs", Context.MODE_PRIVATE)
        sp.edit().putBoolean(PREF_KEY, enabled).apply()
    }

    suspend fun setEnabled(context: Context, enabled: Boolean): PlayIntegrityHideResult = withContext(Dispatchers.IO) {
        setEnabledPref(context, enabled)
        val layers = mutableListOf<String>()
        val ksuActive = Natives.isManager && Natives.version != null
        val kpActive = ApNatives.isReady(FolkPatchPrefs.state.value.superKey)

        // 1. Штатные безопасные флаги ядра SukiSU / KernelSU
        if (ksuActive) {
            runCatching {
                Natives.setDefaultUmountModules(enabled)
                if (enabled) layers.add("Umount модулей")
            }
            runCatching {
                if (Natives.setKernelUmountEnabled(enabled) && enabled) layers.add("Kernel Umount")
            }
            runCatching {
                if (Natives.setMountHideEnabled(enabled) == 0 && enabled) layers.add("Mount Hide")
            }
            runCatching {
                if (Natives.setSelinuxHideEnabled(enabled) == 0 && enabled) layers.add("SELinux Hide")
            }
            runCatching {
                if (Natives.setAvcSpoofEnabled(enabled) && enabled) layers.add("AVC Spoof")
            }
        }

        // 2. Штатные безопасные флаги FolkPatch / KernelPatch (если активен boot)
        if (kpActive) {
            runCatching {
                if (ApNatives.setPathHideEnabled(enabled) && enabled) {
                    layers.add("PathHide (KP)")
                }
            }
            val pm = context.packageManager
            var excludedCount = 0
            for (pkg in GMS_PACKAGES) {
                val uid = runCatching { pm.getApplicationInfo(pkg, 0).uid }.getOrNull() ?: continue
                val rc = ApNatives.setUidExclude(uid, if (enabled) 1 else 0)
                if (rc == 0) excludedCount++
            }
            if (enabled && excludedCount > 0) {
                layers.add("Исключение GMS ($excludedCount)")
            }
        }

        // 3. Безопасная RAM-подмена флагов загрузчика через встроенный libresetprop.so -n (без записи на диск!)
        val hasRootShell = ksuActive || kpActive || runCatching { Shell.getShell().isRoot }.getOrDefault(false)
        if (enabled && hasRootShell) {
            val resetprop = File(context.applicationInfo.nativeLibraryDir, "libresetprop.so")
            if (resetprop.isFile) {
                val cmds = SAFE_RAM_PROPS.map { (k, v) ->
                    "\"${resetprop.absolutePath}\" -n $k $v 2>/dev/null || true"
                }
                val res = runCatching {
                    Shell.cmd(*cmds.toTypedArray()).exec()
                }.getOrNull()
                if (res != null && res.isSuccess) {
                    layers.add("Verified Boot (RAM)")
                }
            }
        }

        Log.i(TAG, "SafePlayIntegrityGuard enabled=$enabled rootActive=$hasRootShell layers=$layers")
        PlayIntegrityHideResult(
            appliedLayers = layers,
            rootActive = hasRootShell,
        )
    }

    suspend fun cleanGmsCacheAndOptimize(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val hideRes = setEnabled(context, true)
        val hasRoot = hideRes.rootActive || runCatching { Shell.getShell().isRoot }.getOrDefault(false)
        if (!hasRoot) {
            return@withContext false to "Требуются права root для сброса кэша Play Services."
        }

        val commands = arrayOf(
            "killall com.google.android.gms.unstable 2>/dev/null || true",
            "rm -rf /data/data/com.google.android.gms/cache/* 2>/dev/null || true",
            "rm -rf /data/user_de/0/com.google.android.gms/cache/* 2>/dev/null || true",
            "rm -rf /data/data/com.android.vending/cache/* 2>/dev/null || true",
            "am force-stop com.google.android.gms.unstable 2>/dev/null || true"
        )
        val res = runCatching { Shell.cmd(*commands).exec() }.getOrNull()
        val success = res != null && res.isSuccess
        val summary = buildString {
            append("Антидетект применён в RAM. ")
            if (hideRes.appliedLayers.isNotEmpty()) {
                append("Слои: ").append(hideRes.appliedLayers.joinToString(", ")).append(". ")
            }
            if (success) {
                append("Кэш Google Play Services и Play Маркета очищен.")
            } else {
                append("Не удалось очистить кэш GMS.")
            }
        }
        success to summary
    }

    suspend fun applyOnStartupIfEnabled(context: Context) {
        if (isEnabled(context)) {
            runCatching { setEnabled(context, true) }
        }
    }
}
