package com.sukisu.nova.folkpatch

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.system.Os
import android.util.Log
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FolkPatchMode {
    PATCH_ONLY,
    PATCH_AND_INSTALL,
    INSTALL_TO_NEXT_SLOT,
    UNPATCH,
}

data class FolkKImgInfo(
    val banner: String = "",
    val patched: Boolean = false,
)

data class FolkKpImgInfo(
    val version: String = "",
    val compileTime: String = "",
    val config: String = "",
    val superKey: String = "",
)

data class EmbeddedKpmItem(
    val fileName: String,
    val localPath: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val args: String = "",
)

data class FolkPatchResult(
    val success: Boolean,
    val outputPath: String? = null,
    val needReboot: Boolean = false,
    val error: String? = null,
)

/**
 * Complete FolkPatch / KernelPatch `boot.img` patching & installation engine.
 * Runs FolkPatch's `boot_patch.sh`, `boot_unpatch.sh`, `boot_extract.sh`,
 * `InstallAP.sh`, `UninstallAP.sh` with bundled `libfpkptools.so` (KernelPatch 0.13.9),
 * `libbusybox.so`, `libkpatch.so`, `libbootctl.so`, `libapd.so`, and `kpimg`.
 */
object FolkPatchBootPatcher {
    private const val TAG = "KitsuneFolkPatcher"

    private val PATCH_ASSETS = listOf(
        "kpimg",
        "boot_patch.sh",
        "boot_unpatch.sh",
        "boot_extract.sh",
        "boot_flash.sh",
        "util_functions.sh",
        "InstallAP.sh",
        "UninstallAP.sh",
    )

    fun generateRandomSuperKey(length: Int = 12): String {
        val letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val digits = "0123456789"
        val all = letters + digits
        val rnd = SecureRandom()
        val chars = CharArray(length.coerceAtLeast(8))
        chars[0] = letters[rnd.nextInt(letters.length)]
        chars[1] = digits[rnd.nextInt(digits.length)]
        for (i in 2 until chars.size) {
            chars[i] = all[rnd.nextInt(all.length)]
        }
        for (i in chars.size - 1 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[j]
            chars[j] = tmp
        }
        return String(chars)
    }

    fun isSuperKeyValid(key: String): Boolean {
        if (key.length < 8) return false
        return key.any { it.isLetter() } && key.any { it.isDigit() }
    }

    fun getPatchDir(context: Context): File = File(context.filesDir, "folkpatch_work")

    private fun getShell(requireRoot: Boolean): Shell {
        return if (requireRoot) {
            runCatching { Shell.getShell() }.getOrElse {
                Shell.Builder.create().build("sh")
            }
        } else {
            Shell.Builder.create().setFlags(Shell.FLAG_NON_ROOT_SHELL).build("sh")
        }
    }

    suspend fun prepareWorkspace(
        context: Context,
        requireRoot: Boolean = false,
        customKpImgUri: Uri? = null,
        onLog: (String) -> Unit = {},
    ): Pair<File, FolkKpImgInfo> = withContext(Dispatchers.IO) {
        val patchDir = getPatchDir(context)
        if (patchDir.exists()) {
            patchDir.deleteRecursively()
        }
        patchDir.mkdirs()

        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val binaryMap = mapOf(
            "libfpkptools.so" to "kptools",
            "libbusybox.so" to "busybox",
            "libkpatch.so" to "kpatch",
            "libbootctl.so" to "bootctl",
            "libapd.so" to "apd",
            "libresetprop.so" to "resetprop",
        )
        for ((soName, binName) in binaryMap) {
            val src = File(nativeDir, soName)
            val dst = File(patchDir, binName)
            if (src.exists()) {
                try {
                    if (dst.exists()) dst.delete()
                    Os.symlink(src.absolutePath, dst.absolutePath)
                } catch (_: Throwable) {
                    src.copyTo(dst, overwrite = true)
                    dst.setExecutable(true, false)
                }
            }
        }

        for (asset in PATCH_ASSETS) {
            val outFile = File(patchDir, asset)
            runCatching {
                context.assets.open("folkpatch/$asset").use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                }
                if (asset.endsWith(".sh")) {
                    outFile.setExecutable(true, false)
                }
            }.onFailure {
                onLog("! Failed to extract asset $asset: ${it.message}")
            }
        }

        if (customKpImgUri != null) {
            val kpFile = File(patchDir, "kpimg")
            context.contentResolver.openInputStream(customKpImgUri)?.use { input ->
                FileOutputStream(kpFile).use { output -> input.copyTo(output) }
            }
            onLog("- Loaded custom kpimg")
        }

