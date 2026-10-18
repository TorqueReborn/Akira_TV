package com.ghostreborn.akira.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.ui.theme.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary

/**
 * TV Remote accessible button featuring:
 * - D-Pad focus state (`focusable()`)
 * - Smooth scale magnification on focus (1.05x)
 * - Gradient focus highlight and elevated shadow glow
 * - D-Pad Center / Enter click handling
 */
@Composable
fun AkiraTVButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    isPrimary: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "tvButtonScale"
    )

    val shape = RoundedCornerShape(14.dp)
    val isActionable = enabled && !loading

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color.Transparent,
        animationSpec = tween(150),
        label = "tvButtonBorder"
    )

    val elevation = if (isFocused) 12.dp else 4.dp

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = if (isFocused) AkiraPrimary else Color.Transparent
            )
            .clip(shape)
            .border(width = if (isFocused) 2.5.dp else 0.dp, color = borderColor, shape = shape)
            .background(
                when {
                    !isActionable -> SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    isFocused || isPrimary -> AkiraLogoGradientBrush
                    else -> SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                }
            )
            .semantics(mergeDescendants = true) {
                role = androidx.compose.ui.semantics.Role.Button
            }
            .clickable(
                enabled = isActionable,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White)
            ) {
                onClick()
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else if (leadingIcon != null) {
                androidx.compose.material3.Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = if (loading) "Please wait..." else text,
                color = if (isActionable) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
