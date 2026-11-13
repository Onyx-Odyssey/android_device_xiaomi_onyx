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

package org.lineageos.settings.gpumanager.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.lineageos.settings.R
import org.lineageos.settings.gpumanager.GpuManagerViewModel
import org.lineageos.settings.ui.theme.ExpressiveMotion
import org.lineageos.settings.ui.theme.ExpressiveShapes
import org.lineageos.settings.ui.theme.ExpressiveTypography
import org.lineageos.settings.ui.theme.expressiveBounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpuManagerScreen(
    viewModel: GpuManagerViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
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
                        stringResource(R.string.gpu_manager_title),
                        style = ExpressiveTypography.HeadlineMediumEmphasized
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.onyx_navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.resetSettings()
                        Toast.makeText(context, R.string.settings_reset, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
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
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = {
                        Text(
                            stringResource(R.string.apply_settings),
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedEntrance(visible = visible, index = 0) {
                    GpuInfoCard(
                        model = uiState.gpuModel,
                        load = uiState.busyPercentage,
                        temp = uiState.temperature,
                        freq = uiState.currentFreq
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 1) {
                    CategoryHeader(stringResource(R.string.gpu_settings_category))
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 2) {
                    GpuDropdownPreference(
                        title = stringResource(R.string.gpu_governor_title),
                        selectedOption = uiState.currentGovernor,
                        options = uiState.availableGovernors,
                        onOptionSelected = { viewModel.setGovernor(it) },
                        icon = Icons.Default.Settings,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 3) {
                    GpuFrequenciesCard(
                        minFreq = uiState.currentMinFreq,
                        maxFreq = uiState.currentMaxFreq,
                        options = uiState.availableFrequencies,
                        onMinSelected = { viewModel.setMinFrequency(it) },
                        onMaxSelected = { viewModel.setMaxFrequency(it) }
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 4) {
                    CategoryHeader(stringResource(R.string.gpu_power_title))
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 5) {
                    GpuSwitchPreference(
                        title = stringResource(R.string.gpu_force_clk_on_title),
                        summary = stringResource(R.string.gpu_force_clk_on_summary),
                        checked = uiState.forceClkOn,
                        onCheckedChange = { viewModel.setForceClkOn(it) }
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 6) {
                    GpuSwitchPreference(
                        title = stringResource(R.string.gpu_force_bus_on_title),
                        summary = stringResource(R.string.gpu_force_bus_on_summary),
                        checked = uiState.forceBusOn,
                        onCheckedChange = { viewModel.setForceBusOn(it) }
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 7) {
                    GpuSwitchPreference(
                        title = stringResource(R.string.gpu_force_rail_on_title),
                        summary = stringResource(R.string.gpu_force_rail_on_summary),
                        checked = uiState.forceRailOn,
                        onCheckedChange = { viewModel.setForceRailOn(it) }
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 8) {
                    GpuSwitchPreference(
                        title = stringResource(R.string.gpu_force_no_nap_title),
                        summary = stringResource(R.string.gpu_force_no_nap_summary),
                        checked = uiState.forceNoNap,
                        onCheckedChange = { viewModel.setForceNoNap(it) }
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 9) {
                    GpuSwitchPreference(
                        title = stringResource(R.string.gpu_bus_split_title),
                        summary = stringResource(R.string.gpu_bus_split_summary),
                        checked = uiState.busSplit,
                        onCheckedChange = { viewModel.setBusSplit(it) }
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 10) {
                    CategoryHeader(stringResource(R.string.gpu_thermal_title))
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 11) {
                    InfoItem(
                        title = stringResource(R.string.gpu_thermal_pwrlevel_title),
                        value = "Level ${uiState.thermalPowerLevel}",
                        summary = stringResource(R.string.gpu_thermal_pwrlevel_summary)
                    )
                }
            }

            item {
                AnimatedEntrance(visible = visible, index = 12) {
                    GpuSwitchPreference(
                        title = stringResource(R.string.apply_on_boot_title),
                        summary = stringResource(R.string.apply_on_boot_summary),
                        checked = uiState.applyOnBoot,
                        onCheckedChange = { viewModel.setApplyOnBoot(it) }
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
            delay(index * 40L)
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

@Composable
fun GpuInfoCard(
    model: String,
    load: String,
    temp: String,
    freq: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .expressiveBounceClick(),
        shape = ExpressiveShapes.ShapeExtraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = ExpressiveShapes.ShapeMedium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = model,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "GPU Accelerator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(stringResource(R.string.gpu_busy_percentage_title), load)
                InfoColumn(stringResource(R.string.gpu_temp_title), "$temp°C")
                InfoColumn(
                    stringResource(R.string.gpu_current_freq_title),
                    if (freq != "0" && freq.isNotEmpty()) "${try { freq.toLong() / 1000000 } catch(e:Exception) { freq }} MHz" else "Offline"
                )
            }
        }
    }
}

@Composable
fun InfoColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (fadeIn(ExpressiveMotion.effectsFast()) + scaleIn(ExpressiveMotion.spatialFast(), initialScale = 0.94f))
                    .togetherWith(fadeOut(ExpressiveMotion.effectsFast()))
            },
            label = "value_anim"
        ) { targetValue ->
            Text(
                text = targetValue,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpuDropdownPreference(
    title: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    var expanded by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        targetValue = if (expanded) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "dropdownColor"
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
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = ExpressiveShapes.ShapeMedium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AnimatedContent(targetState = selectedOption, label = "optionAnim") { opt ->
                        Text(
                            text = "Active: $opt",
                            style = MaterialTheme.typography.bodySmall,
                            color = color
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
                    value = selectedOption,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Option") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedBorderColor = color,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = ExpressiveShapes.ShapeLarge
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    style = if (option == selectedOption) ExpressiveTypography.LabelLargeEmphasized else MaterialTheme.typography.bodyMedium,
                                    color = if (option == selectedOption) color else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onOptionSelected(option)
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
fun GpuFrequenciesCard(
    minFreq: String,
    maxFreq: String,
    options: List<String>,
    onMinSelected: (String) -> Unit,
    onMaxSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(ExpressiveMotion.spatialDefault()),
        shape = ExpressiveShapes.ShapeExtraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                Text(
                    text = stringResource(R.string.gpu_freq_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GpuFreqSelector(
                    modifier = Modifier.weight(1f),
                    label = "Min",
                    currentFreq = minFreq,
                    options = options,
                    onSelected = onMinSelected,
                    color = MaterialTheme.colorScheme.tertiary
                )
                GpuFreqSelector(
                    modifier = Modifier.weight(1f),
                    label = "Max",
                    currentFreq = maxFreq,
                    options = options,
                    onSelected = onMaxSelected,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpuFreqSelector(
    modifier: Modifier = Modifier,
    label: String,
    currentFreq: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    color: Color
) {
    var expanded by remember { mutableStateOf(false) }

    val displayFreq = try {
        "${currentFreq.toLong() / 1000000} MHz"
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
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            singleLine = true,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor = color,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            shape = ExpressiveShapes.ShapeLarge
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            options.forEach { freq ->
                val freqDisplay = try {
                    "${freq.toLong() / 1000000} MHz"
                } catch (e: Exception) {
                    freq
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = freqDisplay,
                            style = if (freq == currentFreq) ExpressiveTypography.LabelLargeEmphasized else MaterialTheme.typography.bodyMedium,
                            color = if (freq == currentFreq) color else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelected(freq)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun GpuSwitchPreference(
    title: String,
    summary: String,
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
            .semantics { role = Role.Switch }
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
                    text = summary,
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
fun InfoItem(title: String, value: String, summary: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .expressiveBounceClick(),
        shape = ExpressiveShapes.ShapeExtraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = ExpressiveShapes.ShapeMedium
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
