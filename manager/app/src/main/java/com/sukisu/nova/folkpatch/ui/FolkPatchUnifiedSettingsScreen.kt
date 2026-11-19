package com.sukisu.nova.folkpatch.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukisu.nova.R
import com.sukisu.nova.folkpatch.ApNatives
import com.sukisu.nova.folkpatch.FolkPatchBootPatcher
import com.sukisu.nova.folkpatch.FolkPatchPrefs
import com.sukisu.nova.ui.LocalUiMode
import com.sukisu.nova.ui.UiMode
import com.sukisu.nova.ui.component.material.ExpressiveScaffold
import com.sukisu.nova.ui.component.material.IconTile
import com.sukisu.nova.ui.theme.MaterialExpressivePalette
import com.sukisu.nova.ui.component.material.SegmentedColumn
import com.sukisu.nova.ui.component.material.SegmentedListItem
import com.sukisu.nova.ui.component.material.SegmentedSwitchItem
import com.sukisu.nova.ui.component.material.TopBarBackButton
import com.sukisu.nova.ui.component.material.expressiveTopAppBarColors
import com.sukisu.nova.ui.component.miuix.AppCard
import com.sukisu.nova.ui.component.miuix.AppScaffold
import com.sukisu.nova.ui.navigation3.LocalNavigator
import com.sukisu.nova.ui.navigation3.Route
import com.sukisu.nova.ui.theme.LocalBackgroundImagePath
import com.sukisu.nova.ui.theme.LocalEnableBlur
import com.sukisu.nova.ui.theme.backgroundAwareCardColors
import com.sukisu.nova.ui.util.BlurredBar
import com.sukisu.nova.ui.util.rememberBlurBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme as miuixColorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles as miuixTextStyles
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File

@Composable
fun FolkPatchUnifiedSettingsScreen(
    @Suppress("UNUSED_PARAMETER") initialCategory: String? = null,
) {
    val navigator = LocalNavigator.current
    val onBack = { navigator.pop() }

    when (LocalUiMode.current) {
        UiMode.Miuix -> FolkPatchSettingsMiuix(onBack = onBack)
        UiMode.Material -> MaterialExpressivePalette {
            FolkPatchSettingsMaterial(onBack = onBack)
        }
    }
}

