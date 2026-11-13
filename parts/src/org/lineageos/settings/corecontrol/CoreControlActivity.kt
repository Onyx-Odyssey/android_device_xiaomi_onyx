/*
 * Copyright (C) 2025 The LineageOS Project
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

package org.lineageos.settings.corecontrol

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.lineageos.settings.R
import org.lineageos.settings.ui.theme.ExpressiveMotion
import org.lineageos.settings.ui.theme.ExpressiveShapes
import org.lineageos.settings.ui.theme.ExpressiveTheme
import org.lineageos.settings.ui.theme.ExpressiveTypography
import org.lineageos.settings.ui.theme.expressiveBounceClick
import org.lineageos.settings.utils.FileUtils

class CoreControlActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExpressiveTheme {
                BackHandler { finish() }
                CoreControlScreen(onBackPressed = { finish() })
            }
        }
    }
}

private const val TAG = "CoreControl"
private const val NUM_CORES = 8

private fun isCoreOnline(core: Int): Boolean {
    val path = "/sys/devices/system/cpu/cpu$core/online"
    if (!FileUtils.fileExists(path)) {
        // CPU0 is usually always online and may not expose an 'online' sysfs node
        return true
    }
    return runCatching {
        FileUtils.readOneLine(path)?.trim() == "1"
    }.getOrDefault(true)
}

private fun setCoreOnline(core: Int, online: Boolean): Boolean {
    val path = "/sys/devices/system/cpu/cpu$core/online"
    if (!FileUtils.fileExists(path)) return false
    return runCatching {
        FileUtils.writeLine(path, if (online) "1" else "0")
    }.getOrDefault(false)
}

// Onyx (SM8735) topology: cpu0-1 little, cpu2-6 big, cpu7 prime.
private const val LITTLE_CORES = 2
private const val PRIME_CORE = 7

private fun canOfflineCore(core: Int, coreStates: Map<Int, Boolean>): Boolean {
    // The little cluster must keep at least one core online, otherwise the
    // device becomes unusable. Onyx only has two little cores, so requiring
    // two online would make them non-toggleable.
    if (core in 0 until LITTLE_CORES) {
        var onlineCount = 0
        for (i in 0 until LITTLE_CORES) {
            if (i != core && (coreStates[i] ?: isCoreOnline(i))) {
                onlineCount++
            }
        }
        return onlineCount >= 1
    }
    return true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreControlScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val coreStates = remember {
        mutableStateMapOf<Int, Boolean>().apply {
            for (i in 0 until NUM_CORES) {
                put(i, isCoreOnline(i))
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                for (i in 0 until NUM_CORES) {
                    coreStates[i] = isCoreOnline(i)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val onlineCount = coreStates.values.count { it }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.core_control_title),
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
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .expressiveBounceClick(),
                    shape = ExpressiveShapes.ShapeExtraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CPU Status",
                                style = ExpressiveTypography.TitleMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$onlineCount of $NUM_CORES Cores Active",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = ExpressiveShapes.ShapeFull,
                            color = if (onlineCount >= 6) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (onlineCount >= 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (onlineCount == NUM_CORES) "Optimal" else "Custom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (onlineCount >= 6) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Efficiency Cluster (Little, CPU 0-1)",
                    style = ExpressiveTypography.LabelLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.ShapeExtraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        for (i in 0 until LITTLE_CORES) {
                            val isOnline = coreStates[i] ?: true
                            val isToggleable = FileUtils.fileExists("/sys/devices/system/cpu/cpu$i/online")
                            CoreRowItem(
                                coreIndex = i,
                                title = "Core $i",
                                subtitle = if (!isToggleable) "Always Active" else if (isOnline) "Active" else "Offline",
                                isOnline = isOnline,
                                enabled = isToggleable,
                                onCheckedChange = { desired ->
                                    if (!desired && !canOfflineCore(i, coreStates)) {
                                        Toast.makeText(
                                            context,
                                            "At least 1 little core must remain online",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val success = setCoreOnline(i, desired)
                                        if (success) {
                                            coreStates[i] = desired
                                        } else {
                                            coreStates[i] = isCoreOnline(i)
                                            Toast.makeText(
                                                context,
                                                "Failed to change Core $i state",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )

                            if (i < LITTLE_CORES - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Performance Cluster (Big + Prime, CPU 2-7)",
                    style = ExpressiveTypography.LabelLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.ShapeExtraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        for (i in LITTLE_CORES..PRIME_CORE) {
                            val isOnline = coreStates[i] ?: true
                            val isToggleable = FileUtils.fileExists("/sys/devices/system/cpu/cpu$i/online")
                            CoreRowItem(
                                coreIndex = i,
                                title = if (i == PRIME_CORE) "Core $i (Prime)" else "Core $i (Big)",
                                subtitle = if (!isToggleable) "Always Active" else if (isOnline) "Active" else "Offline",
                                isOnline = isOnline,
                                enabled = isToggleable,
                                onCheckedChange = { desired ->
                                    if (!desired && !canOfflineCore(i, coreStates)) {
                                        Toast.makeText(
                                            context,
                                            "At least 1 little core must remain online",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val success = setCoreOnline(i, desired)
                                        if (success) {
                                            coreStates[i] = desired
                                        } else {
                                            coreStates[i] = isCoreOnline(i)
                                            Toast.makeText(
                                                context,
                                                "Failed to change Core $i state",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )

                            if (i < 7) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.ShapeLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.core_control_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreRowItem(
    coreIndex: Int,
    title: String,
    subtitle: String,
    isOnline: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val animatedStatusColor by animateColorAsState(
        targetValue = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "core_status_color"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { role = Role.Switch },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = animatedStatusColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = isOnline,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            thumbContent = {
                Icon(
                    imageVector = if (isOnline) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}
