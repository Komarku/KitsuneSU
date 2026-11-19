package com.sukisu.nova.folkpatch

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * JNI bridge to FolkPatch / KernelPatch (`libapjni.so`).
 * Provides full KernelPatch supercall support alongside SukiSU's `Natives` (`libkernelsu.so`).
 */
object ApNatives {
    private const val TAG = "KitsuneApNatives"
    const val DEFAULT_SCONTEXT = "u:r:magisk:s0"
    const val DEFAULT_SU_PATH = "/system/bin/kp"

    @Volatile
    var isNativeLoaded: Boolean = false
        private set

    init {
        isNativeLoaded = try {
            System.loadLibrary("apjni")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "libapjni.so could not be loaded on this ABI", t)
            false
        }
    }

    @Volatile
    var superKey: String = ""

    @Immutable
    @Parcelize
    @Keep
    data class Profile(
        var uid: Int = 0,
        var toUid: Int = 0,
        var scontext: String = DEFAULT_SCONTEXT,
    ) : Parcelable

    @Keep
    class KPMCtlRes {
        var rc: Long = 0
        var outMsg: String? = null

        constructor()

        constructor(rc: Long, outMsg: String?) {
            this.rc = rc
            this.outMsg = outMsg
        }
    }

    private external fun nativeReady(superKey: String): Boolean
    private external fun nativeKernelPatchVersion(superKey: String): Long
    private external fun nativeKernelPatchBuildTime(superKey: String): String
    private external fun nativeSu(superKey: String, toUid: Int, scontext: String?): Long
    private external fun nativeSetUidExclude(superKey: String, uid: Int, exclude: Int): Int
    private external fun nativeGetUidExclude(superKey: String, uid: Int): Int
    private external fun nativeSetNewAppProfileMode(superKey: String, mode: Int): Long
    private external fun nativeGetNewAppProfileMode(superKey: String): Int
    private external fun nativeSuUids(superKey: String): IntArray
    private external fun nativeSuProfile(superKey: String, uid: Int): Profile
    private external fun nativeLoadKernelPatchModule(superKey: String, modulePath: String, args: String): Long
    private external fun nativeControlKernelPatchModule(superKey: String, modName: String, jctlargs: String): KPMCtlRes
    private external fun nativeUnloadKernelPatchModule(superKey: String, moduleName: String): Long
    private external fun nativeKernelPatchModuleNum(superKey: String): Long
    private external fun nativeKernelPatchModuleList(superKey: String): String
    private external fun nativeKernelPatchModuleInfo(superKey: String, modName: String): String
    private external fun nativeGrantSu(superKey: String, uid: Int, toUid: Int, scontext: String?): Long
    private external fun nativeRevokeSu(superKey: String, uid: Int): Long
    private external fun nativeSuPath(superKey: String): String
    private external fun nativeResetSuPath(superKey: String, path: String): Boolean
    private external fun nativeControlFeature(superKey: String, name: String, value: Int): Long
    private external fun nativeUtsSet(superKey: String, key: String, value: String): Long
    private external fun nativeUtsReset(superKey: String): Long
    private external fun nativePathHideAdd(superKey: String, path: String): Long
    private external fun nativePathHideRemove(superKey: String, path: String): Long
    private external fun nativePathHideList(superKey: String): String
    private external fun nativePathHideClear(superKey: String): Long
    private external fun nativePathHideEnable(superKey: String, enable: Int): Long
    private external fun nativePathHideStatus(superKey: String): Long
    private external fun nativePathHideUidAdd(superKey: String, uid: Int): Long
    private external fun nativePathHideUidRemove(superKey: String, uid: Int): Long
    private external fun nativePathHideUidList(superKey: String): String
    private external fun nativePathHideUidClear(superKey: String): Long
    private external fun nativePathHideUidMode(superKey: String, mode: Int): Long
    private external fun nativePathHideFilterSystem(superKey: String, enable: Int): Long
    private external fun nativeNetIsolateEnable(superKey: String, enable: Int): Long
    private external fun nativeNetIsolateStatus(superKey: String): Long
    private external fun nativeNetIsolateUidAdd(superKey: String, uid: Int): Long
    private external fun nativeNetIsolateUidRemove(superKey: String, uid: Int): Long
    private external fun nativeNetIsolateUidList(superKey: String): String
    private external fun nativeNetIsolateUidClear(superKey: String): Long
    private external fun nativeSuAuditList(superKey: String): String
    private external fun nativeSuAuditClear(superKey: String): Long
    private external fun nativeGetApiToken(context: Context): String

    fun isReady(key: String = superKey): Boolean {
        if (!isNativeLoaded) return false
        return runCatching { nativeReady(key.ifBlank { "su" }) }.getOrDefault(false)
    }

    fun kernelPatchVersion(key: String = superKey): Long {
        if (!isNativeLoaded) return 0L
        return runCatching { nativeKernelPatchVersion(key.ifBlank { "su" }) }.getOrDefault(0L)
    }

    fun formatKernelPatchVersion(version: Long = kernelPatchVersion()): String {
        if (version <= 0L) return ""
        val major = (version ushr 16) and 0xFF
        val minor = (version ushr 8) and 0xFF
        val patch = version and 0xFF
        return "$major.$minor.$patch"
    }

    fun kernelPatchBuildTime(key: String = superKey): String {
        if (!isNativeLoaded) return ""
        return runCatching { nativeKernelPatchBuildTime(key.ifBlank { "su" }) }.getOrDefault("")
    }

    fun su(toUid: Int = 0, scontext: String = ""): Boolean {
        if (!isNativeLoaded || superKey.isBlank()) return false
        return runCatching { nativeSu(superKey, toUid, scontext) == 0L }.getOrDefault(false)
    }

    fun suPath(): String {
        if (!isNativeLoaded || superKey.isBlank()) return DEFAULT_SU_PATH
        return runCatching { nativeSuPath(superKey).ifBlank { DEFAULT_SU_PATH } }.getOrDefault(DEFAULT_SU_PATH)
    }

    fun resetSuPath(path: String): Boolean {
        if (!isNativeLoaded || superKey.isBlank()) return false
        return runCatching { nativeResetSuPath(superKey, path) }.getOrDefault(false)
    }

    fun suUids(): IntArray {
        if (!isNativeLoaded || superKey.isBlank()) return intArrayOf()
        return runCatching { nativeSuUids(superKey) }.getOrDefault(intArrayOf())
    }

    fun grantSu(uid: Int, toUid: Int = 0, scontext: String = DEFAULT_SCONTEXT): Boolean {
        if (!isNativeLoaded || superKey.isBlank()) return false
        return runCatching { nativeGrantSu(superKey, uid, toUid, scontext) == 0L }.getOrDefault(false)
    }

    fun revokeSu(uid: Int): Boolean {
        if (!isNativeLoaded || superKey.isBlank()) return false
        return runCatching { nativeRevokeSu(superKey, uid) == 0L }.getOrDefault(false)
    }

    fun setUidExclude(uid: Int, exclude: Int): Int {
        if (!isNativeLoaded || superKey.isBlank()) return -1
        return runCatching { nativeSetUidExclude(superKey, uid, exclude) }.getOrDefault(-1)
    }

    fun getUidExclude(uid: Int): Int {
        if (!isNativeLoaded || superKey.isBlank()) return 0
        return runCatching { nativeGetUidExclude(superKey, uid) }.getOrDefault(0)
    }

    fun setNewAppProfileMode(mode: Int): Long {
        if (!isNativeLoaded || superKey.isBlank()) return -1L
        return runCatching { nativeSetNewAppProfileMode(superKey, mode) }.getOrDefault(-1L)
    }

    fun getNewAppProfileMode(): Int {
        if (!isNativeLoaded || superKey.isBlank()) return 0
        return runCatching { nativeGetNewAppProfileMode(superKey) }.getOrDefault(0)
    }

    fun loadKernelPatchModule(modulePath: String, args: String = ""): Long {
        if (!isNativeLoaded || superKey.isBlank()) return -1L
        return runCatching { nativeLoadKernelPatchModule(superKey, modulePath, args) }.getOrDefault(-1L)
    }

    fun unloadKernelPatchModule(moduleName: String): Long {
        if (!isNativeLoaded || superKey.isBlank()) return -1L
        return runCatching { nativeUnloadKernelPatchModule(superKey, moduleName) }.getOrDefault(-1L)
    }

    fun controlKernelPatchModule(modName: String, args: String): KPMCtlRes {
        if (!isNativeLoaded || superKey.isBlank()) return KPMCtlRes(-1L, "KernelPatch not ready")
        return runCatching { nativeControlKernelPatchModule(superKey, modName, args) }
            .getOrDefault(KPMCtlRes(-1L, "Control call failed"))
    }

    fun kernelPatchModuleNum(): Long {
        if (!isNativeLoaded || superKey.isBlank()) return 0L
        return runCatching { nativeKernelPatchModuleNum(superKey) }.getOrDefault(0L)
    }

    fun kernelPatchModuleList(): String {
        if (!isNativeLoaded || superKey.isBlank()) return ""
        return runCatching { nativeKernelPatchModuleList(superKey) }.getOrDefault("")
    }

    fun kernelPatchModuleInfo(modName: String): String {
        if (!isNativeLoaded || superKey.isBlank()) return ""
        return runCatching { nativeKernelPatchModuleInfo(superKey, modName) }.getOrDefault("")
    }

    fun setPathHideEnabled(enable: Boolean): Boolean {
        if (!isNativeLoaded || superKey.isBlank()) return false
        return runCatching { nativePathHideEnable(superKey, if (enable) 1 else 0) == 0L }.getOrDefault(false)
    }

    fun getPathHideStatus(): Boolean {
        if (!isNativeLoaded || superKey.isBlank()) return false
        return runCatching { nativePathHideStatus(superKey) > 0L }.getOrDefault(false)
    }

    fun setNetIsolateEnabled(enable: Boolean): Boolean {
        if (!isNativeLoaded || superKey.isBlank()) return false
        return runCatching { nativeNetIsolateEnable(superKey, if (enable) 1 else 0) == 0L }.getOrDefault(false)
    }

    fun getNetIsolateStatus(): Boolean {
        if (!isNativeLoaded || superKey.isBlank()) return false
        return runCatching { nativeNetIsolateStatus(superKey) > 0L }.getOrDefault(false)
    }
}

@Keep
object ApMagicaPreload {
    @JvmStatic
    external fun forkDontCareAndExecApd(apdPath: String, superKey: String, packageName: String)
}

