package com.sukisu.nova.folkpatch.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukisu.nova.R
import com.sukisu.nova.folkpatch.FolkPatchPrefs
import com.sukisu.nova.folkpatch.StatusCardDecoration
import com.sukisu.nova.folkpatch.StatusCardIcon
import com.sukisu.nova.folkpatch.StatusCardShape

/**
 * Диалог полной кастомизации плашки статуса KitsuneSU (где написано «Работает»).
 * Позволяет настраивать:
 * - Форму плашки (Hero, Squircle, Cut-Corner, Pill, Asymmetric)
 * - Иконку с галочкой/щитом/звездой/лисой
 * - Декоративные фигуры Material 3 (Pill & Gems, Sparkles, Rings, Polygons)
 * - Кастомный текст бейджа (ВКЛЮЧЕНО / ROOT АКТИВЕН и т.д.)
 * - Неоновое свечение и контур
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCardCustomizerDialog(
    onDismiss: () -> Unit,
) {
    val prefsState by FolkPatchPrefs.state.collectAsState()

    var selectedShape by remember { mutableStateOf(prefsState.statusCardShape) }
    var selectedIcon by remember { mutableStateOf(prefsState.statusCardIcon) }
    var selectedDeco by remember { mutableStateOf(prefsState.statusCardDecoration) }
    var showBadge by remember { mutableStateOf(prefsState.statusCardShowBadge) }
    var badgeText by remember { mutableStateOf(prefsState.statusCardBadgeText) }
    var glowEnabled by remember { mutableStateOf(prefsState.statusCardGlow) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.kitsune_customize_status_card_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Live Interactive Preview
                Text(
                    text = stringResource(R.string.kitsune_preview),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                StatusCardPreview(
                    shape = selectedShape,
                    icon = selectedIcon,
                    deco = selectedDeco,
                    showBadge = showBadge,
                    badgeText = badgeText,
                    glow = glowEnabled,
                )

                HorizontalDivider()

                // 1. Форма плашки
                Text(
                    text = stringResource(R.string.kitsune_status_card_shape_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusCardShape.entries.forEach { shape ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedShape = shape }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = selectedShape == shape,
                                onClick = { selectedShape = shape },
                            )
                            Text(
                                text = when (shape) {
                                    StatusCardShape.HERO -> stringResource(R.string.kitsune_shape_hero)
                                    StatusCardShape.SQUIRCLE -> stringResource(R.string.kitsune_shape_squircle)
                                    StatusCardShape.CUT_CORNER -> stringResource(R.string.kitsune_shape_cut_corner)
                                    StatusCardShape.PILL -> stringResource(R.string.kitsune_shape_pill)
                                    StatusCardShape.ASYMMETRIC -> stringResource(R.string.kitsune_shape_asymmetric)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 2. Иконка статуса / Галочка
                Text(
                    text = stringResource(R.string.kitsune_status_card_icon_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusCardIcon.entries.forEach { icon ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedIcon = icon }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = selectedIcon == icon,
                                onClick = { selectedIcon = icon },
                            )
                            val iconVec = getStatusCardVector(icon)
                            if (iconVec != null) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = when (icon) {
                                    StatusCardIcon.CHECK_CIRCLE -> stringResource(R.string.kitsune_icon_check_circle)
                                    StatusCardIcon.VERIFIED -> stringResource(R.string.kitsune_icon_verified)
                                    StatusCardIcon.SHIELD -> stringResource(R.string.kitsune_icon_shield)
                                    StatusCardIcon.KITSUNE -> stringResource(R.string.kitsune_icon_kitsune)
                                    StatusCardIcon.STAR -> stringResource(R.string.kitsune_icon_star)
                                    StatusCardIcon.NONE -> stringResource(R.string.kitsune_icon_none)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 3. Декорации Material 3
                Text(
                    text = stringResource(R.string.kitsune_status_card_deco_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusCardDecoration.entries.forEach { deco ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedDeco = deco }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = selectedDeco == deco,
                                onClick = { selectedDeco = deco },
                            )
                            Text(
                                text = when (deco) {
                                    StatusCardDecoration.M3_SHAPES -> stringResource(R.string.kitsune_deco_m3_shapes)
                                    StatusCardDecoration.SPARKLES -> stringResource(R.string.kitsune_deco_sparkles)
                                    StatusCardDecoration.RINGS -> stringResource(R.string.kitsune_deco_rings)
                                    StatusCardDecoration.FLOATING_POLYGONS -> stringResource(R.string.kitsune_deco_polygons)
                                    StatusCardDecoration.MINIMAL -> stringResource(R.string.kitsune_deco_minimal)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 4. Опции бейджа и свечения
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kitsune_show_badge_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.kitsune_show_badge_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = showBadge,
                        onCheckedChange = { showBadge = it },
                    )
                }

                if (showBadge) {
                    OutlinedTextField(
                        value = badgeText,
                        onValueChange = { badgeText = it },
                        label = { Text(stringResource(R.string.kitsune_badge_text_label)) },
                        placeholder = { Text("ВКЛЮЧЕНО") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kitsune_glow_title),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.kitsune_glow_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = glowEnabled,
                        onCheckedChange = { glowEnabled = it },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    FolkPatchPrefs.update {
                        it.copy(
                            statusCardShape = selectedShape,
                            statusCardIcon = selectedIcon,
                            statusCardDecoration = selectedDeco,
                            statusCardShowBadge = showBadge,
                            statusCardBadgeText = badgeText.trim().ifBlank { "ВКЛЮЧЕНО" },
                            statusCardGlow = glowEnabled,
                        )
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

fun getStatusCardShape(shape: StatusCardShape): Shape = when (shape) {
    StatusCardShape.HERO -> RoundedCornerShape(32.dp)
    StatusCardShape.SQUIRCLE -> RoundedCornerShape(22.dp)
    StatusCardShape.CUT_CORNER -> CutCornerShape(24.dp, 8.dp, 24.dp, 8.dp)
    StatusCardShape.PILL -> RoundedCornerShape(50)
    StatusCardShape.ASYMMETRIC -> RoundedCornerShape(
        topStart = 36.dp, topEnd = 12.dp, bottomEnd = 36.dp, bottomStart = 12.dp
    )
}

fun getStatusCardVector(icon: StatusCardIcon): ImageVector? = when (icon) {
    StatusCardIcon.CHECK_CIRCLE -> Icons.Rounded.CheckCircle
    StatusCardIcon.VERIFIED -> Icons.Rounded.Verified
    StatusCardIcon.SHIELD -> Icons.Rounded.Security
    StatusCardIcon.KITSUNE -> Icons.Rounded.Pets
    StatusCardIcon.STAR -> Icons.Rounded.AutoAwesome
    StatusCardIcon.NONE -> null
}

@Composable
fun StatusCardPreview(
    shape: StatusCardShape,
    icon: StatusCardIcon,
    deco: StatusCardDecoration,
    showBadge: Boolean,
    badgeText: String,
    glow: Boolean,
) {
    val containerShape = getStatusCardShape(shape)
    val primaryColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Surface(
        shape = containerShape,
        color = primaryColor,
        contentColor = contentColor,
        border = if (glow) BorderStroke(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(
                    contentColor.copy(alpha = 0.6f),
                    contentColor.copy(alpha = 0.2f),
                    contentColor.copy(alpha = 0.5f),
                )
            )
        ) else null,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Tags
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = contentColor,
                    ) {
                        Text(
                            text = "Kitsune (init_boot)",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                    }

                    if (showBadge) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = contentColor.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.45f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val ic = getStatusCardVector(icon)
                                if (ic != null) {
                                    Icon(
                                        imageVector = ic,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = contentColor,
                                    )
                                }
                                Text(
                                    text = badgeText.ifBlank { "ВКЛЮЧЕНО" },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = contentColor,
                                )
                            }
                        }
                    }
                }

                // Main Text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Работает",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    val mainIc = getStatusCardVector(icon)
                    if (mainIc != null) {
                        Icon(
                            imageVector = mainIc,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = contentColor,
                        )
                    }
                }

                Text(
                    text = "v3.1.0 (Fox Edition)",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = contentColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}