@Composable
private fun FolkPatchSettingsMaterial(
    onBack: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by FolkPatchPrefs.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showSuperKeyDialog by remember { mutableStateOf(false) }

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

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.kitsune_unified_settings_title)) },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                scrollBehavior = scrollBehavior,
                colors = expressiveTopAppBarColors(),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // 1. SuperKey & Авторизация
            Text(
                text = stringResource(R.string.kitsune_sec_superkey),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
            SegmentedColumn(
                modifier = Modifier.padding(bottom = 8.dp),
                content = listOf(
                    {
                        SegmentedListItem(
                            onClick = { showSuperKeyDialog = true },
                            headlineContent = { Text(stringResource(R.string.kitsune_set_superkey)) },
                            supportingContent = {
                                Text(
                                    if (state.superKey.isNotBlank()) {
                                        stringResource(R.string.kitsune_set_superkey_set, state.superKey.length)
                                    } else {
                                        stringResource(R.string.kitsune_set_superkey_unset)
                                    }
                                )
                            },
                            leadingContent = { IconTile(Icons.Rounded.Key, stringResource(R.string.kitsune_set_superkey)) },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                            },
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.Lock,
                            title = stringResource(R.string.kitsune_set_remember_key),
                            summary = stringResource(R.string.kitsune_set_remember_key_desc),
                            checked = state.storeSuperKey,
                            onCheckedChange = { v ->
                                FolkPatchPrefs.update {
                                    it.copy(storeSuperKey = v, superKey = if (v) it.superKey else "")
                                }
                            },
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.Fingerprint,
                            title = stringResource(R.string.kitsune_set_biometric),
                            summary = stringResource(R.string.kitsune_set_biometric_desc),
                            checked = state.biometricAppLock,
                            onCheckedChange = { v ->
                                FolkPatchPrefs.update { it.copy(biometricAppLock = v) }
                            },
                        )
                    },
                ),
            )

            // 2. Установка и патчинг boot.img
            Text(
                text = stringResource(R.string.kitsune_sec_boot_patcher),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
            SegmentedColumn(
                modifier = Modifier.padding(bottom = 8.dp),
                content = listOf(
                    {
                        SegmentedListItem(
                            onClick = { navigator.push(Route.FolkPatchBootInstall(0)) },
                            headlineContent = { Text(stringResource(R.string.kitsune_folkpatch_boot_title)) },
                            supportingContent = { Text(stringResource(R.string.kitsune_fp_banner_subtitle)) },
                            leadingContent = { IconTile(Icons.Rounded.Build, stringResource(R.string.kitsune_folkpatch_boot_title)) },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                            },
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    FolkPatchBootPatcher.getPatchDir(context).deleteRecursively()
                                    File(context.cacheDir, "patch").deleteRecursively()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, R.string.kitsune_cache_cleaned, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            headlineContent = { Text(stringResource(R.string.kitsune_set_clean_cache)) },
                            supportingContent = { Text(stringResource(R.string.kitsune_set_clean_cache_desc)) },
                            leadingContent = { IconTile(Icons.Rounded.LayersClear, stringResource(R.string.kitsune_set_clean_cache)) },
                        )
                    },
                ),
            )

            // 3. Функции ядра KernelPatch
            Text(
                text = stringResource(R.string.kitsune_sec_kp_features),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
            SegmentedColumn(
                modifier = Modifier.padding(bottom = 12.dp),
                content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.VisibilityOff,
                            title = stringResource(R.string.kitsune_set_pathhide),
                            summary = stringResource(R.string.kitsune_set_pathhide_desc),
                            checked = state.pathHideEnabled,
                            onCheckedChange = { v ->
                                FolkPatchPrefs.update { it.copy(pathHideEnabled = v) }
                                scope.launch(Dispatchers.IO) { ApNatives.setPathHideEnabled(v) }
                            },
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.Block,
                            title = stringResource(R.string.kitsune_set_netisolate),
                            summary = stringResource(R.string.kitsune_set_netisolate_desc),
                            checked = state.netIsolateEnabled,
                            onCheckedChange = { v ->
                                FolkPatchPrefs.update { it.copy(netIsolateEnabled = v) }
                                scope.launch(Dispatchers.IO) { ApNatives.setNetIsolateEnabled(v) }
                            },
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.Folder,
                            title = stringResource(R.string.kitsune_set_magic_mount),
                            summary = stringResource(R.string.kitsune_set_magic_mount_desc),
                            checked = state.magicMountEnabled,
                            onCheckedChange = { v ->
                                FolkPatchPrefs.update { it.copy(magicMountEnabled = v) }
                            },
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Rounded.Code,
                            title = stringResource(R.string.kitsune_set_shizuku),
                            summary = stringResource(R.string.kitsune_set_shizuku_desc),
                            checked = state.shizukuBridgeEnabled,
                            onCheckedChange = { v ->
                                FolkPatchPrefs.update { it.copy(shizukuBridgeEnabled = v) }
                            },
                        )
                    },
                ),
            )

            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                ),
            )
        }
    }
}