        val sh = getShell(requireRoot)
        val kpInfo = parseKpImgInfo(sh, patchDir)
        patchDir to kpInfo
    }

    private fun parseIniSimple(text: String): Map<String, Map<String, String>> {
        val result = linkedMapOf<String, MutableMap<String, String>>()
        var currentSection = ""
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) return@forEach
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length - 1).trim()
                result.getOrPut(currentSection) { linkedMapOf() }
            } else {
                val eq = line.indexOf('=')
                if (eq > 0) {
                    val key = line.substring(0, eq).trim()
                    val value = line.substring(eq + 1).trim()
                    result.getOrPut(currentSection) { linkedMapOf() }[key] = value
                }
            }
        }
        return result
    }

    private fun parseKpImgInfo(shell: Shell, patchDir: File): FolkKpImgInfo {
        val res = shell.newJob()
            .add("cd \"${patchDir.absolutePath}\" && ./kptools -l -k kpimg")
            .to(ArrayList(), ArrayList())
            .exec()
        val out = res.out.joinToString("\n")
        val ini = parseIniSimple(out)
        val kp = ini["kpimg"].orEmpty()
        return FolkKpImgInfo(
            version = kp["version"] ?: BuildConfigKpVersion(),
            compileTime = kp["compile_time"].orEmpty(),
            config = kp["config"].orEmpty(),
            superKey = kp["superkey"].orEmpty(),
        )
    }

    private fun BuildConfigKpVersion(): String = "0.13.9"

    suspend fun inspectBootImage(
        context: Context,
        bootUri: Uri,
        onLog: (String) -> Unit = {},
    ): Pair<FolkKImgInfo, String> = withContext(Dispatchers.IO) {
        val patchDir = getPatchDir(context)
        if (!File(patchDir, "kptools").exists()) {
            prepareWorkspace(context, requireRoot = false, onLog = onLog)
        }
        val bootFile = File(patchDir, "boot.img")
        context.contentResolver.openInputStream(bootUri)?.use { input ->
            FileOutputStream(bootFile).use { output -> input.copyTo(output) }
        } ?: error("Cannot open selected boot image")

        val fileName = getDisplayName(context, bootUri) ?: "boot.img"
        onLog("- Selected image: $fileName (${bootFile.length() / 1024} KB)")

        val sh = getShell(requireRoot = false)
        val unpackRes = sh.newJob()
            .add("cd \"${patchDir.absolutePath}\" && rm -f kernel && ./kptools unpack boot.img")
            .to(ArrayList(), ArrayList())
            .exec()
        if (!unpackRes.isSuccess) {
            val errText = (unpackRes.out + unpackRes.err).joinToString("\n")
            onLog("! kptools unpack failed:\n$errText")
            return@withContext FolkKImgInfo(banner = "Unpack failed", patched = false) to fileName
        }

        val listRes = sh.newJob()
            .add("cd \"${patchDir.absolutePath}\" && ./kptools -l -i kernel")
            .to(ArrayList(), ArrayList())
            .exec()
        val ini = parseIniSimple(listRes.out.joinToString("\n"))
        val kernelSec = ini["kernel"].orEmpty()
        val info = FolkKImgInfo(
            banner = kernelSec["banner"] ?: "Android Kernel",
            patched = kernelSec["patched"]?.equals("true", ignoreCase = true) == true,
        )
        onLog("- Kernel: ${info.banner.lines().firstOrNull().orEmpty()}")
        onLog("- Already patched: ${info.patched}")
        info to fileName
    }

    suspend fun inspectAndStageKpm(
        context: Context,
        kpmUri: Uri,
    ): EmbeddedKpmItem? = withContext(Dispatchers.IO) {
        val patchDir = getPatchDir(context)
        if (!File(patchDir, "kptools").exists()) {
            prepareWorkspace(context, requireRoot = false)
        }
        val fileName = getDisplayName(context, kpmUri) ?: "module_${System.currentTimeMillis()}.kpm"
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destFile = File(patchDir, safeName)
        context.contentResolver.openInputStream(kpmUri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        } ?: return@withContext null

        val sh = getShell(requireRoot = false)
        val res = sh.newJob()
            .add("cd \"${patchDir.absolutePath}\" && ./kptools -l -M \"${destFile.name}\"")
            .to(ArrayList(), ArrayList())
            .exec()
        val ini = parseIniSimple(res.out.joinToString("\n"))
        val kpm = ini["kpm"].orEmpty()
        EmbeddedKpmItem(
            fileName = safeName,
            localPath = destFile.absolutePath,
            name = kpm["name"]?.ifBlank { safeName } ?: safeName,
            version = kpm["version"].orEmpty(),
            author = kpm["author"].orEmpty(),
            description = kpm["description"].orEmpty(),
        )
    }

    suspend fun executePatch(
        context: Context,
        mode: FolkPatchMode,
        superKey: String,
        bootUri: Uri?,
        customKpImgUri: Uri? = null,
        embeddedKpms: List<EmbeddedKpmItem> = emptyList(),
        onLog: (String) -> Unit,
    ): FolkPatchResult = withContext(Dispatchers.IO) {
        val requireRoot = mode != FolkPatchMode.PATCH_ONLY
        val effectiveKey = superKey.trim().ifBlank { "su" }

        onLog("****************************")
        onLog(" KitsuneSU × Kitsunepatch Boot Patcher")
        onLog(" Mode: $mode")
        onLog("****************************")

        val (patchDir, kpInfo) = prepareWorkspace(
            context = context,
            requireRoot = requireRoot,
            customKpImgUri = customKpImgUri,
            onLog = onLog,
        )
        onLog("- KernelPatch kpimg v${kpInfo.version}")

        // Re-stage KPM files if they were copied before prepareWorkspace wiped patchDir
        val kpmArgs = mutableListOf<String>()
        for (kpm in embeddedKpms) {
            val f = File(kpm.localPath)
            if (f.exists()) {
                val staged = File(patchDir, f.name)
                if (staged.absolutePath != f.absolutePath) {
                    f.copyTo(staged, overwrite = true)
                }
                kpmArgs.add("-M")
                kpmArgs.add(staged.name)
                if (kpm.args.isNotBlank()) {
                    kpmArgs.add("-A")
                    kpmArgs.add(kpm.args)
                }
            }
        }

        val sh = getShell(requireRoot)
        val callback = object : CallbackList<String>() {
            override fun onAddElement(e: String?) {
                if (!e.isNullOrEmpty()) onLog(e)
            }
        }

        when (mode) {
            FolkPatchMode.PATCH_ONLY -> {
                if (bootUri == null) {
                    return@withContext FolkPatchResult(false, error = "No boot.img selected")
                }
                val bootFile = File(patchDir, "boot.img")
                context.contentResolver.openInputStream(bootUri)?.use { input ->
                    FileOutputStream(bootFile).use { output -> input.copyTo(output) }
                } ?: return@withContext FolkPatchResult(false, error = "Failed to read boot.img")

                val extraArgStr = kpmArgs.joinToString(" ") { shellQuote(it) }
                val cmd = "cd ${shellQuote(patchDir.absolutePath)} && ./busybox sh boot_patch.sh ${shellQuote(effectiveKey)} boot.img false $extraArgStr"
                val res = sh.newJob().add(cmd).to(callback, callback).exec()
                val newBoot = File(patchDir, "new-boot.img")
                if (!res.isSuccess || !newBoot.exists()) {
                    return@withContext FolkPatchResult(false, error = "Patching failed (exit ${res.code})")
                }
                val outName = "kitsunesu_boot_patched_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.img"
                val savedPath = saveToDownloads(context, newBoot, outName)
                if (savedPath != null) {
                    onLog("- Saved patched boot image to: $savedPath")
                    if (effectiveKey != "su") {
                        FolkPatchPrefs.setSuperKey(effectiveKey)
                    }
                    FolkPatchResult(true, outputPath = savedPath, needReboot = false)
                } else {
                    FolkPatchResult(false, error = "Failed to write patched boot image to Downloads")
                }
            }

            FolkPatchMode.PATCH_AND_INSTALL, FolkPatchMode.INSTALL_TO_NEXT_SLOT -> {
                val nextSlotFlag = if (mode == FolkPatchMode.INSTALL_TO_NEXT_SLOT) "true" else "false"
                val findBootCmd = """
                    cd ${shellQuote(patchDir.absolutePath)}
                    . ./util_functions.sh
                    if [ "$nextSlotFlag" = "true" ]; then
                        SLOT=$(getprop ro.boot.slot_suffix)
                        if [ "${'$'}SLOT" = "_a" ]; then SLOT="_b"; elif [ "${'$'}SLOT" = "_b" ]; then SLOT="_a"; fi
                    else
                        SLOT=$(getprop ro.boot.slot_suffix)
                    fi
                    BOOTIMAGE=$(find_block boot${'$'}SLOT)
                    echo "KITSUNE_BOOT_DEV=${'$'}BOOTIMAGE"
                """.trimIndent()
                val findOut = ArrayList<String>()
                sh.newJob().add(findBootCmd).to(findOut, callback).exec()
                val bootDev = findOut.firstOrNull { it.startsWith("KITSUNE_BOOT_DEV=") }
                    ?.removePrefix("KITSUNE_BOOT_DEV=")?.trim().orEmpty()
                if (bootDev.isBlank()) {
                    return@withContext FolkPatchResult(false, error = "Could not locate boot partition (root required)")
                }
                onLog("- Target boot partition: $bootDev")
                val extraArgStr = kpmArgs.joinToString(" ") { shellQuote(it) }
                val cmd = "cd ${shellQuote(patchDir.absolutePath)} && ./busybox sh boot_patch.sh ${shellQuote(effectiveKey)} ${shellQuote(bootDev)} true $extraArgStr"
                val res = sh.newJob().add(cmd).to(callback, callback).exec()
                if (res.isSuccess) {
                    if (effectiveKey != "su") {
                        FolkPatchPrefs.setSuperKey(effectiveKey)
                    }
                    FolkPatchResult(true, outputPath = bootDev, needReboot = true)
                } else {
                    FolkPatchResult(false, error = "Direct flash failed (exit ${res.code})")
                }
            }

            FolkPatchMode.UNPATCH -> {
                val findBootCmd = """
                    cd ${shellQuote(patchDir.absolutePath)}
                    . ./util_functions.sh
                    SLOT=$(getprop ro.boot.slot_suffix)
                    BOOTIMAGE=$(find_block boot${'$'}SLOT)
                    echo "KITSUNE_BOOT_DEV=${'$'}BOOTIMAGE"
                """.trimIndent()
                val findOut = ArrayList<String>()
                sh.newJob().add(findBootCmd).to(findOut, callback).exec()
                val bootDev = findOut.firstOrNull { it.startsWith("KITSUNE_BOOT_DEV=") }
                    ?.removePrefix("KITSUNE_BOOT_DEV=")?.trim().orEmpty()
                if (bootDev.isBlank()) {
                    return@withContext FolkPatchResult(false, error = "Could not locate boot partition (root required)")
                }
                onLog("- Unpatching target boot partition: $bootDev")
                val cmd = "cd ${shellQuote(patchDir.absolutePath)} && ./busybox sh boot_unpatch.sh ${shellQuote(bootDev)}"
                val res = sh.newJob().add(cmd).to(callback, callback).exec()
                if (res.isSuccess) {
                    FolkPatchResult(true, outputPath = bootDev, needReboot = true)
                } else {
                    FolkPatchResult(false, error = "Unpatch failed (exit ${res.code})")
                }
            }
        }
    }

    suspend fun installAndroidPatch(
        context: Context,
        onLog: (String) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val (patchDir, _) = prepareWorkspace(context, requireRoot = true, onLog = onLog)
        val sh = getShell(requireRoot = true)
        val callback = object : CallbackList<String>() {
            override fun onAddElement(e: String?) {
                if (!e.isNullOrEmpty()) onLog(e)
            }
        }
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val cmd = """
            cd ${shellQuote(patchDir.absolutePath)}
            export SUPERKEY=${shellQuote(ApNatives.superKey.ifBlank { "su" })}
            export NATIVE_DIR=${shellQuote(nativeDir)}
            ./busybox sh InstallAP.sh
        """.trimIndent()
        val res = sh.newJob().add(cmd).to(callback, callback).exec()
        res.isSuccess
    }

    suspend fun uninstallAndroidPatch(
        context: Context,
        onLog: (String) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val (patchDir, _) = prepareWorkspace(context, requireRoot = true, onLog = onLog)
        val sh = getShell(requireRoot = true)
        val callback = object : CallbackList<String>() {
            override fun onAddElement(e: String?) {
                if (!e.isNullOrEmpty()) onLog(e)
            }
        }
        val cmd = "cd ${shellQuote(patchDir.absolutePath)} && ./busybox sh UninstallAP.sh"
        val res = sh.newJob().add(cmd).to(callback, callback).exec()
        res.isSuccess
    }

    private fun saveToDownloads(context: Context, source: File, fileName: String): String? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(source).use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "/sdcard/Download/$fileName"
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val dst = File(dir, fileName)
                source.copyTo(dst, overwrite = true)
                dst.absolutePath
            }
        }.onFailure {
            Log.e(TAG, "Failed to save patched boot image", it)
        }.getOrNull()
    }

    private fun getDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun shellQuote(arg: String): String = "'" + arg.replace("'", "'\"'\"'") + "'"
}

