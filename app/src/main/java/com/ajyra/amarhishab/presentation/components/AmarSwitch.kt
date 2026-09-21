package com.ajyra.amarhishab.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ajyra.amarhishab.ui.theme.FintechPrimary

/**
 * Premium, minimal Material-style animated toggle switch for Amar Hishab.
 * Designed with a 200ms easing curve, crisp active/inactive contrast in light and dark mode,
 * tactile geometry (48dp interactive width, 28dp track height, 22dp floating thumb).
 */
@Composable
fun AmarSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Dimensions: 48dp width, 28dp height, 22dp thumb
    val trackWidth = 48.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val thumbPadding = 3.dp

    // Animation transition: 200ms FastOutSlowInEasing for a responsive, tactile feel
    val animDuration = 200
    val animEasing = FastOutSlowInEasing

    // Thumb position offset (0.dp to 20.dp)
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - (thumbPadding * 2) else 0.dp,
        animationSpec = tween(durationMillis = animDuration, easing = animEasing),
        label = "amar_switch_thumb_offset"
    )

    // Palette adaptations
    val activeTrackColor = FintechPrimary
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeThumbColor = Color.White
    val inactiveThumbColor = MaterialTheme.colorScheme.onSurfaceVariant

    val trackColor by animateColorAsState(
        targetValue = if (checked) activeTrackColor else inactiveTrackColor,
        animationSpec = tween(durationMillis = animDuration, easing = animEasing),
        label = "amar_switch_track_color"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) activeThumbColor else inactiveThumbColor,
        animationSpec = tween(durationMillis = animDuration, easing = animEasing),
        label = "amar_switch_thumb_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) activeTrackColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = animDuration, easing = animEasing),
        label = "amar_switch_border_color"
    )

    val clickableModifier = if (onCheckedChange != null && enabled) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = false, radius = 24.dp),
            role = Role.Switch,
            onClick = { onCheckedChange(!checked) }
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickableModifier)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Track
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(trackColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(thumbPadding)
        ) {
            // Sliding Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .shadow(
                        elevation = if (checked) 2.dp else 1.dp,
                        shape = CircleShape,
                        clip = false
                    )
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}
