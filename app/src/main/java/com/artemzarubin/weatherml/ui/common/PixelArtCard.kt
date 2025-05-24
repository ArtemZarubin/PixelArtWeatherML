// File: com/artemzarubin/weatherml/ui/common/PixelArtCard.kt
package com.artemzarubin.weatherml.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PixelArtCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    borderColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 2.dp, // Default to 2.dp, can be set to 0.dp
    cornerRadius: Dp = 3.dp,
    internalPadding: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape: Shape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RectangleShape
    var borderModifier = Modifier.border( // Create a base border modifier
        width = borderWidth,
        brush = SolidColor(borderColor),
        shape = shape
    )
    // If borderWidth is 0, don't apply the border modifier at all
    if (borderWidth <= 0.dp) {
        borderModifier = Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .then(borderModifier) // Apply borderModifier (it will be empty if borderWidth is 0)
            .padding(internalPadding),
        content = content
    )
}