@Composable
private fun FolkPatchSettingsMiuix(
    onBack: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by FolkPatchPrefs.state.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val hasCustomBackground = LocalBackgroundImagePath.current.isNotEmpty()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive || hasCustomBackground) Color.Transparent else miuixColorScheme.surface

    var showSuperKeyDialog by remember { mutableStateOf(false) }

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

    AppScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixTopAppBar(
                    title = stringResource(R.string.kitsune_unified_settings_title),
                    color = barColor,
                    navigationIcon = {
                        MiuixIconButton(onClick = onBack) {
                            val layoutDirection = LocalLayoutDirection.current
                            MiuixIcon(
                                modifier = Modifier.graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                },
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
        ) {
            // 1. SuperKey & Авторизация
            item {
                MiuixText(
                    text = stringResource(R.string.kitsune_sec_superkey),
                    style = miuixTextStyles.title4,
                    color = miuixColorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                )
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = backgroundAwareCardColors(),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.kitsune_set_superkey),
                        summary = if (state.superKey.isNotBlank()) {
                            stringResource(R.string.kitsune_set_superkey_set, state.superKey.length)
                        } else {
                            stringResource(R.string.kitsune_set_superkey_unset)
                        },
                        startAction = {
                            Icon(
                                Icons.Rounded.Key,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        },
                        onClick = { showSuperKeyDialog = true },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.kitsune_set_remember_key),
                        summary = stringResource(R.string.kitsune_set_remember_key_desc),
                        startAction = {
                            Icon(
                                Icons.Rounded.Lock,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        },
                        checked = state.storeSuperKey,
                        onCheckedChange = { v ->
                            FolkPatchPrefs.update {
                                it.copy(storeSuperKey = v, superKey = if (v) it.superKey else "")
                            }
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.kitsune_set_biometric),
                        summary = stringResource(R.string.kitsune_set_biometric_desc),
                        startAction = {
                            Icon(
                                Icons.Rounded.Fingerprint,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        },
                        checked = state.biometricAppLock,
                        onCheckedChange = { v ->
                            FolkPatchPrefs.update { it.copy(biometricAppLock = v) }
                        },
                    )
                }
            }

            // 2. Установка и патчинг boot.img
            item {
                MiuixText(
                    text = stringResource(R.string.kitsune_sec_boot_patcher),
                    style = miuixTextStyles.title4,
                    color = miuixColorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                )
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = backgroundAwareCardColors(),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.kitsune_folkpatch_boot_title),
                        summary = stringResource(R.string.kitsune_fp_banner_subtitle),
                        startAction = {
                            Icon(
                                Icons.Rounded.Build,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        },
                        onClick = { navigator.push(Route.FolkPatchBootInstall(0)) },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.kitsune_set_clean_cache),
                        summary = stringResource(R.string.kitsune_set_clean_cache_desc),
                        startAction = {
                            Icon(
                                Icons.Rounded.LayersClear,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        },
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

            // 3. Функции ядра KernelPatch
            item {
                MiuixText(
                    text = stringResource(R.string.kitsune_sec_kp_features),
                    style = miuixTextStyles.title4,
                    color = miuixColorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                )
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = backgroundAwareCardColors(),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.kitsune_set_pathhide),
                        summary = stringResource(R.string.kitsune_set_pathhide_desc),
                        startAction = {
                            Icon(
                                Icons.Rounded.VisibilityOff,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        },
                        checked = state.pathHideEnabled,
                        onCheckedChange = { v ->
                            FolkPatchPrefs.update { it.copy(pathHideEnabled = v) }
                            scope.launch(Dispatchers.IO) { ApNatives.setPathHideEnabled(v) }
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.kitsune_set_netisolate),
                        summary = stringResource(R.string.kitsune_set_netisolate_desc),
                        startAction = {
                            Icon(
                                Icons.Rounded.Block,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        },
                        checked = state.netIsolateEnabled,
                        onCheckedChange = { v ->
                            FolkPatchPrefs.update { it.copy(netIsolateEnabled = v) }
                            scope.launch(Dispatchers.IO) { ApNatives.setNetIsolateEnabled(v) }
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.kitsune_set_magic_mount),
                        summary = stringResource(R.string.kitsune_set_magic_mount_desc),
                        startAction = {
                            Icon(
                                Icons.Rounded.Folder,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        },
                        checked = state.magicMountEnabled,
                        onCheckedChange = { v ->
                            FolkPatchPrefs.update { it.copy(magicMountEnabled = v) }
                        },
                    )
                    SwitchPreference(
                        title = stringResource(R.string.kitsune_set_shizuku),
                        summary = stringResource(R.string.kitsune_set_shizuku_desc),
                        startAction = {
                            Icon(
                                Icons.Rounded.Code,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = miuixColorScheme.onBackground,
                            )
                        },
                        checked = state.shizukuBridgeEnabled,
                        onCheckedChange = { v ->
                            FolkPatchPrefs.update { it.copy(shizukuBridgeEnabled = v) }
                        },
                    )
                }
            }
        }
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
