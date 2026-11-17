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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 10-step Corner Radius Scale from M3 Expressive.
 *
 * Reference: m3-expressive/references/tokens.md
 * - None: 0dp (CornerNone)
 * - Extra small: 4dp (CornerExtraSmall)
 * - Small: 8dp (CornerSmall)
 * - Medium: 12dp (CornerMedium)
 * - Large: 16dp (CornerLarge)
 * - Large increased: 20dp (CornerLargeIncreased) - New in M3 Expressive
 * - Extra large: 28dp (CornerExtraLarge)
 * - Extra large increased: 32dp (CornerExtraLargeIncreased) - New in M3 Expressive
 * - Extra extra large: 48dp (CornerExtraExtraLarge) - New in M3 Expressive
 * - Full: fully rounded (CornerFull)
 */
object ExpressiveShapes {
    val CornerNone: Dp = 0.dp
    val CornerExtraSmall: Dp = 4.dp
    val CornerSmall: Dp = 8.dp
    val CornerMedium: Dp = 12.dp
    val CornerLarge: Dp = 16.dp
    val CornerLargeIncreased: Dp = 20.dp
    val CornerExtraLarge: Dp = 28.dp
    val CornerExtraLargeIncreased: Dp = 32.dp
    val CornerExtraExtraLarge: Dp = 48.dp

    val ShapeNone: Shape = RectangleShape
    val ShapeExtraSmall: Shape = RoundedCornerShape(CornerExtraSmall)
    val ShapeSmall: Shape = RoundedCornerShape(CornerSmall)
    val ShapeMedium: Shape = RoundedCornerShape(CornerMedium)
    val ShapeLarge: Shape = RoundedCornerShape(CornerLarge)
    val ShapeLargeIncreased: Shape = RoundedCornerShape(CornerLargeIncreased)
    val ShapeExtraLarge: Shape = RoundedCornerShape(CornerExtraLarge)
    val ShapeExtraLargeIncreased: Shape = RoundedCornerShape(CornerExtraLargeIncreased)
    val ShapeExtraExtraLarge: Shape = RoundedCornerShape(CornerExtraExtraLarge)
    val ShapeFull: Shape = CircleShape

    /**
     * Compose MaterialTheme Shapes mapping
     */
    val Material3Shapes = Shapes(
        extraSmall = RoundedCornerShape(CornerExtraSmall),
        small = RoundedCornerShape(CornerSmall),
        medium = RoundedCornerShape(CornerMedium),
        large = RoundedCornerShape(CornerLarge),
        extraLarge = RoundedCornerShape(CornerExtraLarge)
    )

    /**
     * Optical roundness calculation for nested containers.
     * Formula: inner radius = outer radius - padding
     * Reference: m3-expressive/references/tokens.md
     */
    fun opticalInnerCorner(outerRadius: Dp, padding: Dp): Dp {
        val inner = outerRadius - padding
        return if (inner > 0.dp) inner else 0.dp
    }

    fun opticalInnerShape(outerRadius: Dp, padding: Dp): Shape {
        return RoundedCornerShape(opticalInnerCorner(outerRadius, padding))
    }
}
