package com.sukisu.nova.folkpatch.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sukisu.nova.R
import com.sukisu.nova.folkpatch.BankAppItem
import com.sukisu.nova.folkpatch.KitsuneBankHideManager
import com.sukisu.nova.ui.LocalUiMode
import com.sukisu.nova.ui.UiMode
import com.sukisu.nova.ui.component.AppIconImage
import com.sukisu.nova.ui.component.material.ExpressiveScaffold
import com.sukisu.nova.ui.component.material.SegmentedColumn
import com.sukisu.nova.ui.component.material.TopBarBackButton
import com.sukisu.nova.ui.component.material.expressiveTopAppBarColors
import com.sukisu.nova.ui.component.miuix.AppCard
import com.sukisu.nova.ui.component.miuix.AppScaffold
import com.sukisu.nova.ui.navigation3.LocalNavigator
import com.sukisu.nova.ui.theme.LocalBackgroundImagePath
import com.sukisu.nova.ui.theme.LocalEnableBlur
import com.sukisu.nova.ui.theme.MaterialExpressivePalette
import com.sukisu.nova.ui.theme.backgroundAwareCardColors
import com.sukisu.nova.ui.util.BlurredBar
import com.sukisu.nova.ui.util.rememberBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme as miuixColorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles as miuixTextStyles
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun KitsuneBankHideScreen() {
    val navigator = LocalNavigator.current
    val onBack = { navigator.pop() }

    when (LocalUiMode.current) {
        UiMode.Miuix -> BankHideMiuix(onBack = onBack)
        UiMode.Material -> MaterialExpressivePalette {
            BankHideMaterial(onBack = onBack)
        }
    }
}

