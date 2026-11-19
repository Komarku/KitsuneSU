package com.sukisu.nova.folkpatch.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukisu.nova.R
import com.sukisu.nova.folkpatch.ApNatives
import com.sukisu.nova.folkpatch.FolkPatchPrefs
import com.sukisu.nova.folkpatch.HomeLayoutStyle
import com.sukisu.nova.ui.navigation3.LocalNavigator
import com.sukisu.nova.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Единая чистая карточка статуса KitsuneSU на главном экране.
 * Заменяет дублирующиеся статусы: показывает либо SukiSU (init_boot),
 * либо FolkPatch (boot), либо аккуратный выбор установки без каши.
 */
@Composable
fun KitsuneHomeDualCard(
    ksuInstalled: Boolean,
    ksuVersionLabel: String,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val fpState by FolkPatchPrefs.state.collectAsStateWithLifecycle()

    var kpReady by remember { mutableStateOf(false) }
    var kpVersion by remember { mutableStateOf("") }
    var showSuperKeyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(fpState.superKey) {
        withContext(Dispatchers.IO) {
            val ready = ApNatives.isReady(fpState.superKey)
            kpReady = ready
            kpVersion = if (ready) {
                ApNatives.formatKernelPatchVersion(ApNatives.kernelPatchVersion(fpState.superKey))
            } else {
                ""
            }
        }
    }

    if (showSuperKeyDialog) {
        SuperKeyAuthDialog(
            initialKey = fpState.superKey,
            rememberKey = fpState.storeSuperKey,
            onDismiss = { showSuperKeyDialog = false },
            onConfirm = { key, persist ->
                FolkPatchPrefs.setSuperKey(key, persist)
                val ready = ApNatives.isReady(key)
                kpReady = ready
                kpVersion = if (ready) ApNatives.formatKernelPatchVersion(ApNatives.kernelPatchVersion(key)) else ""
                Toast.makeText(
                    context,
                    if (ready) {
                        context.getString(R.string.kitsune_kp_unlocked, kpVersion)
                    } else {
                        context.getString(R.string.kitsune_kp_saved_inactive)
                    },
                    Toast.LENGTH_LONG,
                ).show()
                showSuperKeyDialog = false
            },
            onClear = {
                FolkPatchPrefs.clearSuperKey()
                kpReady = false
                kpVersion = ""
                Toast.makeText(context, R.string.kitsune_kp_cleared, Toast.LENGTH_SHORT).show()
                showSuperKeyDialog = false
            },
        )
    }

    val conflictDetected = ksuInstalled && kpReady
    val isActive = ksuInstalled || kpReady

    val cardAlpha = if (fpState.liquidGlassEnabled) fpState.glassTransparency.coerceIn(0.65f, 0.98f) else 1f
    val borderStroke = if (fpState.liquidGlassEnabled && fpState.glassBorder) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    } else null
    val cardShape = when (fpState.homeLayoutStyle) {
        HomeLayoutStyle.CIRCLE -> RoundedCornerShape(30.dp)
        HomeLayoutStyle.V3_MAGISK -> RoundedCornerShape(18.dp)
        else -> RoundedCornerShape(26.dp)
    }

    val statusTitle = when {
        conflictDetected -> stringResource(R.string.kitsune_status_conflict)
        ksuInstalled -> stringResource(R.string.kitsune_status_working_sukisu)
        kpReady -> stringResource(R.string.kitsune_status_working_folkpatch)
        else -> stringResource(R.string.kitsune_status_not_installed)
    }

    val statusSubtitle = when {
        conflictDetected -> stringResource(R.string.kitsune_conflict_warning)
        ksuInstalled -> stringResource(R.string.kitsune_subtitle_sukisu_active, ksuVersionLabel.ifBlank { "GKI" })
        kpReady -> stringResource(R.string.kitsune_subtitle_folkpatch_active, kpVersion.ifBlank { "0.13.9" })
        else -> stringResource(R.string.kitsune_subtitle_choose)
    }

    val accentTint = when {
        conflictDetected -> MaterialTheme.colorScheme.errorContainer
        isActive -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = accentTint.copy(alpha = cardAlpha),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isActive) 0.14f else 0.08f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Верхняя строка: Логотип KitsuneSU + Чёткий статус + Кнопка SuperKey
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.kitsune_logo_hero),
                    contentDescription = "KitsuneSU",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(if (fpState.homeLayoutStyle == HomeLayoutStyle.CIRCLE) CircleShape else RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF3EC)),
                )
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                conflictDetected -> Icons.Rounded.WarningAmber
                                isActive -> Icons.Rounded.CheckCircle
                                else -> Icons.Rounded.ErrorOutline
                            },
                            contentDescription = null,
                            tint = when {
                                conflictDetected -> MaterialTheme.colorScheme.error
                                isActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!fpState.hideStatusBadge && fpState.customBadgeText.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = fpState.customBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                FilledTonalIconButton(
                    onClick = { showSuperKeyDialog = true },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = stringResource(R.string.kitsune_dlg_superkey_title),
                    )
                }
            }

            // Нижняя строка: Две чистые кнопки установки (init_boot SukiSU | boot FolkPatch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { navigator.push(Route.Install()) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Memory, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.kitsune_btn_install_init_boot),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                FilledTonalButton(
                    onClick = { navigator.push(Route.FolkPatchBootInstall(0)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.kitsune_btn_install_boot),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
