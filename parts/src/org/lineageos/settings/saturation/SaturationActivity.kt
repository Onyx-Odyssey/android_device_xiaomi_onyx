/*
 * Copyright (C) 2025 kenway214
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

package org.lineageos.settings.saturation

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.os.ServiceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.lineageos.settings.Constants
import org.lineageos.settings.R
import org.lineageos.settings.ui.theme.ExpressiveMotion
import org.lineageos.settings.ui.theme.ExpressiveShapes
import org.lineageos.settings.ui.theme.ExpressiveTheme
import org.lineageos.settings.ui.theme.ExpressiveTypography
import org.lineageos.settings.ui.theme.expressiveBounceClick
import org.lineageos.settings.utils.TileUtils

class SaturationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val controller = SaturationController(this)
        controller.restore()

        setContent {
            ExpressiveTheme {
                BackHandler { finish() }
                SaturationScreen(
                    initialValue = controller.value,
                    onValueChanged = controller::setValue,
                    onReset = controller::reset,
                    onAddTile = {
                        TileUtils.requestAddTileService(
                            this,
                            SaturationTileService::class.java,
                            R.string.saturation_title,
                            R.drawable.ic_saturation_tile,
                        )
                    },
                    onBackPressed = ::finish,
                )
            }
        }
    }
}

private class SaturationController(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val surfaceFlinger: IBinder? = ServiceManager.getService("SurfaceFlinger")
    var value: Int = preferences.getInt(Constants.KEY_SATURATION, DEFAULT_VALUE)
        private set

    fun restore() {
        setValue(value)
    }

    fun setValue(newValue: Int) {
        value = newValue.coerceIn(MIN_VALUE, MAX_VALUE)
        preferences.edit { putInt(Constants.KEY_SATURATION, value) }
        val saturation = if (value == DEFAULT_VALUE) 1.001f else value / 100.0f
        surfaceFlinger?.let { binder ->
            val data = Parcel.obtain()
            try {
                data.writeInterfaceToken("android.ui.ISurfaceComposer")
                data.writeFloat(saturation)
                binder.transact(TRANSACTION_SET_SATURATION, data, null, 0)
            } catch (exception: RemoteException) {
                android.util.Log.e(TAG, "Unable to update display saturation", exception)
            } finally {
                data.recycle()
            }
        }
    }

    fun reset() {
        setValue(DEFAULT_VALUE)
    }

    private companion object {
        const val DEFAULT_VALUE = 100
        const val MIN_VALUE = 0
        const val MAX_VALUE = 200
        const val TRANSACTION_SET_SATURATION = 1022
        const val TAG = "SaturationController"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaturationScreen(
    initialValue: Int,
    onValueChanged: (Int) -> Unit,
    onReset: () -> Unit,
    onAddTile: () -> Unit,
    onBackPressed: () -> Unit,
) {
    var value by remember { mutableIntStateOf(initialValue) }
    var page by remember { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val previewImages = listOf(
        R.drawable.image_preview1,
        R.drawable.image_preview2,
        R.drawable.image_preview3,
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.saturation_title),
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
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.saturation_actions),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.clip(ExpressiveShapes.ShapeLarge)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tile_add)) },
                            leadingIcon = {
                                Icon(Icons.Default.AddToHomeScreen, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onAddTile()
                            },
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreviewCarousel(
                    images = previewImages,
                    page = page,
                    onPageChanged = { page = it },
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveShapes.ShapeExtraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.saturation_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = ExpressiveShapes.ShapeFull,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "$value%",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val saturationDescription = stringResource(R.string.saturation_title)
                        Slider(
                            value = value.toFloat(),
                            onValueChange = {
                                val newValue = it.toInt()
                                if (newValue != value) {
                                    value = newValue
                                    onValueChanged(newValue)
                                }
                            },
                            valueRange = 0f..200f,
                            steps = 199,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = saturationDescription
                                },
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("0%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("100%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("200%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                50 to "Natural",
                                100 to "Default",
                                140 to "Vibrant"
                            ).forEach { (presetVal, presetName) ->
                                AssistChip(
                                    onClick = {
                                        value = presetVal
                                        onValueChanged(presetVal)
                                    },
                                    label = { Text(presetName, style = MaterialTheme.typography.labelSmall) },
                                    shape = ExpressiveShapes.ShapeSmall,
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (value == presetVal) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = if (value == presetVal) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(
                                onClick = {
                                    value = 100
                                    onReset()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.saturation_reset),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
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
                            text = stringResource(R.string.saturation_footer_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCarousel(
    images: List<Int>,
    page: Int,
    onPageChanged: (Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .expressiveBounceClick(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = ExpressiveShapes.ShapeExtraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Optical roundness: inner = outer (28dp) - padding (8dp) = 20dp (CornerLargeIncreased)
            val innerShape = ExpressiveShapes.opticalInnerShape(ExpressiveShapes.CornerExtraLarge, 8.dp)
            Image(
                painter = painterResource(images[page]),
                contentDescription = stringResource(R.string.image_preview_content_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(innerShape),
                contentScale = ContentScale.Crop,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { onPageChanged(page - 1) },
                    enabled = page > 0,
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(
                            R.string.image_preview_previous_page_content_description,
                        ),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    images.indices.forEach { index ->
                        val isSelected = index == page
                        val indicatorWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = ExpressiveMotion.spatialFast(),
                            label = "indicator_width_$index"
                        )
                        val indicatorColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            animationSpec = ExpressiveMotion.effectsDefault(),
                            label = "indicator_color_$index"
                        )

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(indicatorWidth)
                                .background(
                                    color = indicatorColor,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }

                IconButton(
                    onClick = { onPageChanged(page + 1) },
                    enabled = page < images.lastIndex,
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(
                            R.string.image_preview_next_page_content_description,
                        ),
                    )
                }
            }
        }
    }
}
