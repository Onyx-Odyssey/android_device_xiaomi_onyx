/*
 * Copyright (C) 2025 KamiKaonashi
 *           (C) 2026 zylhdrXP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.lineageos.settings.kernelmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.lineageos.settings.R
import org.lineageos.settings.ui.theme.ExpressiveMotion
import org.lineageos.settings.ui.theme.ExpressiveShapes
import org.lineageos.settings.ui.theme.ExpressiveTheme
import org.lineageos.settings.ui.theme.ExpressiveTypography
import org.lineageos.settings.ui.theme.expressiveBounceClick

class KernelManagerActivity : ComponentActivity() {
    private val viewModel: KernelManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExpressiveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    KernelManagerScreen(
                        viewModel = viewModel,
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KernelManagerScreen(
    viewModel: KernelManagerViewModel,
    onBackPressed: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.kernel_manager_title),
                        style = ExpressiveTypography.HeadlineMediumEmphasized
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.onyx_navigate_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(ExpressiveMotion.FastSpatial) + fadeIn(ExpressiveMotion.effectsFast()),
                exit = scaleOut(ExpressiveMotion.FastSpatial) + fadeOut(ExpressiveMotion.effectsFast())
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.applySettings()
                        Toast.makeText(context, R.string.settings_applied, Toast.LENGTH_SHORT).show()
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = "Apply") },
                    text = {
                        Text(
                            "Apply Settings",
                            style = ExpressiveTypography.LabelLargeEmphasized
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = ExpressiveShapes.ShapeLarge,
                    modifier = Modifier.expressiveBounceClick(),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 6.dp
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
        ) {
            item {
                AnimatedEntrance(visible = visible, index = 0) {
                    GovernorSelectionCard(
                        governors = state.availableGovernors,
                        currentGovernor = state.currentGovernor,
                        onGovernorSelected = viewModel::updateGovernor
                    )
                }
            }

            itemsIndexed(state.clusters) { index, cluster ->
                AnimatedEntrance(visible = visible, index = index + 1) {
                    FrequencyClusterCard(
                        title = cluster.label,
                        availableFreqs = cluster.availableFreqs,
                        minFreq = cluster.minFreq,
                        maxFreq = cluster.maxFreq,
                        iconContainerColor = if (index == 0) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        iconContentColor = if (index == 0) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        onMinFreqSelected = { viewModel.updateClusterMinFreq(cluster.policy, it) },
                        onMaxFreqSelected = { viewModel.updateClusterMaxFreq(cluster.policy, it) }
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 3) {
                    ActionCard(
                        title = "Reset to Defaults",
                        subtitle = "Restore original kernel configurations",
                        icon = Icons.Default.Refresh,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = {
                            viewModel.resetSettings()
                            Toast.makeText(context, R.string.settings_reset, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 4) {
                    SwitchCard(
                        title = stringResource(R.string.apply_on_boot_title),
                        subtitle = stringResource(R.string.apply_on_boot_summary),
                        checked = state.applyOnBoot,
                        onCheckedChange = viewModel::setApplyOnBoot
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedEntrance(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    var itemVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(index * 50L)
            itemVisible = true
        } else {
            itemVisible = false
        }
    }

    AnimatedVisibility(
        visible = itemVisible,
        enter = slideInVertically(
            initialOffsetY = { 32 },
            animationSpec = ExpressiveMotion.spatialFast()
        ) + fadeIn(animationSpec = ExpressiveMotion.effectsFast())
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GovernorSelectionCard(
    governors: List<String>,
    currentGovernor: String,
    onGovernorSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "govColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .expressiveBounceClick()
            .animateContentSize(ExpressiveMotion.spatialDefault()),
        shape = ExpressiveShapes.ShapeExtraLarge,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = ExpressiveShapes.ShapeMedium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "CPU Governor",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AnimatedContent(targetState = currentGovernor, label = "govAnim") { gov ->
                        Text(
                            text = "Active: $gov",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = currentGovernor,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Governor") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = ExpressiveShapes.ShapeLarge
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    governors.forEach { gov ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = gov,
                                    style = if (gov == currentGovernor) ExpressiveTypography.LabelLargeEmphasized else MaterialTheme.typography.bodyMedium,
                                    color = if (gov == currentGovernor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onGovernorSelected(gov)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyClusterCard(
    title: String,
    availableFreqs: List<String>,
    minFreq: String,
    maxFreq: String,
    iconContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    onMinFreqSelected: (String) -> Unit,
    onMaxFreqSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(ExpressiveMotion.spatialDefault()),
        shape = ExpressiveShapes.ShapeExtraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = iconContainerColor,
                            shape = ExpressiveShapes.ShapeMedium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = iconContentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FrequencySelector(
                    modifier = Modifier.weight(1f),
                    label = "Min",
                    currentFreq = minFreq,
                    availableFreqs = availableFreqs,
                    onFreqSelected = onMinFreqSelected
                )

                FrequencySelector(
                    modifier = Modifier.weight(1f),
                    label = "Max",
                    currentFreq = maxFreq,
                    availableFreqs = availableFreqs,
                    onFreqSelected = onMaxFreqSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySelector(
    modifier: Modifier = Modifier,
    label: String,
    currentFreq: String,
    availableFreqs: List<String>,
    onFreqSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val displayFreq = try {
        "${currentFreq.toInt() / 1000} MHz"
    } catch (e: Exception) {
        if (currentFreq.isNotEmpty()) currentFreq else "Unknown"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.expressiveBounceClick()
    ) {
        OutlinedTextField(
            value = displayFreq,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, maxLines = 1) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            shape = ExpressiveShapes.ShapeLarge
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            availableFreqs.forEach { freq ->
                val freqDisplay = try {
                    "${freq.toInt() / 1000} MHz"
                } catch (e: Exception) {
                    freq
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = freqDisplay,
                            style = if (freq == currentFreq) ExpressiveTypography.LabelLargeEmphasized else MaterialTheme.typography.bodyMedium,
                            color = if (freq == currentFreq) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onFreqSelected(freq)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun SwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "switchBg"
    )

    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = ExpressiveShapes.ShapeExtraLarge,
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .expressiveBounceClick()
            .animateContentSize(ExpressiveMotion.spatialDefault())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ExpressiveShapes.ShapeExtraLarge,
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .expressiveBounceClick()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}