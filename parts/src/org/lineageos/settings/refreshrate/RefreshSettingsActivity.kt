/*
 * Copyright (C) 2026 The LineageOS Project
 *           (C) 2026 zylhdrXP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.lineageos.settings.refreshrate

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import org.lineageos.settings.R
import org.lineageos.settings.ui.theme.ExpressiveMotion
import org.lineageos.settings.ui.theme.ExpressiveShapes
import org.lineageos.settings.ui.theme.ExpressiveTheme
import org.lineageos.settings.ui.theme.ExpressiveTypography
import org.lineageos.settings.ui.theme.expressiveBounceClick

class RefreshSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpressiveTheme {
                BackHandler { finish() }
                RefreshSettingsScreen(onBackPressed = ::finish)
            }
        }
    }
}

private data class RefreshApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshSettingsScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val refreshUtils = remember(context) { RefreshUtils(context) }
    var apps by remember { mutableStateOf(emptyList<RefreshApp>()) }
    var query by remember { mutableStateOf("") }
    var refreshVersion by remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun loadApps() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        apps = packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val info = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
                if (!info.enabled) return@mapNotNull null
                RefreshApp(
                    packageName = info.packageName,
                    label = info.loadLabel(packageManager).toString(),
                    icon = info.loadIcon(packageManager),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    LaunchedEffect(Unit) {
        loadApps()
    }

    val filteredApps = remember(apps, query, refreshVersion) {
        apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.refresh_title),
                        style = ExpressiveTypography.HeadlineMediumEmphasized
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.onyx_navigate_back),
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
        containerColor = MaterialTheme.colorScheme.surface,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = ExpressiveShapes.ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxSize(),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.refresh_search),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = query.isNotEmpty(),
                            enter = fadeIn(ExpressiveMotion.effectsFast()),
                            exit = fadeOut(ExpressiveMotion.effectsFast())
                        ) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = ExpressiveShapes.ShapeFull
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    RefreshAppCard(
                        app = app,
                        selectedMode = refreshUtils.getStateForPackage(app.packageName),
                        onModeSelected = { mode ->
                            refreshUtils.writePackage(app.packageName, mode)
                            refreshVersion++
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RefreshAppCard(
    app: RefreshApp,
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveShapes.ShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Optical roundness: outer (16dp) - padding (8dp) = 8dp (CornerSmall)
            Image(
                bitmap = remember(app.packageName) { app.icon.toBitmap(48, 48).asImageBitmap() },
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(ExpressiveShapes.ShapeSmall),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RefreshModeMenu(
                selectedMode = selectedMode,
                onModeSelected = onModeSelected,
            )
        }
    }
}

@Composable
private fun RefreshModeMenu(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
) {
    val modes = listOf(
        R.string.refresh_default,
        R.string.refresh_60,
        R.string.refresh_90,
        R.string.refresh_120,
        R.string.refresh_60_land,
        R.string.refresh_90_land,
        R.string.refresh_120_land,
    )
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = ExpressiveShapes.ShapeMedium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.semantics { role = Role.Button }
        ) {
            Text(
                text = stringResource(modes.getOrElse(selectedMode) { R.string.refresh_default }),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.clip(ExpressiveShapes.ShapeLarge)
        ) {
            modes.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(label),
                            style = if (index == selectedMode) ExpressiveTypography.LabelLargeEmphasized else MaterialTheme.typography.bodyMedium,
                            color = if (index == selectedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        expanded = false
                        onModeSelected(index)
                    },
                )
            }
        }
    }
}
