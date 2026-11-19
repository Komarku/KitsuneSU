package com.sukisu.nova.folkpatch

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class InstallEngineMode(val id: Int, val label: String) {
    SUKISU_INIT_BOOT(0, "Kitsune (init_boot)"),
    FOLKPATCH_BOOT(1, "Kitsunepatch (boot)"),
    ;

    companion object {
        fun fromId(id: Int): InstallEngineMode = entries.firstOrNull { it.id == id } ?: SUKISU_INIT_BOOT
    }
}

enum class HomeLayoutStyle(val id: Int, val key: String) {
    UNIFIED(0, "unified"),
    CLASSIC(1, "classic"),
    V2_GRID(2, "v2"),
    V3_MAGISK(3, "v3"),
    V4_HERO(4, "v4"),
    CIRCLE(5, "circle"),
    ;

    companion object {
        fun fromId(id: Int): HomeLayoutStyle = entries.firstOrNull { it.id == id } ?: UNIFIED
    }
}

enum class FloatingBarStyle(val id: Int, val key: String) {
    STANDARD(0, "standard"),
    FLOATING_PILL(1, "floating_pill"),
    LIQUID_GLASS(2, "liquid_glass"),
    ;

    companion object {
        fun fromId(id: Int): FloatingBarStyle = entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

enum class StatusCardShape(val id: Int, val label: String) {
    HERO(0, "Hero (M3 Expressive)"),
    SQUIRCLE(1, "Сквиркл (Squircle)"),
    CUT_CORNER(2, "Вырезанные углы (Cut-Corner)"),
    PILL(3, "Капсула (Pill)"),
    ASYMMETRIC(4, "Асимметричная (Asymmetric)"),
    ;

    companion object {
        fun fromId(id: Int): StatusCardShape = entries.firstOrNull { it.id == id } ?: HERO
    }
}

enum class StatusCardIcon(val id: Int, val label: String) {
    CHECK_CIRCLE(0, "Галочка в круге (CheckCircle)"),
    VERIFIED(1, "Звезда с галочкой (Verified)"),
    SHIELD(2, "Щит защиты (Security)"),
    KITSUNE(3, "Лиса Kitsune (Fox)"),
    STAR(4, "Искры M3 (Sparkle)"),
    NONE(5, "Без иконки"),
    ;

    companion object {
        fun fromId(id: Int): StatusCardIcon = entries.firstOrNull { it.id == id } ?: CHECK_CIRCLE
    }
}

enum class StatusCardDecoration(val id: Int, val label: String) {
    M3_SHAPES(0, "Фигуры Material 3 (Pill & Gems)"),
    SPARKLES(1, "Звёзды и искры M3"),
    RINGS(2, "Пульсирующие кольца"),
    FLOATING_POLYGONS(3, "Плавающие полигоны"),
    MINIMAL(4, "Минимализм"),
    ;

    companion object {
        fun fromId(id: Int): StatusCardDecoration = entries.firstOrNull { it.id == id } ?: M3_SHAPES
    }
}

@Immutable
data class FolkPatchSettingsState(
    // === Root Engine & General (FolkPatch 1-to-1) ===
    val defaultInstallEngine: InstallEngineMode = InstallEngineMode.SUKISU_INIT_BOOT,
    val superKey: String = "",
    val storeSuperKey: Boolean = true,
    val customSuperKeyPatch: Boolean = true,
    val suPath: String = ApNatives.DEFAULT_SU_PATH,
    val customAppTitle: String = "KitsuneSU",
    val globalNamespaceMode: Boolean = false,
    val magicMountEnabled: Boolean = false,
    val newAppProfileMode: Int = 0, // 0=Default, 1=Auto Exclude, 2=Auto Grant
    val blockKpUpdate: Boolean = false,
    val blockApUpdate: Boolean = false,
    val autoUpdateCheck: Boolean = true,
    val predictiveBack: Boolean = true,

    // === Design & Appearance (FolkPatch 1-to-1 merged with SukiSU) ===
    val homeLayoutStyle: HomeLayoutStyle = HomeLayoutStyle.UNIFIED,
    val amoledPureBlack: Boolean = false,
    val liquidGlassEnabled: Boolean = true,
    val glassBlurStrength: Float = 16f,
    val glassTransparency: Float = 0.80f,
    val glassHighlightStrength: Float = 0.45f,
    val glassSpecular: Boolean = true,
    val glassInnerGlow: Boolean = true,
    val glassBorder: Boolean = true,
    val floatingBarStyle: FloatingBarStyle = FloatingBarStyle.LIQUID_GLASS,
    val floatingAutoHide: Boolean = false,
    val floatingSwipeHide: Boolean = false,
    val switchShowIcon: Boolean = true,
    val discreteSlider: Boolean = false,
    val hideStatusBadge: Boolean = false,
    val customBadgeText: String = "",
    val showInfoCardIcons: Boolean = true,
    val moduleBannerEnabled: Boolean = true,
    val advancedTitleStyle: Boolean = true,
    val gridCardHideCheck: Boolean = false,
    val gridCardHideText: Boolean = false,
    val gridCardHideMode: Boolean = false,

    // === Status Card Customization (Кастомизация плашки статуса) ===
    val statusCardShape: StatusCardShape = StatusCardShape.HERO,
    val statusCardIcon: StatusCardIcon = StatusCardIcon.CHECK_CIRCLE,
    val statusCardDecoration: StatusCardDecoration = StatusCardDecoration.M3_SHAPES,
    val statusCardShowBadge: Boolean = true,
    val statusCardBadgeText: String = "ВКЛЮЧЕНО",
    val statusCardGlow: Boolean = true,

    // === Security (FolkPatch 1-to-1) ===
    val biometricAppLock: Boolean = false,
    val hideAppDisguise: Boolean = false,
    val pathHideEnabled: Boolean = false,
    val netIsolateEnabled: Boolean = false,

    // === Function & Services (FolkPatch 1-to-1) ===
    val shizukuBridgeEnabled: Boolean = false,
    val jailbreakModeEnabled: Boolean = false,
    val autoInstallAndroidPatch: Boolean = false,

    // === Multimedia (FolkPatch 1-to-1) ===
    val startupSoundEnabled: Boolean = false,
    val startupSoundAsset: String = "Zako.wav",
    val bgmEnabled: Boolean = false,
)

object FolkPatchPrefs {
    private const val PREF_NAME = "kitsune_folkpatch_prefs"

    val AVAILABLE_SOUNDS = listOf(
        "Zako.wav",
        "Zako2.wav",
        "Ciallo.wav",
        "Baka.wav",
        "Ehe.wav",
        "Imoi.wav",
    )

    private var prefs: SharedPreferences? = null
    private val _state = MutableStateFlow(FolkPatchSettingsState())
    val state: StateFlow<FolkPatchSettingsState> = _state.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs = sp
        val loaded = loadFromPrefs(sp)
        _state.value = loaded
        if (loaded.storeSuperKey && loaded.superKey.isNotBlank()) {
            ApNatives.superKey = loaded.superKey
        }
    }

    private fun loadFromPrefs(sp: SharedPreferences): FolkPatchSettingsState {
        return FolkPatchSettingsState(
            defaultInstallEngine = InstallEngineMode.fromId(sp.getInt("default_install_engine", 0)),
            superKey = sp.getString("super_key", "") ?: "",
            storeSuperKey = sp.getBoolean("store_super_key", true),
            customSuperKeyPatch = sp.getBoolean("custom_superkey_patch", true),
            suPath = sp.getString("su_path", ApNatives.DEFAULT_SU_PATH) ?: ApNatives.DEFAULT_SU_PATH,
            customAppTitle = sp.getString("custom_app_title", "KitsuneSU") ?: "KitsuneSU",
            globalNamespaceMode = sp.getBoolean("global_namespace_mode", false),
            magicMountEnabled = sp.getBoolean("magic_mount_enabled", false),
            newAppProfileMode = sp.getInt("new_app_profile_mode", 0),
            blockKpUpdate = sp.getBoolean("block_kp_update", false),
            blockApUpdate = sp.getBoolean("block_ap_update", false),
            autoUpdateCheck = sp.getBoolean("auto_update_check", true),
            predictiveBack = sp.getBoolean("predictive_back", true),

            homeLayoutStyle = HomeLayoutStyle.fromId(sp.getInt("home_layout_style", 0)),
            amoledPureBlack = sp.getBoolean("amoled_pure_black", false),
            liquidGlassEnabled = sp.getBoolean("liquid_glass_enabled", true),
            glassBlurStrength = sp.getFloat("glass_blur_strength", 16f),
            glassTransparency = sp.getFloat("glass_transparency", 0.80f),
            glassHighlightStrength = sp.getFloat("glass_highlight_strength", 0.45f),
            glassSpecular = sp.getBoolean("glass_specular", true),
            glassInnerGlow = sp.getBoolean("glass_inner_glow", true),
            glassBorder = sp.getBoolean("glass_border", true),
            floatingBarStyle = FloatingBarStyle.fromId(sp.getInt("floating_bar_style", 2)),
            floatingAutoHide = sp.getBoolean("floating_auto_hide", false),
            floatingSwipeHide = sp.getBoolean("floating_swipe_hide", false),
            switchShowIcon = sp.getBoolean("switch_show_icon", true),
            discreteSlider = sp.getBoolean("discrete_slider", false),
            hideStatusBadge = sp.getBoolean("hide_status_badge", false),
            customBadgeText = sp.getString("custom_badge_text", "") ?: "",
            showInfoCardIcons = sp.getBoolean("show_info_card_icons", true),
            moduleBannerEnabled = sp.getBoolean("module_banner_enabled", true),
            advancedTitleStyle = sp.getBoolean("advanced_title_style", true),
            gridCardHideCheck = sp.getBoolean("grid_card_hide_check", false),
            gridCardHideText = sp.getBoolean("grid_card_hide_text", false),
            gridCardHideMode = sp.getBoolean("grid_card_hide_mode", false),

            statusCardShape = StatusCardShape.fromId(sp.getInt("status_card_shape", 0)),
            statusCardIcon = StatusCardIcon.fromId(sp.getInt("status_card_icon", 0)),
            statusCardDecoration = StatusCardDecoration.fromId(sp.getInt("status_card_decoration", 0)),
            statusCardShowBadge = sp.getBoolean("status_card_show_badge", true),
            statusCardBadgeText = sp.getString("status_card_badge_text", "ВКЛЮЧЕНО") ?: "ВКЛЮЧЕНО",
            statusCardGlow = sp.getBoolean("status_card_glow", true),

            biometricAppLock = sp.getBoolean("biometric_app_lock", false),
            hideAppDisguise = sp.getBoolean("hide_app_disguise", false),
            pathHideEnabled = sp.getBoolean("path_hide_enabled", false),
            netIsolateEnabled = sp.getBoolean("net_isolate_enabled", false),

            shizukuBridgeEnabled = sp.getBoolean("shizuku_bridge_enabled", false),
            jailbreakModeEnabled = sp.getBoolean("jailbreak_mode_enabled", false),
            autoInstallAndroidPatch = sp.getBoolean("auto_install_android_patch", false),

            startupSoundEnabled = sp.getBoolean("startup_sound_enabled", false),
            startupSoundAsset = sp.getString("startup_sound_asset", "Zako.wav") ?: "Zako.wav",
            bgmEnabled = sp.getBoolean("bgm_enabled", false),
        )
    }

    fun update(transform: (FolkPatchSettingsState) -> FolkPatchSettingsState) {
        val next = transform(_state.value)
        _state.value = next
        if (next.storeSuperKey) {
            ApNatives.superKey = next.superKey
        }
        prefs?.edit {
            putInt("default_install_engine", next.defaultInstallEngine.id)
            putString("super_key", if (next.storeSuperKey) next.superKey else "")
            putBoolean("store_super_key", next.storeSuperKey)
            putBoolean("custom_superkey_patch", next.customSuperKeyPatch)
            putString("su_path", next.suPath)
            putString("custom_app_title", next.customAppTitle)
            putBoolean("global_namespace_mode", next.globalNamespaceMode)
            putBoolean("magic_mount_enabled", next.magicMountEnabled)
            putInt("new_app_profile_mode", next.newAppProfileMode)
            putBoolean("block_kp_update", next.blockKpUpdate)
            putBoolean("block_ap_update", next.blockApUpdate)
            putBoolean("auto_update_check", next.autoUpdateCheck)
            putBoolean("predictive_back", next.predictiveBack)

            putInt("home_layout_style", next.homeLayoutStyle.id)
            putBoolean("amoled_pure_black", next.amoledPureBlack)
            putBoolean("liquid_glass_enabled", next.liquidGlassEnabled)
            putFloat("glass_blur_strength", next.glassBlurStrength)
            putFloat("glass_transparency", next.glassTransparency)
            putFloat("glass_highlight_strength", next.glassHighlightStrength)
            putBoolean("glass_specular", next.glassSpecular)
            putBoolean("glass_inner_glow", next.glassInnerGlow)
            putBoolean("glass_border", next.glassBorder)
            putInt("floating_bar_style", next.floatingBarStyle.id)
            putBoolean("floating_auto_hide", next.floatingAutoHide)
            putBoolean("floating_swipe_hide", next.floatingSwipeHide)
            putBoolean("switch_show_icon", next.switchShowIcon)
            putBoolean("discrete_slider", next.discreteSlider)
            putBoolean("hide_status_badge", next.hideStatusBadge)
            putString("custom_badge_text", next.customBadgeText)
            putBoolean("show_info_card_icons", next.showInfoCardIcons)
            putBoolean("module_banner_enabled", next.moduleBannerEnabled)
            putBoolean("advanced_title_style", next.advancedTitleStyle)
            putBoolean("grid_card_hide_check", next.gridCardHideCheck)
            putBoolean("grid_card_hide_text", next.gridCardHideText)
            putBoolean("grid_card_hide_mode", next.gridCardHideMode)

            putInt("status_card_shape", next.statusCardShape.id)
            putInt("status_card_icon", next.statusCardIcon.id)
            putInt("status_card_decoration", next.statusCardDecoration.id)
            putBoolean("status_card_show_badge", next.statusCardShowBadge)
            putString("status_card_badge_text", next.statusCardBadgeText)
            putBoolean("status_card_glow", next.statusCardGlow)

            putBoolean("biometric_app_lock", next.biometricAppLock)
            putBoolean("hide_app_disguise", next.hideAppDisguise)
            putBoolean("path_hide_enabled", next.pathHideEnabled)
            putBoolean("net_isolate_enabled", next.netIsolateEnabled)

            putBoolean("shizuku_bridge_enabled", next.shizukuBridgeEnabled)
            putBoolean("jailbreak_mode_enabled", next.jailbreakModeEnabled)
            putBoolean("auto_install_android_patch", next.autoInstallAndroidPatch)

            putBoolean("startup_sound_enabled", next.startupSoundEnabled)
            putString("startup_sound_asset", next.startupSoundAsset)
            putBoolean("bgm_enabled", next.bgmEnabled)
        }
    }

    fun setSuperKey(key: String, persist: Boolean = _state.value.storeSuperKey) {
        ApNatives.superKey = key
        update { it.copy(superKey = key, storeSuperKey = persist) }
    }

    fun clearSuperKey() {
        ApNatives.superKey = ""
        update { it.copy(superKey = "") }
    }

    fun exportToJson(): String {
        val s = _state.value
        val obj = JSONObject()
        obj.put("version", 1)
        obj.put("manager", "KitsuneSU")
        obj.put("default_install_engine", s.defaultInstallEngine.id)
        obj.put("store_super_key", s.storeSuperKey)
        obj.put("custom_superkey_patch", s.customSuperKeyPatch)
        obj.put("su_path", s.suPath)
        obj.put("custom_app_title", s.customAppTitle)
        obj.put("global_namespace_mode", s.globalNamespaceMode)
        obj.put("magic_mount_enabled", s.magicMountEnabled)
        obj.put("new_app_profile_mode", s.newAppProfileMode)
        obj.put("block_kp_update", s.blockKpUpdate)
        obj.put("block_ap_update", s.blockApUpdate)
        obj.put("auto_update_check", s.autoUpdateCheck)
        obj.put("predictive_back", s.predictiveBack)
        obj.put("home_layout_style", s.homeLayoutStyle.id)
        obj.put("amoled_pure_black", s.amoledPureBlack)
        obj.put("liquid_glass_enabled", s.liquidGlassEnabled)
        obj.put("glass_blur_strength", s.glassBlurStrength.toDouble())
        obj.put("glass_transparency", s.glassTransparency.toDouble())
        obj.put("glass_highlight_strength", s.glassHighlightStrength.toDouble())
        obj.put("glass_specular", s.glassSpecular)
        obj.put("glass_inner_glow", s.glassInnerGlow)
        obj.put("glass_border", s.glassBorder)
        obj.put("floating_bar_style", s.floatingBarStyle.id)
        obj.put("floating_auto_hide", s.floatingAutoHide)
        obj.put("floating_swipe_hide", s.floatingSwipeHide)
        obj.put("switch_show_icon", s.switchShowIcon)
        obj.put("discrete_slider", s.discreteSlider)
        obj.put("hide_status_badge", s.hideStatusBadge)
        obj.put("custom_badge_text", s.customBadgeText)
        obj.put("show_info_card_icons", s.showInfoCardIcons)
        obj.put("module_banner_enabled", s.moduleBannerEnabled)
        obj.put("advanced_title_style", s.advancedTitleStyle)
        obj.put("grid_card_hide_check", s.gridCardHideCheck)
        obj.put("grid_card_hide_text", s.gridCardHideText)
        obj.put("grid_card_hide_mode", s.gridCardHideMode)
        obj.put("status_card_shape", s.statusCardShape.id)
        obj.put("status_card_icon", s.statusCardIcon.id)
        obj.put("status_card_decoration", s.statusCardDecoration.id)
        obj.put("status_card_show_badge", s.statusCardShowBadge)
        obj.put("status_card_badge_text", s.statusCardBadgeText)
        obj.put("status_card_glow", s.statusCardGlow)
        obj.put("biometric_app_lock", s.biometricAppLock)
        obj.put("hide_app_disguise", s.hideAppDisguise)
        obj.put("path_hide_enabled", s.pathHideEnabled)
        obj.put("net_isolate_enabled", s.netIsolateEnabled)
        obj.put("shizuku_bridge_enabled", s.shizukuBridgeEnabled)
        obj.put("jailbreak_mode_enabled", s.jailbreakModeEnabled)
        obj.put("auto_install_android_patch", s.autoInstallAndroidPatch)
        obj.put("startup_sound_enabled", s.startupSoundEnabled)
        obj.put("startup_sound_asset", s.startupSoundAsset)
        obj.put("bgm_enabled", s.bgmEnabled)
        return obj.toString(2)
    }

    fun importFromJson(jsonText: String): Boolean {
        return runCatching {
            val obj = JSONObject(jsonText)
            update { cur ->
                cur.copy(
                    defaultInstallEngine = InstallEngineMode.fromId(obj.optInt("default_install_engine", cur.defaultInstallEngine.id)),
                    storeSuperKey = obj.optBoolean("store_super_key", cur.storeSuperKey),
                    customSuperKeyPatch = obj.optBoolean("custom_superkey_patch", cur.customSuperKeyPatch),
                    suPath = obj.optString("su_path", cur.suPath),
                    customAppTitle = obj.optString("custom_app_title", cur.customAppTitle),
                    globalNamespaceMode = obj.optBoolean("global_namespace_mode", cur.globalNamespaceMode),
                    magicMountEnabled = obj.optBoolean("magic_mount_enabled", cur.magicMountEnabled),
                    newAppProfileMode = obj.optInt("new_app_profile_mode", cur.newAppProfileMode),
                    blockKpUpdate = obj.optBoolean("block_kp_update", cur.blockKpUpdate),
                    blockApUpdate = obj.optBoolean("block_ap_update", cur.blockApUpdate),
                    autoUpdateCheck = obj.optBoolean("auto_update_check", cur.autoUpdateCheck),
                    predictiveBack = obj.optBoolean("predictive_back", cur.predictiveBack),
                    homeLayoutStyle = HomeLayoutStyle.fromId(obj.optInt("home_layout_style", cur.homeLayoutStyle.id)),
                    amoledPureBlack = obj.optBoolean("amoled_pure_black", cur.amoledPureBlack),
                    liquidGlassEnabled = obj.optBoolean("liquid_glass_enabled", cur.liquidGlassEnabled),
                    glassBlurStrength = obj.optDouble("glass_blur_strength", cur.glassBlurStrength.toDouble()).toFloat(),
                    glassTransparency = obj.optDouble("glass_transparency", cur.glassTransparency.toDouble()).toFloat(),
                    glassHighlightStrength = obj.optDouble("glass_highlight_strength", cur.glassHighlightStrength.toDouble()).toFloat(),
                    glassSpecular = obj.optBoolean("glass_specular", cur.glassSpecular),
                    glassInnerGlow = obj.optBoolean("glass_inner_glow", cur.glassInnerGlow),
                    glassBorder = obj.optBoolean("glass_border", cur.glassBorder),
                    floatingBarStyle = FloatingBarStyle.fromId(obj.optInt("floating_bar_style", cur.floatingBarStyle.id)),
                    floatingAutoHide = obj.optBoolean("floating_auto_hide", cur.floatingAutoHide),
                    floatingSwipeHide = obj.optBoolean("floating_swipe_hide", cur.floatingSwipeHide),
                    switchShowIcon = obj.optBoolean("switch_show_icon", cur.switchShowIcon),
                    discreteSlider = obj.optBoolean("discrete_slider", cur.discreteSlider),
                    hideStatusBadge = obj.optBoolean("hide_status_badge", cur.hideStatusBadge),
                    customBadgeText = obj.optString("custom_badge_text", cur.customBadgeText),
                    showInfoCardIcons = obj.optBoolean("show_info_card_icons", cur.showInfoCardIcons),
                    moduleBannerEnabled = obj.optBoolean("module_banner_enabled", cur.moduleBannerEnabled),
                    advancedTitleStyle = obj.optBoolean("advanced_title_style", cur.advancedTitleStyle),
                    gridCardHideCheck = obj.optBoolean("grid_card_hide_check", cur.gridCardHideCheck),
                    gridCardHideText = obj.optBoolean("grid_card_hide_text", cur.gridCardHideText),
                    gridCardHideMode = obj.optBoolean("grid_card_hide_mode", cur.gridCardHideMode),
                    statusCardShape = StatusCardShape.fromId(obj.optInt("status_card_shape", cur.statusCardShape.id)),
                    statusCardIcon = StatusCardIcon.fromId(obj.optInt("status_card_icon", cur.statusCardIcon.id)),
                    statusCardDecoration = StatusCardDecoration.fromId(obj.optInt("status_card_decoration", cur.statusCardDecoration.id)),
                    statusCardShowBadge = obj.optBoolean("status_card_show_badge", cur.statusCardShowBadge),
                    statusCardBadgeText = obj.optString("status_card_badge_text", cur.statusCardBadgeText),
                    statusCardGlow = obj.optBoolean("status_card_glow", cur.statusCardGlow),
                    biometricAppLock = obj.optBoolean("biometric_app_lock", cur.biometricAppLock),
                    hideAppDisguise = obj.optBoolean("hide_app_disguise", cur.hideAppDisguise),
                    pathHideEnabled = obj.optBoolean("path_hide_enabled", cur.pathHideEnabled),
                    netIsolateEnabled = obj.optBoolean("net_isolate_enabled", cur.netIsolateEnabled),
                    shizukuBridgeEnabled = obj.optBoolean("shizuku_bridge_enabled", cur.shizukuBridgeEnabled),
                    jailbreakModeEnabled = obj.optBoolean("jailbreak_mode_enabled", cur.jailbreakModeEnabled),
                    autoInstallAndroidPatch = obj.optBoolean("auto_install_android_patch", cur.autoInstallAndroidPatch),
                    startupSoundEnabled = obj.optBoolean("startup_sound_enabled", cur.startupSoundEnabled),
                    startupSoundAsset = obj.optString("startup_sound_asset", cur.startupSoundAsset),
                    bgmEnabled = obj.optBoolean("bgm_enabled", cur.bgmEnabled),
                )
            }
            true
        }.getOrDefault(false)
    }

    fun resetDefaults() {
        update { FolkPatchSettingsState() }
    }
}

