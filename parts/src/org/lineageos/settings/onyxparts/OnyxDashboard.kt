/*
 * Copyright (C) 2026 The LineageOS Project
 *           (C) 2026 zylhdrXP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.onyxparts

import android.content.Context
import android.content.Intent
import android.os.SystemProperties
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.lineageos.settings.R
import org.lineageos.settings.corecontrol.CoreControlActivity
import org.lineageos.settings.gpumanager.GpuManagerActivity
import org.lineageos.settings.kernelmanager.KernelManagerActivity
import org.lineageos.settings.refreshrate.RefreshSettingsActivity
import org.lineageos.settings.saturation.SaturationActivity
import org.lineageos.settings.speaker.ClearSpeakerActivity
import org.lineageos.settings.thermal.ThermalComposeActivity
import org.lineageos.settings.ui.theme.ExpressiveMotion
import org.lineageos.settings.ui.theme.ExpressiveShapes
import org.lineageos.settings.ui.theme.ExpressiveTheme
import org.lineageos.settings.ui.theme.ExpressiveTypography
import org.lineageos.settings.ui.theme.expressiveBounceClick

data class OnyxFeature(
    val title: String,
    val summary: String,
    val iconRes: Int,
    val activityClass: Class<*>
)

private fun getSupportedDeviceName(): String? =
    SystemProperties.get("bluetooth.device.default_name").takeIf { it.isNotBlank() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnyxDashboard(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val coreControl = OnyxFeature(
        stringResource(R.string.core_control_title),
        stringResource(R.string.onyx_core_control_summary),
        R.drawable.ic_cpu,
        CoreControlActivity::class.java
    )
    val kernelManager = OnyxFeature(
        stringResource(R.string.kernel_manager_title),
        stringResource(R.string.onyx_kernel_manager_summary),
        R.drawable.ic_kernel_manager,
        KernelManagerActivity::class.java
    )
    val gpuManager = OnyxFeature(
        stringResource(R.string.gpu_manager_title),
        stringResource(R.string.onyx_gpu_manager_summary),
        R.drawable.ic_gpu_manager,
        GpuManagerActivity::class.java
    )
    val thermalEngine = OnyxFeature(
        stringResource(R.string.thermal_title),
        stringResource(R.string.onyx_thermal_engine_summary),
        R.drawable.ic_thermal_settings,
        ThermalComposeActivity::class.java
    )
    val carouselFeatures = listOf(
        OnyxFeature(
            stringResource(R.string.onyx_display_labs_title),
            stringResource(R.string.onyx_display_labs_summary),
            R.drawable.ic_saturation_tile,
            SaturationActivity::class.java
        ),
        OnyxFeature(
            stringResource(R.string.clear_speaker_title),
            stringResource(R.string.onyx_clear_speaker_summary),
            R.drawable.ic_clear_speaker,
            ClearSpeakerActivity::class.java
        ),
        OnyxFeature(
            stringResource(R.string.refresh_title),
            stringResource(R.string.onyx_smooth_display_summary),
            R.drawable.ic_refresh_default,
            RefreshSettingsActivity::class.java
        )
    )

    val carouselState = rememberCarouselState { carouselFeatures.size }

    LaunchedEffect(carouselState) {
        snapshotFlow { carouselState.currentItem }.collect {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    val scrollState = rememberScrollState()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    ExpressiveTheme {
        Scaffold(
            topBar = {
                val collapseThreshold = 120f
                val collapseTarget = (scrollState.value / collapseThreshold).coerceIn(0f, 1f)
                val collapseProgress by animateFloatAsState(
                    targetValue = collapseTarget,
                    animationSpec = ExpressiveMotion.spatialDefault(),
                    label = "header_collapse_progress"
                )
                CollapsingHeader(
                    collapseProgress = collapseProgress,
                    onBackPressed = onBackPressed
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                StaggeredAnimatedItem(index = 0, isVisible = isVisible) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        HeroBanner(scrollValue = scrollState.value)
                    }
                }

                StaggeredAnimatedItem(index = 1, isVisible = isVisible, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GroupedFeatureCard(listOf(gpuManager), context)
                            GroupedFeatureCard(listOf(thermalEngine), context)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            GroupedFeatureCard(
                                features = listOf(coreControl, kernelManager),
                                context = context,
                                modifier = Modifier.fillMaxHeight(),
                                stretchHeight = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                StaggeredAnimatedItem(index = 2, isVisible = isVisible) {
                    Text(
                        text = stringResource(R.string.onyx_system_utilities),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        style = ExpressiveTypography.LabelLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                StaggeredAnimatedItem(index = 3, isVisible = isVisible) {
                    HorizontalMultiBrowseCarousel(
                        state = carouselState,
                        preferredItemWidth = 190.dp,
                        itemSpacing = 16.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) { index ->
                        val feature = carouselFeatures[index]
                        CarouselFeatureItem(
                            feature = feature,
                            context = context,
                            modifier = Modifier.maskClip(ExpressiveShapes.ShapeExtraLarge)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CollapsingHeader(
    collapseProgress: Float,
    onBackPressed: () -> Unit
) {
    val navigateBackDescription = stringResource(R.string.onyx_navigate_back)
    val progress = collapseProgress.coerceIn(0f, 1f)

    val titleScale = 1f - (0.36f * progress)
    val subtitleAlpha = (1f - progress).coerceIn(0f, 1f)

    val headerHeight = lerp(160.dp, 104.dp, progress)
    val headerBottomCorner = lerp(0.dp, ExpressiveShapes.CornerExtraLarge, progress).coerceAtLeast(0.dp)
    val backButtonCorner = lerp(12.dp, 24.dp, progress).coerceAtLeast(0.dp)

    val backButtonBgColor = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.primary,
        progress
    )

    val backButtonIconColor = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.onPrimary,
        progress
    )

    val titleXOffset = lerp(0.dp, 56.dp, progress)
    val titleYOffset = lerp(60.dp, 16.dp, progress)

    val backButtonShape: Shape = if (progress >= 0.98f) {
        CircleShape
    } else {
        androidx.compose.foundation.shape.RoundedCornerShape(backButtonCorner)
    }

    val headerContainerColor = androidx.compose.ui.graphics.lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceContainer,
        progress
    )

    Surface(
        color = headerContainerColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = headerBottomCorner,
            bottomEnd = headerBottomCorner
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .background(
                        color = backButtonBgColor,
                        shape = backButtonShape
                    )
                    .semantics {
                        contentDescription = navigateBackDescription
                        role = Role.Button
                    }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = backButtonIconColor
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = titleXOffset, y = titleYOffset)
                    .graphicsLayer {
                        scaleX = titleScale
                        scaleY = titleScale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                Text(
                    text = stringResource(R.string.onyx_dashboard_title),
                    style = ExpressiveTypography.HeadlineMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.onyx_dashboard_subtitle),
                    style = ExpressiveTypography.LabelMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer {
                        alpha = subtitleAlpha
                        translationY = -12f * progress
                    }
                )
            }
        }
    }
}

@Composable
fun HeroBanner(scrollValue: Int = 0) {
    val supportedDeviceName = remember { getSupportedDeviceName() }
    val infiniteTransition = rememberInfiniteTransition(label = "hero_banner")

    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    val iconOffset = (animationProgress - 0.5f) * 20f
    val parallaxOffset = scrollValue * 0.15f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .expressiveBounceClick(),
        shape = ExpressiveShapes.ShapeExtraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.72f)
                    .graphicsLayer {
                        translationY = parallaxOffset * 0.5f
                    }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = ExpressiveShapes.ShapeSmall
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.onyx_hero_prompt),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.onyx_hero_terminal),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = supportedDeviceName ?: stringResource(R.string.onyx_supported_devices),
                    style = ExpressiveTypography.TitleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.onyx_system_performance_optimized),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.ic_onyx),
                contentDescription = stringResource(R.string.onyx_engine_content_description),
                modifier = Modifier
                    .size(136.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp, y = iconOffset.dp)
                    .graphicsLayer {
                        translationY = parallaxOffset
                    }
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun GroupedFeatureCard(
    features: List<OnyxFeature>,
    context: Context,
    modifier: Modifier = Modifier,
    stretchHeight: Boolean = false
) {
    val isGrouped = features.size > 1
    val cardBgColor = if (isGrouped) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .expressiveBounceClick(),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = ExpressiveShapes.ShapeExtraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        val columnModifier = if (stretchHeight) Modifier.fillMaxSize() else Modifier
        Column(modifier = columnModifier) {
            features.forEachIndexed { index, feature ->
                // Optical roundness: inner = outer - padding (28dp - 16dp = 12dp)
                val outerCorner = ExpressiveShapes.CornerExtraLarge // 28dp
                val innerCorner = ExpressiveShapes.CornerSmall // 8dp
                val itemShape: Shape = when {
                    !isGrouped -> ExpressiveShapes.ShapeExtraLarge
                    index == 0 -> androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = outerCorner,
                        topEnd = outerCorner,
                        bottomStart = innerCorner,
                        bottomEnd = innerCorner
                    )
                    index == features.size - 1 -> androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = innerCorner,
                        topEnd = innerCorner,
                        bottomStart = outerCorner,
                        bottomEnd = outerCorner
                    )
                    else -> ExpressiveShapes.ShapeExtraSmall
                }

                if (stretchHeight) {
                    FeatureItemContent(
                        feature = feature,
                        context = context,
                        isGrouped = isGrouped,
                        shape = itemShape,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    FeatureItemContent(
                        feature = feature,
                        context = context,
                        isGrouped = isGrouped,
                        shape = itemShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (index < features.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarouselFeatureItem(
    feature: OnyxFeature,
    context: Context,
    modifier: Modifier = Modifier
) {
    val isGrouped = false
    val openFeatureDescription = stringResource(R.string.onyx_open_feature, feature.title)

    Card(
        onClick = { context.startActivity(Intent(context, feature.activityClass)) },
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = openFeatureDescription
                role = Role.Button
            },
        shape = ExpressiveShapes.ShapeExtraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FeatureIcon(feature.iconRes, isGrouped)

            Column {
                Text(
                    text = feature.title,
                    style = ExpressiveTypography.TitleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = feature.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ArrowBubble(isGrouped)
            }
        }
    }
}

@Composable
fun FeatureItemContent(
    feature: OnyxFeature,
    context: Context,
    isGrouped: Boolean = false,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(shape)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                onClickLabel = stringResource(R.string.onyx_open_feature, feature.title)
            ) {
                context.startActivity(Intent(context, feature.activityClass))
            }
            .semantics { role = Role.Button }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.Center
    ) {
        FeatureIcon(feature.iconRes, isGrouped)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = feature.title,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = feature.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            ArrowBubble(isGrouped)
        }
    }
}

@Composable
private fun FeatureIcon(iconRes: Int, isGrouped: Boolean = false) {
    val targetBgColor = if (isGrouped) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primary
    val iconTint = if (isGrouped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

    val cornerRadius by animateDpAsState(
        targetValue = if (isGrouped) ExpressiveShapes.CornerExtraLarge else ExpressiveShapes.CornerLarge,
        animationSpec = ExpressiveMotion.spatialDefault(),
        label = "shape_morph"
    )

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "bg_color_morph"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .background(
                color = animatedBgColor,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.coerceAtLeast(0.dp))
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = iconTint
        )
    }
}

@Composable
private fun ArrowBubble(isGrouped: Boolean = false) {
    val bgColor = if (isGrouped) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primary
    val iconTint = if (isGrouped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = bgColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconTint
        )
    }
}

/**
 * Staggered enter animation using fast spatial translation and default effects fade.
 */
@Composable
fun StaggeredAnimatedItem(
    index: Int,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var itemVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(index * 60L)
            itemVisible = true
        } else {
            itemVisible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (itemVisible) 1f else 0f,
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "alpha_$index"
    )

    val translateY by animateDpAsState(
        targetValue = if (itemVisible) 0.dp else 28.dp,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "translateY_$index"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translateY.toPx()
        }
    ) {
        content()
    }
}