@Composable
private fun BankHideMaterial(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var apps by remember { mutableStateOf<List<BankAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var onlyBanks by remember { mutableStateOf(true) }

    fun refreshApps() {
        scope.launch {
            isLoading = true
            apps = KitsuneBankHideManager.loadApps(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshApps()
    }

    val filteredApps = remember(apps, searchQuery, onlyBanks) {
        apps.filter { item ->
            val matchBank = !onlyBanks || item.isBankingApp || item.isHidden
            val matchQuery = searchQuery.isBlank() ||
                    item.label.contains(searchQuery, ignoreCase = true) ||
                    item.packageName.contains(searchQuery, ignoreCase = true)
            matchBank && matchQuery
        }
    }

    val hiddenCount = remember(apps) { apps.count { it.isHidden } }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.kitsune_bank_hide_title)) },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                scrollBehavior = scrollBehavior,
                colors = expressiveTopAppBarColors(),
            )
        },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 16.dp),
        ) {
            // Главная карточка щита
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.kitsune_bank_hide_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.kitsune_bank_protected_count, hiddenCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.kitsune_bank_hide_card_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val count = KitsuneBankHideManager.hideAllBanks(context)
                                refreshApps()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.kitsune_bank_hide_toast_all, count),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.kitsune_bank_hide_btn_all))
                    }
                }
            }

            // Поиск и фильтры
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text(stringResource(R.string.kitsune_bank_hide_search)) },
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = onlyBanks,
                    onClick = { onlyBanks = true },
                    label = { Text(stringResource(R.string.kitsune_bank_hide_filter_banks)) },
                    leadingIcon = { Icon(Icons.Rounded.Security, null, modifier = Modifier.size(16.dp)) },
                )
                FilterChip(
                    selected = !onlyBanks,
                    onClick = { onlyBanks = false },
                    label = { Text(stringResource(R.string.kitsune_bank_hide_filter_all)) },
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(filteredApps, key = { it.packageName }) { appItem ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIconImage(
                                    packageInfo = appItem.packageInfo,
                                    label = appItem.label,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = appItem.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (appItem.isBankingApp) {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.kitsune_bank_badge),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = appItem.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = appItem.isHidden,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            KitsuneBankHideManager.setAppHidden(
                                                context = context,
                                                packageName = appItem.packageName,
                                                uid = appItem.uid,
                                                enable = checked,
                                            )
                                            apps = apps.map {
                                                if (it.packageName == appItem.packageName) it.copy(isHidden = checked) else it
                                            }
                                            val msg = if (checked) {
                                                context.getString(R.string.kitsune_bank_hide_toast_applied, appItem.label)
                                            } else {
                                                context.getString(R.string.kitsune_bank_hide_toast_reverted, appItem.label)
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BankHideMiuix(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val hasCustomBackground = LocalBackgroundImagePath.current.isNotEmpty()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive || hasCustomBackground) Color.Transparent else miuixColorScheme.surface

    var apps by remember { mutableStateOf<List<BankAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var onlyBanks by remember { mutableStateOf(true) }

    fun refreshApps() {
        scope.launch {
            isLoading = true
            apps = KitsuneBankHideManager.loadApps(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshApps()
    }

    val filteredApps = remember(apps, searchQuery, onlyBanks) {
        apps.filter { item ->
            val matchBank = !onlyBanks || item.isBankingApp || item.isHidden
            val matchQuery = searchQuery.isBlank() ||
                    item.label.contains(searchQuery, ignoreCase = true) ||
                    item.packageName.contains(searchQuery, ignoreCase = true)
            matchBank && matchQuery
        }
    }

    val hiddenCount = remember(apps) { apps.count { it.isHidden } }

    AppScaffold(
        topBar = {
            BlurredBar(backdrop) {
                MiuixTopAppBar(
                    title = stringResource(R.string.kitsune_bank_hide_title),
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
        Column(
            modifier = Modifier
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Карточка щита
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = backgroundAwareCardColors(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = miuixColorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            MiuixText(
                                text = stringResource(R.string.kitsune_bank_hide_card_title),
                                style = miuixTextStyles.title3,
                            )
                            MiuixText(
                                text = stringResource(R.string.kitsune_bank_protected_count, hiddenCount),
                                style = miuixTextStyles.body2,
                                color = miuixColorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MiuixText(
                        text = stringResource(R.string.kitsune_bank_hide_card_desc),
                        style = miuixTextStyles.body2,
                        color = miuixColorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(12.dp))
                    MiuixButton(
                        onClick = {
                            scope.launch {
                                val count = KitsuneBankHideManager.hideAllBanks(context)
                                refreshApps()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.kitsune_bank_hide_toast_all, count),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MiuixText(text = stringResource(R.string.kitsune_bank_hide_btn_all))
                    }
                }
            }

            // Поиск и фильтры
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text(stringResource(R.string.kitsune_bank_hide_search)) },
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = onlyBanks,
                    onClick = { onlyBanks = true },
                    label = { Text(stringResource(R.string.kitsune_bank_hide_filter_banks)) },
                    leadingIcon = { Icon(Icons.Rounded.Security, null, modifier = Modifier.size(16.dp)) },
                )
                FilterChip(
                    selected = !onlyBanks,
                    onClick = { onlyBanks = false },
                    label = { Text(stringResource(R.string.kitsune_bank_hide_filter_all)) },
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .scrollEndHaptic()
                        .overScrollVertical(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(filteredApps, key = { it.packageName }) { appItem ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = backgroundAwareCardColors(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIconImage(
                                    packageInfo = appItem.packageInfo,
                                    label = appItem.label,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        MiuixText(
                                            text = appItem.label,
                                            style = miuixTextStyles.title4,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (appItem.isBankingApp) {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(miuixColorScheme.primary)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                MiuixText(
                                                    text = stringResource(R.string.kitsune_bank_badge),
                                                    style = miuixTextStyles.body2,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                    }
                                    MiuixText(
                                        text = appItem.packageName,
                                        style = miuixTextStyles.body2,
                                        color = miuixColorScheme.onSurfaceVariantSummary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                MiuixSwitch(
                                    checked = appItem.isHidden,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            KitsuneBankHideManager.setAppHidden(
                                                context = context,
                                                packageName = appItem.packageName,
                                                uid = appItem.uid,
                                                enable = checked,
                                            )
                                            apps = apps.map {
                                                if (it.packageName == appItem.packageName) it.copy(isHidden = checked) else it
                                            }
                                            val msg = if (checked) {
                                                context.getString(R.string.kitsune_bank_hide_toast_applied, appItem.label)
                                            } else {
                                                context.getString(R.string.kitsune_bank_hide_toast_reverted, appItem.label)
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
