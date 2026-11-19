package com.sukisu.nova.folkpatch.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukisu.nova.R
import com.sukisu.nova.folkpatch.ApNatives
import com.sukisu.nova.folkpatch.EmbeddedKpmItem
import com.sukisu.nova.folkpatch.FolkKImgInfo
import com.sukisu.nova.folkpatch.FolkKpImgInfo
import com.sukisu.nova.folkpatch.FolkPatchBootPatcher
import com.sukisu.nova.folkpatch.FolkPatchMode
import com.sukisu.nova.folkpatch.FolkPatchPrefs
import com.sukisu.nova.folkpatch.InstallEngineMode
import com.sukisu.nova.ui.navigation3.LocalNavigator
import com.sukisu.nova.ui.navigation3.Route
import com.sukisu.nova.ui.util.reboot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Prominent engine switcher shown in Install screens so users can switch between:
 *  - SukiSU (`init_boot` / LKM / GKI / AnyKernel3)
 *  - FolkPatch (`boot` / KernelPatch / SuperKey / KPM Embed)
 */
@Composable
fun KitsuneInstallEngineSwitcher(
    selectedEngine: InstallEngineMode,
    onSelectEngine: (InstallEngineMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fpState by FolkPatchPrefs.state.collectAsStateWithLifecycle()
    val folkPatchActive = remember(fpState.superKey) { ApNatives.isReady(fpState.superKey) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                InstallEngineTabItem(
                    title = stringResource(R.string.kitsune_engine_sukisu_title),
                    subtitle = "init_boot · LKM / GKI",
                    selected = selectedEngine == InstallEngineMode.SUKISU_INIT_BOOT,
                    onClick = { onSelectEngine(InstallEngineMode.SUKISU_INIT_BOOT) },
                    modifier = Modifier.weight(1f),
                )
                InstallEngineTabItem(
                    title = stringResource(R.string.kitsune_engine_folkpatch_title),
                    subtitle = "boot · KernelPatch",
                    selected = selectedEngine == InstallEngineMode.FOLKPATCH_BOOT,
                    onClick = { onSelectEngine(InstallEngineMode.FOLKPATCH_BOOT) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (selectedEngine == InstallEngineMode.SUKISU_INIT_BOOT && folkPatchActive) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.kitsune_conflict_folkpatch_active_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = stringResource(R.string.kitsune_conflict_folkpatch_active_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallEngineTabItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = fg.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolkPatchBootInstallScreen(
    initialMode: Int = 0,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val fpSettings by FolkPatchPrefs.state.collectAsStateWithLifecycle()

    var patchMode by rememberSaveable {
        mutableStateOf(
            FolkPatchMode.entries.getOrElse(initialMode) { FolkPatchMode.PATCH_ONLY }
        )
    }
    var selectedBootUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var selectedBootName by rememberSaveable { mutableStateOf("") }
    var customKpImgUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var customKpImgName by rememberSaveable { mutableStateOf("") }

    var useCustomSuperKey by rememberSaveable { mutableStateOf(fpSettings.customSuperKeyPatch) }
    var superKey by rememberSaveable {
        mutableStateOf(
            fpSettings.superKey.ifBlank {
                ApNatives.superKey.takeUnless { it == "su" }.orEmpty()
                    .ifBlank { FolkPatchBootPatcher.generateRandomSuperKey() }
            }
        )
    }
    var superKeyVisible by rememberSaveable { mutableStateOf(true) }

    var kpInfo by remember { mutableStateOf(FolkKpImgInfo(version = "0.13.9")) }
    var kimgInfo by remember { mutableStateOf(FolkKImgInfo()) }
    val embeddedKpms = remember { mutableStateListOf<EmbeddedKpmItem>() }

    var isWorking by remember { mutableStateOf(false) }
    var needReboot by remember { mutableStateOf(false) }
    var outputPath by remember { mutableStateOf<String?>(null) }
    val logLines = remember { mutableStateListOf<String>() }
    val logScrollState = rememberScrollState()
    val sukiSuActive = remember { com.sukisu.nova.Natives.isManager }

    fun appendLog(line: String) {
        scope.launch(Dispatchers.Main) {
            logLines.add(line)
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val (_, info) = FolkPatchBootPatcher.prepareWorkspace(
                context = context,
                requireRoot = false,
            )
            kpInfo = info
        }
    }

    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            logScrollState.animateScrollTo(logScrollState.maxValue)
        }
    }

    val pickBootLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedBootUri = uri
            scope.launch {
                isWorking = true
                runCatching {
                    val (info, name) = FolkPatchBootPatcher.inspectBootImage(
                        context = context,
                        bootUri = uri,
                        onLog = ::appendLog,
                    )
                    kimgInfo = info
                    selectedBootName = name
                }.onFailure {
                    appendLog("! Boot inspect error: ${it.message}")
                }
                isWorking = false
            }
        }
    }

    val pickKpImgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            customKpImgUri = uri
            customKpImgName = uri.lastPathSegment?.substringAfterLast('/') ?: "custom_kpimg"
            appendLog("- Custom kpimg selected: $customKpImgName")
        }
    }

    val pickKpmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val item = FolkPatchBootPatcher.inspectAndStageKpm(context, uri)
                if (item != null) {
                    embeddedKpms.add(item)
                    appendLog("- Added KPM: ${item.name} (${item.version})")
                } else {
                    Toast.makeText(context, R.string.kitsune_kpm_load_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.kitsune_folkpatch_boot_title),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "KernelPatch v${kpInfo.version.ifBlank { "0.13.9" }} · boot.img",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            if (needReboot) {
                FloatingActionButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) { reboot() }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reboot")
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Engine Switcher Header (jump straight back to SukiSU init_boot)
            item {
                KitsuneInstallEngineSwitcher(
                    selectedEngine = InstallEngineMode.FOLKPATCH_BOOT,
                    onSelectEngine = { mode ->
                        if (mode == InstallEngineMode.SUKISU_INIT_BOOT) {
                            navigator.replace(Route.Install())
                        }
                    },
                )
            }

            // KitsuneSU x FolkPatch Banner Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.kitsune_logo_hero),
                            contentDescription = "KitsuneSU",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFF3EC)),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.kitsune_fp_banner_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.kitsune_fp_banner_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (kpInfo.compileTime.isNotBlank()) {
                                Text(
                                    text = "kpimg ${kpInfo.version} · ${kpInfo.compileTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // Patch Mode Selection
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.kitsune_fp_mode_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = patchMode == FolkPatchMode.PATCH_ONLY,
                                onClick = { patchMode = FolkPatchMode.PATCH_ONLY },
                                label = { Text(stringResource(R.string.kitsune_fp_mode_patch_file)) },
                            )
                            FilterChip(
                                selected = patchMode == FolkPatchMode.PATCH_AND_INSTALL,
                                onClick = { patchMode = FolkPatchMode.PATCH_AND_INSTALL },
                                label = { Text(stringResource(R.string.kitsune_fp_mode_direct)) },
                            )
                            FilterChip(
                                selected = patchMode == FolkPatchMode.INSTALL_TO_NEXT_SLOT,
                                onClick = { patchMode = FolkPatchMode.INSTALL_TO_NEXT_SLOT },
                                label = { Text(stringResource(R.string.kitsune_fp_mode_next_slot)) },
                            )
                            FilterChip(
                                selected = patchMode == FolkPatchMode.UNPATCH,
                                onClick = { patchMode = FolkPatchMode.UNPATCH },
                                label = { Text(stringResource(R.string.kitsune_fp_mode_unpatch)) },
                            )
                        }
                    }
                }
            }

            // Boot.img selector (for PATCH_ONLY)
            if (patchMode == FolkPatchMode.PATCH_ONLY) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.kitsune_fp_select_boot),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { pickBootLauncher.launch(arrayOf("*/*")) },
                                    enabled = !isWorking,
                                ) {
                                    Text(
                                        text = if (selectedBootUri == null) {
                                            stringResource(R.string.kitsune_fp_choose_file)
                                        } else {
                                            stringResource(R.string.kitsune_fp_change_file)
                                        }
                                    )
                                }
                            }
                            if (selectedBootName.isNotBlank()) {
                                Text(
                                    text = selectedBootName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (kimgInfo.banner.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = kimgInfo.banner.trim(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (kimgInfo.patched) {
                                                stringResource(R.string.kitsune_fp_kernel_patched)
                                            } else {
                                                stringResource(R.string.kitsune_fp_kernel_stock)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (kimgInfo.patched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SuperKey & KernelPatch Configuration (for patch modes)
            if (patchMode != FolkPatchMode.UNPATCH) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.kitsune_fp_superkey_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Switch(
                                    checked = useCustomSuperKey,
                                    onCheckedChange = {
                                        useCustomSuperKey = it
                                        FolkPatchPrefs.update { st -> st.copy(customSuperKeyPatch = it) }
                                    },
                                )
                            }

                            AnimatedVisibility(visible = useCustomSuperKey) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val keyValid = FolkPatchBootPatcher.isSuperKeyValid(superKey)
                                    OutlinedTextField(
                                        value = superKey,
                                        onValueChange = { superKey = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(stringResource(R.string.kitsune_fp_superkey_field)) },
                                        singleLine = true,
                                        isError = superKey.isNotEmpty() && !keyValid,
                                        visualTransformation = if (superKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            Row {
                                                IconButton(onClick = {
                                                    superKey = FolkPatchBootPatcher.generateRandomSuperKey()
                                                }) {
                                                    Icon(Icons.Rounded.Casino, contentDescription = "Random SuperKey")
                                                }
                                                IconButton(onClick = { superKeyVisible = !superKeyVisible }) {
                                                    Icon(
                                                        if (superKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        },
                                    )
                                    Text(
                                        text = if (keyValid) {
                                            stringResource(R.string.kitsune_fp_superkey_valid)
                                        } else {
                                            stringResource(R.string.kitsune_fp_superkey_hint)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (keyValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Custom kpimg option
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.kitsune_fp_custom_kpimg),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = customKpImgName.ifBlank { stringResource(R.string.kitsune_fp_builtin_kpimg, kpInfo.version) },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { pickKpImgLauncher.launch(arrayOf("*/*")) },
                                    enabled = !isWorking,
                                ) {
                                    Text(stringResource(R.string.kitsune_fp_choose_file))
                                }
                            }
                        }
                    }
                }

                // Embed KPM Modules Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.kitsune_fp_embed_kpm),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { pickKpmLauncher.launch(arrayOf("*/*")) },
                                    enabled = !isWorking,
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("KPM")
                                }
                            }
                            if (embeddedKpms.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.kitsune_fp_embed_kpm_empty),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                embeddedKpms.forEachIndexed { index, item ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${item.name} ${item.version}".trim(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                    if (item.author.isNotBlank()) {
                                                        Text(
                                                            text = item.author,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                }
                                                IconButton(onClick = { embeddedKpms.removeAt(index) }) {
                                                    Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                                                }
                                            }
                                                OutlinedTextField(
                                                    value = item.args,
                                                    onValueChange = { newArgs ->
                                                        embeddedKpms[index] = item.copy(args = newArgs)
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    label = { Text(stringResource(R.string.kitsune_fp_kpm_args)) },
                                                    singleLine = true,
                                                )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Conflict Guard: warn & guard direct boot flash if SukiSU (init_boot) is already active
            if (sukiSuActive) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.kitsune_conflict_sukisu_active_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = stringResource(R.string.kitsune_conflict_sukisu_active_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            // Primary Patch / Flash Action Button + AndroidPatch actions
            item {
                val directFlashBlockedByConflict = sukiSuActive &&
                    (patchMode == FolkPatchMode.PATCH_AND_INSTALL || patchMode == FolkPatchMode.INSTALL_TO_NEXT_SLOT)
                val canStart = !isWorking && !directFlashBlockedByConflict && when (patchMode) {
                    FolkPatchMode.PATCH_ONLY -> selectedBootUri != null && (!useCustomSuperKey || FolkPatchBootPatcher.isSuperKeyValid(superKey))
                    FolkPatchMode.UNPATCH -> true
                    else -> !useCustomSuperKey || FolkPatchBootPatcher.isSuperKeyValid(superKey)
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                isWorking = true
                                needReboot = false
                                outputPath = null
                                logLines.clear()
                                val keyToUse = if (useCustomSuperKey) superKey.trim() else "su"
                                val result = FolkPatchBootPatcher.executePatch(
                                    context = context,
                                    mode = patchMode,
                                    superKey = keyToUse,
                                    bootUri = selectedBootUri,
                                    customKpImgUri = customKpImgUri,
                                    embeddedKpms = embeddedKpms.toList(),
                                    onLog = ::appendLog,
                                )
                                isWorking = false
                                needReboot = result.needReboot
                                outputPath = result.outputPath
                                if (result.success) {
                                    Toast.makeText(context, R.string.kitsune_fp_patch_success, Toast.LENGTH_LONG).show()
                                } else {
                                    appendLog("! ERROR: ${result.error ?: "Unknown error"}")
                                    Toast.makeText(context, result.error ?: "Patch failed", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = canStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(10.dp))
                        } else {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = when (patchMode) {
                                FolkPatchMode.PATCH_ONLY -> stringResource(R.string.kitsune_fp_btn_patch_boot)
                                FolkPatchMode.PATCH_AND_INSTALL -> stringResource(R.string.kitsune_fp_btn_direct_flash)
                                FolkPatchMode.INSTALL_TO_NEXT_SLOT -> stringResource(R.string.kitsune_fp_btn_next_slot)
                                FolkPatchMode.UNPATCH -> stringResource(R.string.kitsune_fp_btn_unpatch)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // AndroidPatch System Daemon (apd) Install / Uninstall Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isWorking = true
                                    logLines.clear()
                                    appendLog("- Installing AndroidPatch (apd)...")
                                    val ok = FolkPatchBootPatcher.installAndroidPatch(context, ::appendLog)
                                    isWorking = false
                                    Toast.makeText(
                                        context,
                                        if (ok) "AndroidPatch installed!" else "AndroidPatch install failed (root required)",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            enabled = !isWorking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.kitsune_fp_install_ap), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isWorking = true
                                    logLines.clear()
                                    appendLog("- Uninstalling AndroidPatch (apd)...")
                                    val ok = FolkPatchBootPatcher.uninstallAndroidPatch(context, ::appendLog)
                                    isWorking = false
                                    Toast.makeText(
                                        context,
                                        if (ok) "AndroidPatch uninstalled!" else "AndroidPatch uninstall failed",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            enabled = !isWorking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.kitsune_fp_uninstall_ap), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Output result card
            if (!outputPath.isNullOrBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.kitsune_fp_output_ready),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = outputPath.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            }

            // Live Console Output Card
            if (logLines.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14161A)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Terminal, contentDescription = null, tint = Color(0xFF7EE787), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.kitsune_fp_console),
                                        color = Color(0xFFE6EDF3),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(logLines.joinToString("\n")))
                                        Toast.makeText(context, R.string.kitsune_fp_log_copied, Toast.LENGTH_SHORT).show()
                                    },
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = Color(0xFF8B949E), modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            SelectionContainer {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 300.dp)
                                        .verticalScroll(logScrollState)
                                        .horizontalScroll(rememberScrollState()),
                                ) {
                                    Text(
                                        text = logLines.joinToString("\n"),
                                        color = Color(0xFFD0D7DE),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

