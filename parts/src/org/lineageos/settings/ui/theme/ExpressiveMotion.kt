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

package org.lineageos.settings.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * M3 Expressive Motion Physics tokens.
 *
 * Reference: m3-expressive/references/tokens.md
 * - Expressive Spatial: carries moves, resizes, rotations, and corner morphs (overshoots/bounces).
 * - Expressive Effects: carries color and opacity transitions (damping 1.0, strictly no overshoot).
 */
object ExpressiveMotion {
    // Spatial springs (moves / translates / resizes / corner morphs)
    val DefaultSpatial: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 380f
    )

    val FastSpatial: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness = 800f
    )

    val SlowSpatial: SpringSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 200f
    )

    // Spatial springs for offsets / corner radii / heights
    fun <T> spatialDefault(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> spatialFast(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> spatialSlow(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)

    // Effects springs (color / alpha / tonal transitions)
    fun <T> effectsDefault(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 1600f)
    fun <T> effectsFast(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 3800f)
    fun <T> effectsSlow(): SpringSpec<T> = spring(dampingRatio = 1.0f, stiffness = 800f)
}
