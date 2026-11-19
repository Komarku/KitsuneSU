package com.sukisu.nova.folkpatch.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukisu.nova.R
import com.sukisu.nova.folkpatch.ApNatives
import com.sukisu.nova.folkpatch.FolkPatchBootPatcher
import com.sukisu.nova.folkpatch.FolkPatchPrefs
import com.sukisu.nova.folkpatch.FolkPatchSoundPlayer
import com.sukisu.nova.folkpatch.HomeLayoutStyle
import com.sukisu.nova.folkpatch.InstallEngineMode
import com.sukisu.nova.ui.navigation3.LocalNavigator
import com.sukisu.nova.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class UnifiedSettingsCategory(val key: String, val labelRes: Int) {
    ALL("all", R.string.kitsune_cat_all),
    GENERAL("general", R.string.kitsune_cat_general),
    APPEARANCE("appearance", R.string.kitsune_cat_appearance),
    SECURITY("security", R.string.kitsune_cat_security),
    FUNCTION("function", R.string.kitsune_cat_function),
    MULTIMEDIA("multimedia", R.string.kitsune_cat_multimedia),
    BACKUP("backup", R.string.kitsune_cat_backup),
}

@Composable
fun FolkPatchUnifiedSettingsScreen(
    initialCategory: String? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by FolkPatchPrefs.state.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable {
        mutableStateOf(
            UnifiedSettingsCategory.entries.firstOrNull { it.key == initialCategory }
                ?: UnifiedSettingsCategory.ALL
        )
    }

    var showSuperKeyDialog by remember { mutableStateOf(false) }
    var showSuPathDialog by remember { mutableStateOf(false) }
    var showTitleDialog by remember { mutableStateOf(false) }
    var showBadgeTextDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val ok = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(FolkPatchPrefs.exportToJson().toByteArray(Charsets.UTF_8))
                    }
                    true
                }.getOrDefault(false)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (ok) R.string.kitsune_backup_exported else R.string.kitsune_backup_error,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val ok = runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@runCatching false
                    FolkPatchPrefs.importFromJson(text)
                }.getOrDefault(false)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (ok) R.string.kitsune_backup_imported else R.string.kitsune_backup_error,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    fun matchesQuery(vararg texts: String): Boolean {
        val q = searchQuery.trim()
        if (q.isEmpty()) return true
        return texts.any { it.contains(q, ignoreCase = true) }
    }

    fun showCat(cat: UnifiedSettingsCategory): Boolean {
        if (searchQuery.isNotBlank()) return true
        return selectedCategory == UnifiedSettingsCategory.ALL || selectedCategory == cat
    }

    if (showSuperKeyDialog) {
        SuperKeyAuthDialog(
            initialKey = state.superKey,
            rememberKey = state.storeSuperKey,
            onDismiss = { showSuperKeyDialog = false },
            onConfirm = { key, persist ->
                FolkPatchPrefs.setSuperKey(key, persist)
                val ready = ApNatives.isReady(key)
                val ver = if (ready) ApNatives.formatKernelPatchVersion(ApNatives.kernelPatchVersion(key)) else ""
                Toast.makeText(
                    context,
                    if (ready) {
                        context.getString(R.string.kitsune_kp_unlocked, ver)
                    } else {
                        context.getString(R.string.kitsune_kp_saved_inactive)
                    },
                    Toast.LENGTH_LONG,
                ).show()
                showSuperKeyDialog = false
            },
            onClear = {
                FolkPatchPrefs.clearSuperKey()
                Toast.makeText(context, R.string.kitsune_kp_cleared, Toast.LENGTH_SHORT).show()
                showSuperKeyDialog = false
            },
        )
    }

    if (showSuPathDialog) {
        TextInputDialog(
            title = stringResource(R.string.kitsune_set_su_path),
            label = stringResource(R.string.kitsune_set_su_path),
            initialValue = state.suPath,
            onDismiss = { showSuPathDialog = false },
            onConfirm = { newPath ->
                FolkPatchPrefs.update { it.copy(suPath = newPath) }
                scope.launch(Dispatchers.IO) { ApNatives.resetSuPath(newPath) }
                showSuPathDialog = false
            },
        )
    }

    if (showTitleDialog) {
        TextInputDialog(
            title = stringResource(R.string.kitsune_set_app_title),
            label = stringResource(R.string.kitsune_set_app_title),
            initialValue = state.customAppTitle,
            onDismiss = { showTitleDialog = false },
            onConfirm = { newTitle ->
                FolkPatchPrefs.update { it.copy(customAppTitle = newTitle.ifBlank { "KitsuneSU" }) }
                showTitleDialog = false
            },
        )
    }

    if (showBadgeTextDialog) {
        TextInputDialog(
            title = stringResource(R.string.kitsune_set_badge_text),
            label = stringResource(R.string.kitsune_set_badge_text),
            initialValue = state.customBadgeText,
            onDismiss = { showBadgeTextDialog = false },
            onConfirm = { newBadge ->
                FolkPatchPrefs.update { it.copy(customBadgeText = newBadge) }
                showBadgeTextDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.kitsune_unified_settings_title),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Поиск по настройкам
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.kitsune_settings_search_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            // Категории-фильтры
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UnifiedSettingsCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(stringResource(cat.labelRes)) },
                        )
                    }
                }
            }

            // === 1. ОСНОВНЫЕ И ДВИЖОК РУТА ===
            if (showCat(UnifiedSettingsCategory.GENERAL) && matchesQuery("основные", "движок", "init_boot", "boot", "superkey", "su", "заголовок", "mount", "профиль", "кэш", "general")) {
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.kitsune_sec_general),
                        icon = Icons.Rounded.Settings,
                    ) {
                        Text(
                            text = stringResource(R.string.kitsune_set_default_engine),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.defaultInstallEngine == InstallEngineMode.SUKISU_INIT_BOOT,
                                onClick = { FolkPatchPrefs.update { it.copy(defaultInstallEngine = InstallEngineMode.SUKISU_INIT_BOOT) } },
                                label = { Text(stringResource(R.string.kitsune_btn_install_init_boot)) },
                            )
                            FilterChip(
                                selected = state.defaultInstallEngine == InstallEngineMode.FOLKPATCH_BOOT,
                                onClick = { FolkPatchPrefs.update { it.copy(defaultInstallEngine = InstallEngineMode.FOLKPATCH_BOOT) } },
                                label = { Text(stringResource(R.string.kitsune_btn_install_boot)) },
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ClickableSettingRow(
                            title = stringResource(R.string.kitsune_set_superkey),
                            summary = if (state.superKey.isNotBlank()) {
                                stringResource(R.string.kitsune_set_superkey_set, state.superKey.length)
                            } else {
                                stringResource(R.string.kitsune_set_superkey_unset)
                            },
                            onClick = { showSuperKeyDialog = true },
                        )
                        ClickableSettingRow(
                            title = stringResource(R.string.kitsune_set_app_title),
                            summary = state.customAppTitle,
                            onClick = { showTitleDialog = true },
                        )
                        ClickableSettingRow(
                            title = stringResource(R.string.kitsune_set_su_path),
                            summary = state.suPath,
                            onClick = { showSuPathDialog = true },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_global_ns),
                            summary = stringResource(R.string.kitsune_set_global_ns_desc),
                            checked = state.globalNamespaceMode,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(globalNamespaceMode = v) } },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_magic_mount),
                            summary = stringResource(R.string.kitsune_set_magic_mount_desc),
                            checked = state.magicMountEnabled,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(magicMountEnabled = v) } },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.kitsune_set_new_app_profile),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                0 to R.string.kitsune_profile_default,
                                1 to R.string.kitsune_profile_exclude,
                                2 to R.string.kitsune_profile_grant,
                            ).forEach { (id, lblRes) ->
                                FilterChip(
                                    selected = state.newAppProfileMode == id,
                                    onClick = {
                                        FolkPatchPrefs.update { it.copy(newAppProfileMode = id) }
                                        scope.launch(Dispatchers.IO) { ApNatives.setNewAppProfileMode(id) }
                                    },
                                    label = { Text(stringResource(lblRes)) },
                                )
                            }
                        }
                        ClickableSettingRow(
                            title = stringResource(R.string.kitsune_set_clean_cache),
                            summary = stringResource(R.string.kitsune_set_clean_cache_desc),
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    FolkPatchBootPatcher.getPatchDir(context).deleteRecursively()
                                    File(context.cacheDir, "patch").deleteRecursively()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, R.string.kitsune_cache_cleaned, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        )
                    }
                }
            }

            // === 2. ОФОРМЛЕНИЕ И ДИЗАЙН ===
            if (showCat(UnifiedSettingsCategory.APPEARANCE) && matchesQuery("оформление", "дизайн", "тема", "amoled", "стекло", "liquid", "glass", "логотип", "иконка", "цвета", "фон", "стиль", "бейдж", "design")) {
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.kitsune_sec_appearance),
                        icon = Icons.Rounded.Palette,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { navigator.push(Route.AppIcon) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.kitsune_btn_all_logos), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { navigator.push(Route.ColorPalette()) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Rounded.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.kitsune_btn_palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { navigator.push(Route.CustomTexts) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(stringResource(R.string.kitsune_btn_custom_text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { navigator.push(Route.ThemeStore()) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(stringResource(R.string.kitsune_btn_theme_store), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Text(
                            text = stringResource(R.string.kitsune_set_home_style),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                HomeLayoutStyle.UNIFIED to R.string.kitsune_style_unified,
                                HomeLayoutStyle.CLASSIC to R.string.kitsune_style_classic,
                                HomeLayoutStyle.V2_GRID to R.string.kitsune_style_v2,
                                HomeLayoutStyle.V3_MAGISK to R.string.kitsune_style_v3,
                                HomeLayoutStyle.V4_HERO to R.string.kitsune_style_v4,
                                HomeLayoutStyle.CIRCLE to R.string.kitsune_style_circle,
                            ).forEach { (style, labelRes) ->
                                FilterChip(
                                    selected = state.homeLayoutStyle == style,
                                    onClick = { FolkPatchPrefs.update { it.copy(homeLayoutStyle = style) } },
                                    label = { Text(stringResource(labelRes)) },
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_amoled),
                            summary = stringResource(R.string.kitsune_set_amoled_desc),
                            checked = state.amoledPureBlack,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(amoledPureBlack = v) } },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_glass),
                            summary = stringResource(R.string.kitsune_set_glass_desc),
                            checked = state.liquidGlassEnabled,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(liquidGlassEnabled = v) } },
                        )
                        AnimatedVisibility(visible = state.liquidGlassEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SliderSettingRow(
                                    title = stringResource(R.string.kitsune_set_glass_blur),
                                    value = state.glassBlurStrength,
                                    valueRange = 0f..32f,
                                    valueLabel = "${state.glassBlurStrength.toInt()} dp",
                                    onValueChange = { v -> FolkPatchPrefs.update { it.copy(glassBlurStrength = v) } },
                                )
                                SliderSettingRow(
                                    title = stringResource(R.string.kitsune_set_glass_alpha),
                                    value = state.glassTransparency,
                                    valueRange = 0.25f..1f,
                                    valueLabel = "${(state.glassTransparency * 100).toInt()}%",
                                    onValueChange = { v -> FolkPatchPrefs.update { it.copy(glassTransparency = v) } },
                                )
                                SwitchSettingRow(
                                    title = stringResource(R.string.kitsune_set_glass_border),
                                    summary = stringResource(R.string.kitsune_set_glass_border_desc),
                                    checked = state.glassBorder,
                                    onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(glassBorder = v) } },
                                )
                            }
                        }
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_hide_badge),
                            summary = stringResource(R.string.kitsune_set_hide_badge_desc),
                            checked = state.hideStatusBadge,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(hideStatusBadge = v) } },
                        )
                        ClickableSettingRow(
                            title = stringResource(R.string.kitsune_set_badge_text),
                            summary = state.customBadgeText.ifBlank { stringResource(R.string.kitsune_set_badge_default) },
                            onClick = { showBadgeTextDialog = true },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_module_banner),
                            summary = stringResource(R.string.kitsune_set_module_banner_desc),
                            checked = state.moduleBannerEnabled,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(moduleBannerEnabled = v) } },
                        )
                    }
                }
            }

            // === 3. БЕЗОПАСНОСТЬ И СКРЫТИЕ РУТА ===
            if (showCat(UnifiedSettingsCategory.SECURITY) && matchesQuery("безопасность", "защита", "скрытие", "superkey", "биометрия", "pathhide", "netisolate", "susfs", "umount", "security")) {
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.kitsune_sec_security),
                        icon = Icons.Rounded.Security,
                    ) {
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_remember_key),
                            summary = stringResource(R.string.kitsune_set_remember_key_desc),
                            checked = state.storeSuperKey,
                            onCheckedChange = { v ->
                                FolkPatchPrefs.update {
                                    it.copy(storeSuperKey = v, superKey = if (v) it.superKey else "")
                                }
                            },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_biometric),
                            summary = stringResource(R.string.kitsune_set_biometric_desc),
                            checked = state.biometricAppLock,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(biometricAppLock = v) } },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_hide_app),
                            summary = stringResource(R.string.kitsune_set_hide_app_desc),
                            checked = state.hideAppDisguise,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(hideAppDisguise = v) } },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_pathhide),
                            summary = stringResource(R.string.kitsune_set_pathhide_desc),
                            checked = state.pathHideEnabled,
                            onCheckedChange = { v ->
                                FolkPatchPrefs.update { it.copy(pathHideEnabled = v) }
                                scope.launch(Dispatchers.IO) { ApNatives.setPathHideEnabled(v) }
                            },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_netisolate),
                            summary = stringResource(R.string.kitsune_set_netisolate_desc),
                            checked = state.netIsolateEnabled,
                            onCheckedChange = { v ->
                                FolkPatchPrefs.update { it.copy(netIsolateEnabled = v) }
                                scope.launch(Dispatchers.IO) { ApNatives.setNetIsolateEnabled(v) }
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { navigator.push(Route.SuSFS()) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("SuSFS", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { navigator.push(Route.UmountManager) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Umount", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // === 4. ФУНКЦИИ И МОДУЛИ ===
            if (showCat(UnifiedSettingsCategory.FUNCTION) && matchesQuery("функции", "модули", "shizuku", "jailbreak", "androidpatch", "zygisk", "kpm", "boot", "init_boot")) {
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.kitsune_sec_functions),
                        icon = Icons.Rounded.Build,
                    ) {
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_shizuku),
                            summary = stringResource(R.string.kitsune_set_shizuku_desc),
                            checked = state.shizukuBridgeEnabled,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(shizukuBridgeEnabled = v) } },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_jailbreak),
                            summary = stringResource(R.string.kitsune_set_jailbreak_desc),
                            checked = state.jailbreakModeEnabled,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(jailbreakModeEnabled = v) } },
                        )
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_auto_ap),
                            summary = stringResource(R.string.kitsune_set_auto_ap_desc),
                            checked = state.autoInstallAndroidPatch,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(autoInstallAndroidPatch = v) } },
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { navigator.push(Route.Install()) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(stringResource(R.string.kitsune_btn_install_init_boot), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = { navigator.push(Route.FolkPatchBootInstall(0)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text(stringResource(R.string.kitsune_btn_install_boot), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { navigator.push(Route.Kpm) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("KPM", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { navigator.push(Route.BuiltinZygisk) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Zygisk", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { navigator.push(Route.Tool) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Tools", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // === 5. ЗВУКИ ПРИ ЗАПУСКЕ ===
            if (showCat(UnifiedSettingsCategory.MULTIMEDIA) && matchesQuery("звук", "звуки", "музыка", "zako", "ciallo", "baka", "запуск", "sound")) {
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.kitsune_sec_multimedia),
                        icon = Icons.Rounded.MusicNote,
                    ) {
                        SwitchSettingRow(
                            title = stringResource(R.string.kitsune_set_startup_sound),
                            summary = stringResource(R.string.kitsune_set_startup_sound_desc),
                            checked = state.startupSoundEnabled,
                            onCheckedChange = { v -> FolkPatchPrefs.update { it.copy(startupSoundEnabled = v) } },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.kitsune_set_sound_pick),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        FolkPatchPrefs.AVAILABLE_SOUNDS.forEach { soundFile ->
                            val selected = state.startupSoundAsset == soundFile
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        FolkPatchPrefs.update { it.copy(startupSoundAsset = soundFile) }
                                        FolkPatchSoundPlayer.playAssetSound(context, soundFile)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = soundFile.removeSuffix(".wav"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                IconButton(
                                    onClick = {
                                        FolkPatchPrefs.update { it.copy(startupSoundAsset = soundFile) }
                                        FolkPatchSoundPlayer.playAssetSound(context, soundFile)
                                    },
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = soundFile)
                                }
                            }
                        }
                    }
                }
            }

            // === 6. РЕЗЕРВНОЕ КОПИРОВАНИЕ ===
            if (showCat(UnifiedSettingsCategory.BACKUP) && matchesQuery("бэкап", "резерв", "экспорт", "импорт", "сброс", "json", "backup")) {
                item {
                    SettingsSectionCard(
                        title = stringResource(R.string.kitsune_sec_backup),
                        icon = Icons.Rounded.Backup,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { exportLauncher.launch("kitsunesu_settings.json") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.kitsune_btn_export_json), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.kitsune_btn_import_json), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                FolkPatchPrefs.resetDefaults()
                                Toast.makeText(context, R.string.kitsune_reset_done, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.kitsune_btn_reset_defaults))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ClickableSettingRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SliderSettingRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(text = valueLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
fun SuperKeyAuthDialog(
    initialKey: String,
    rememberKey: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit,
    onClear: () -> Unit,
) {
    var key by remember { mutableStateOf(initialKey) }
    var persist by remember { mutableStateOf(rememberKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Key, contentDescription = null) },
        title = { Text(stringResource(R.string.kitsune_dlg_superkey_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.kitsune_dlg_superkey_desc),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("SuperKey") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.kitsune_set_remember_key), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = persist, onCheckedChange = { persist = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(key.trim(), persist) },
                enabled = key.isNotBlank(),
            ) {
                Text(stringResource(R.string.kitsune_dlg_auth))
            }
        },
        dismissButton = {
            Row {
                if (initialKey.isNotBlank()) {
                    TextButton(onClick = onClear) { Text(stringResource(R.string.kitsune_dlg_clear)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.kitsune_dlg_cancel)) }
            }
        },
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value.trim()) }) { Text(stringResource(R.string.kitsune_dlg_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.kitsune_dlg_cancel)) }
        },
    )
}
