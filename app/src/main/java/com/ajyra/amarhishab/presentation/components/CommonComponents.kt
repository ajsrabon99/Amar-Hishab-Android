package com.ajyra.amarhishab.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.ajyra.amarhishab.R
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajyra.amarhishab.data.local.CategoryManager
import com.ajyra.amarhishab.model.AccountType
import com.ajyra.amarhishab.model.Transaction
import com.ajyra.amarhishab.model.TransactionType
import com.ajyra.amarhishab.ui.theme.AccountBkash
import com.ajyra.amarhishab.ui.theme.AccountNagad
import com.ajyra.amarhishab.ui.theme.BankBlue
import com.ajyra.amarhishab.ui.theme.CashGreen
import com.ajyra.amarhishab.ui.theme.EmeraldPrimary
import com.ajyra.amarhishab.ui.theme.ExpenseRed
import com.ajyra.amarhishab.ui.theme.FintechPrimary
import com.ajyra.amarhishab.ui.theme.IncomeGreen
import com.ajyra.amarhishab.ui.theme.TransferBlue
import com.ajyra.amarhishab.utils.CurrencyFormatter
import com.ajyra.amarhishab.utils.DateUtils

// -------------------------------------------------------------------------
// BUTTON DESIGN SYSTEM (PART 9)
// -------------------------------------------------------------------------

enum class AmarButtonType {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT,
    DESTRUCTIVE
}

@Composable
fun AmarButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    type: AmarButtonType = AmarButtonType.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "button_scale"
    )

    val finalModifier = modifier
        .height(50.dp)
        .scale(animatedScale)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)

    when (type) {
        AmarButtonType.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FintechPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = FintechPrimary.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                modifier = finalModifier
            ) {
                ButtonContent(icon = icon, text = text, isLoading = isLoading, contentColor = Color.White)
            }
        }
        AmarButtonType.SECONDARY -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = finalModifier
            ) {
                ButtonContent(
                    icon = icon,
                    text = text,
                    isLoading = isLoading,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AmarButtonType.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                modifier = finalModifier
            ) {
                ButtonContent(
                    icon = icon,
                    text = text,
                    isLoading = isLoading,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
        AmarButtonType.TEXT -> {
            TextButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = finalModifier
            ) {
                ButtonContent(
                    icon = icon,
                    text = text,
                    isLoading = isLoading,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
        AmarButtonType.DESTRUCTIVE -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ExpenseRed,
                    contentColor = Color.White,
                    disabledContainerColor = ExpenseRed.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                modifier = finalModifier
            ) {
                ButtonContent(icon = icon, text = text, isLoading = isLoading, contentColor = Color.White)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    icon: ImageVector?,
    text: String,
    isLoading: Boolean,
    contentColor: Color
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = contentColor,
            strokeWidth = 2.dp
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

// -------------------------------------------------------------------------
// TOP APP BAR
// -------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceTopBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable (androidx.compose.foundation.layout.RowScope.() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(imageVector = navigationIcon, contentDescription = "Back")
                }
            }
        },
        actions = {
            actions?.invoke(this)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

// -------------------------------------------------------------------------
// HERO BALANCE CARD (PART 6)
// -------------------------------------------------------------------------

private data class LakeWavelet(
    val relY: Float,
    val xOffsetFactor: Float,
    val widthDp: Float,
    val thicknessDp: Float,
    val speed: Float,
    val phase: Float,
    val baseAlpha: Float
)

private val LAKE_WAVELETS = listOf(
    // Distant water horizon (directly below moon, denser, delicate)
    LakeWavelet(0.03f, 0.000f, 10f, 1.0f, 1.0f, 0.0f, 0.32f),
    LakeWavelet(0.07f, -0.004f, 14f, 1.1f, 1.2f, 0.9f, 0.30f),
    LakeWavelet(0.11f, 0.003f, 8f, 0.9f, 0.9f, 1.8f, 0.28f),
    LakeWavelet(0.15f, -0.006f, 18f, 1.2f, 1.1f, 2.7f, 0.27f),
    LakeWavelet(0.19f, 0.005f, 12f, 1.0f, 0.8f, 3.6f, 0.26f),
    LakeWavelet(0.23f, -0.002f, 16f, 1.1f, 1.3f, 4.5f, 0.24f),
    // Upper-mid water with natural gaps & varied width
    LakeWavelet(0.28f, 0.007f, 9f, 1.1f, 1.0f, 0.4f, 0.23f),
    LakeWavelet(0.33f, -0.009f, 21f, 1.3f, 1.1f, 1.5f, 0.22f),
    LakeWavelet(0.38f, 0.002f, 15f, 1.2f, 0.9f, 2.6f, 0.20f),
    LakeWavelet(0.43f, -0.006f, 24f, 1.3f, 1.2f, 3.8f, 0.19f),
    LakeWavelet(0.48f, 0.008f, 11f, 1.1f, 0.8f, 4.9f, 0.18f),
    // Mid water with perspective spread
    LakeWavelet(0.53f, -0.012f, 26f, 1.4f, 1.0f, 0.7f, 0.17f),
    LakeWavelet(0.58f, 0.006f, 17f, 1.2f, 1.2f, 1.9f, 0.16f),
    LakeWavelet(0.63f, -0.003f, 23f, 1.4f, 0.9f, 3.1f, 0.15f),
    LakeWavelet(0.68f, 0.011f, 13f, 1.2f, 1.1f, 4.2f, 0.14f),
    LakeWavelet(0.73f, -0.014f, 28f, 1.5f, 0.8f, 5.3f, 0.13f),
    // Lower water gently fading out towards foreground
    LakeWavelet(0.78f, 0.004f, 19f, 1.3f, 1.3f, 0.2f, 0.12f),
    LakeWavelet(0.83f, -0.008f, 25f, 1.4f, 1.0f, 1.6f, 0.10f),
    LakeWavelet(0.88f, 0.012f, 16f, 1.2f, 0.9f, 2.8f, 0.09f),
    LakeWavelet(0.93f, -0.005f, 21f, 1.3f, 1.1f, 4.1f, 0.07f),
    LakeWavelet(0.97f, 0.009f, 14f, 1.1f, 0.8f, 5.4f, 0.05f)
)

@Composable
fun HeroBalanceCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    isBengali: Boolean,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onTransferClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean = false,
    onToggleHideBalance: (() -> Unit)? = null
) {
    // Calm natural ambient motion: flock of birds gliding, gentle breeze, water ripple shimmer
    val infiniteTransition = rememberInfiniteTransition(label = "hero_nature_motion")
    val natureShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nature_float"
    )
    val rippleShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "water_ripple"
    )
    // Flock flight progression across the sky (slow, calm 28-second loop)
    val flockProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flock_flight"
    )
    // Wing gentle flap cycle (1.8-second cycle)
    val wingCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wing_cycle"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.65f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050F1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
        ) {
            // 1. Natural Landscape Artwork Asset (Moon, mountains, lake, and scenery from reference)
            Image(
                painter = painterResource(id = R.drawable.bg_balance_nature),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // 2. Subtle Natural Animation Overlay: Flock gliding, moonlight reflections, swaying leaves
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                // A. REALISTIC MOONLIGHT WATER REFLECTION ON THE CALM LAKE
                // Originates directly below the moon (x ~ 0.775w) and stays inside the water area (y ~ 0.355h to 0.505h)
                val moonReflectX = w * 0.775f
                val lakeTop = h * 0.355f
                val lakeBottom = h * 0.505f
                val lakeDepth = lakeBottom - lakeTop

                // Soft atmospheric / water surface haze right below the distant shore
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x38FEF3C7),
                            Color(0x18FDE68A),
                            Color.Transparent
                        ),
                        center = Offset(moonReflectX, lakeTop + 3.dp.toPx()),
                        radius = w * 0.12f
                    ),
                    radius = w * 0.12f,
                    center = Offset(moonReflectX, lakeTop + 3.dp.toPx())
                )

                // Irregular broken shimmering water wavelets with soft feathered edges
                for (wavelet in LAKE_WAVELETS) {
                    val wy = lakeTop + (wavelet.relY * lakeDepth)
                    // Horizontal drift gently shifting with the water ripple
                    val waveOscillation = kotlin.math.sin((rippleShift * 6.28f * wavelet.speed) + wavelet.phase).toFloat() * 2.2.dp.toPx()
                    val wx = moonReflectX + (wavelet.xOffsetFactor * w) + waveOscillation
                    val wWidth = wavelet.widthDp.dp.toPx()
                    val wHeight = wavelet.thicknessDp.dp.toPx()

                    // Shimmer opacity variation: breathing gently and asynchronously
                    val shimmerAlpha = (wavelet.baseAlpha * (0.80f + 0.35f * kotlin.math.sin((rippleShift * 6.28f * 0.9f * wavelet.speed) + wavelet.phase).toFloat()))
                        .coerceIn(0.02f, 0.35f)

                    // Soft horizontal gradient for feathered edges without hard borders
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFEF3C7).copy(alpha = shimmerAlpha),
                                Color.Transparent
                            ),
                            startX = wx - (wWidth / 2f),
                            endX = wx + (wWidth / 2f)
                        ),
                        topLeft = Offset(wx - (wWidth / 2f), wy),
                        size = androidx.compose.ui.geometry.Size(wWidth, wHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(wHeight / 2f, wHeight / 2f)
                    )
                }

                // B. CLEARLY VISIBLE NATURAL FLOCK OF BIRDS IN UPPER SKY
                // Travel gracefully across the luminous blue sky between the AMAR HISHAB badge and moon
                // Alpha envelope: quadratic sine curve so birds smoothly fade in from distant mist and fade out gently near the moon
                val flockAlpha = (kotlin.math.sin(flockProgress * Math.PI.toFloat())).coerceIn(0f, 1f)
                val smoothFlockAlpha = flockAlpha * flockAlpha

                if (smoothFlockAlpha > 0.005f) {
                    val flockStartX = w * 0.32f
                    val flockEndX = w * 0.78f
                    val flockCenterX = flockStartX + (flockProgress * (flockEndX - flockStartX))
                    val flockCenterY = h * 0.135f + kotlin.math.sin(flockProgress * 6.28f).toFloat() * 3.5.dp.toPx()

                    fun drawBird(
                        relX: Float,
                        relY: Float,
                        spanPx: Float,
                        flapOffset: Float
                    ) {
                        val bx = flockCenterX + relX
                        val by = flockCenterY + relY
                        // Stay safely within the card bounds
                        if (bx < 10f || bx > w - 10f || by < 5f || by > h - 10f) return

                        // Wing flapping motion: smooth sinusoidal movement with gliding intervals
                        val flapAngle = kotlin.math.sin((wingCycle + flapOffset) * 6.28f).toFloat()
                        val flapFactor = (flapAngle * 0.65f) + 0.35f // -0.3f (down) to +1.0f (up)
                        val wingLift = spanPx * 0.42f * flapFactor

                        val p = androidx.compose.ui.graphics.Path().apply {
                            // Body head
                            moveTo(bx, by + spanPx * 0.10f)
                            // Left wing curve up to tip
                            cubicTo(
                                bx - spanPx * 0.35f, by - wingLift * 0.85f,
                                bx - spanPx * 0.70f, by - wingLift,
                                bx - spanPx, by - wingLift * 0.70f
                            )
                            // Left wing lower curve back to body
                            cubicTo(
                                bx - spanPx * 0.65f, by - wingLift * 0.35f,
                                bx - spanPx * 0.25f, by - wingLift * 0.08f,
                                bx, by + spanPx * 0.20f
                            )
                            // Right wing lower curve to tip
                            cubicTo(
                                bx + spanPx * 0.25f, by - wingLift * 0.08f,
                                bx + spanPx * 0.65f, by - wingLift * 0.35f,
                                bx + spanPx, by - wingLift * 0.70f
                            )
                            // Right wing upper curve back to head
                            cubicTo(
                                bx + spanPx * 0.70f, by - wingLift,
                                bx + spanPx * 0.35f, by - wingLift * 0.85f,
                                bx, by + spanPx * 0.10f
                            )
                            close()
                        }
                        // Distinct dark silhouette visible against the luminous blue night sky
                        drawPath(
                            path = p,
                            color = Color(0xFF060B14).copy(alpha = (0.95f * smoothFlockAlpha).coerceIn(0f, 1f))
                        )
                    }

                    // Natural loose flock formation of 6 birds
                    drawBird(0f, 0f, 6.8.dp.toPx(), 0.0f)                            // Leader
                    drawBird(-16.dp.toPx(), -5.5.dp.toPx(), 5.4.dp.toPx(), 0.18f)    // Left wing
                    drawBird(-31.dp.toPx(), -10.dp.toPx(), 4.5.dp.toPx(), 0.35f)     // Left rear
                    drawBird(-14.dp.toPx(), 6.5.dp.toPx(), 5.6.dp.toPx(), 0.12f)     // Right wing
                    drawBird(-28.dp.toPx(), 12.dp.toPx(), 4.7.dp.toPx(), 0.28f)      // Right rear
                    drawBird(-42.dp.toPx(), 3.dp.toPx(), 4.1.dp.toPx(), 0.45f)       // Trailing
                }

                // C. GENTLY SWAYING GREEN LEAVES IN THE BREEZE
                val leafColor = Color(0xFF34D399).copy(alpha = 0.85f)
                fun drawAnimatedLeaf(lx: Float, ly: Float, angleDegrees: Float, scale: Float) {
                    val leafPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(lx, ly - 7f * scale)
                        cubicTo(lx + 6f * scale, ly - 3f * scale, lx + 6f * scale, ly + 3f * scale, lx, ly + 7f * scale)
                        cubicTo(lx - 6f * scale, ly + 3f * scale, lx - 6f * scale, ly - 3f * scale, lx, ly - 7f * scale)
                        close()
                    }
                    rotate(degrees = angleDegrees, pivot = Offset(lx, ly)) {
                        drawPath(path = leafPath, color = leafColor)
                    }
                }

                // Leaf 1: upper middle drifting down-right
                val l1X = w * (0.47f + natureShift * 0.03f)
                val l1Y = h * (0.16f + natureShift * 0.02f)
                drawAnimatedLeaf(l1X, l1Y, 35f + natureShift * 20f, 1.2f)

                // Leaf 2: right lower edge floating gently
                val l2X = w * (0.95f - natureShift * 0.02f)
                val l2Y = h * (0.50f + natureShift * 0.03f)
                drawAnimatedLeaf(l2X, l2Y, -40f - natureShift * 15f, 1.3f)
            }

            // 3. Foreground Card Content matching reference hierarchy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row: Brand badge & Balance visibility toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "AMAR HISHAB" Brand Badge with purple/indigo dot
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F1E33).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AMAR HISHAB",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.6.sp,
                                    fontSize = 11.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    // Circular eye button
                    if (onToggleHideBalance != null) {
                        IconButton(
                            onClick = onToggleHideBalance,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("toggle_hide_balance_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF16253B).copy(alpha = 0.90f),
                                border = BorderStroke(1.dp, Color(0xFF2A3E58)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isBalanceHidden) "Show Balance" else "Hide Balance",
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Balance Label
                Text(
                    text = if (isBengali) "মোট উপলব্ধ ব্যালেন্স" else "Total Available Balance",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Large Bold Balance Amount
                Text(
                    text = if (isBalanceHidden) "৳ ••••••" else CurrencyFormatter.format(totalBalance, isBengali),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 38.sp,
                        letterSpacing = if (isBalanceHidden) 3.sp else (-0.5).sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                // Subtitle
                Text(
                    text = if (isBengali) "সকল অ্যাকাউন্ট সংযুক্ত" else "All accounts combined",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Side-by-side Summary Panels (Income & Expense)
                // Layout ensures amounts NEVER truncate, never show "...", and dynamically adapt text size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Income Sub-card (Green border, green downward badge)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF03261F).copy(alpha = 0.75f),
                        border = BorderStroke(1.5.dp, Color(0xFF00C853))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // Top Row: circular icon + label
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF064E3B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = if (isBengali) "মোট আয়" else "Income",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            // Amount Row: Full card width available with responsive text sizing
                            val incomeFormatted = if (isBalanceHidden) "••••" else CurrencyFormatter.format(totalIncome, isBengali)
                            val incomeFontSize = when {
                                incomeFormatted.length <= 9 -> 16.sp
                                incomeFormatted.length <= 11 -> 14.5.sp
                                incomeFormatted.length <= 13 -> 13.sp
                                else -> 11.5.sp
                            }
                            Text(
                                text = incomeFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = incomeFontSize,
                                    letterSpacing = (-0.2).sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Expense Sub-card (Red/Pink border, red upward badge)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF380715).copy(alpha = 0.75f),
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // Top Row: circular icon + label
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF7F1D1D)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = if (isBengali) "মোট ব্যয়" else "Expense",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            // Amount Row: Full card width available with responsive text sizing
                            val expenseFormatted = if (isBalanceHidden) "••••" else CurrencyFormatter.format(totalExpense, isBengali)
                            val expenseFontSize = when {
                                expenseFormatted.length <= 9 -> 16.sp
                                expenseFormatted.length <= 11 -> 14.5.sp
                                expenseFormatted.length <= 13 -> 13.sp
                                else -> 11.5.sp
                            }
                            Text(
                                text = expenseFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = expenseFontSize,
                                    letterSpacing = (-0.2).sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Three Bottom Action Buttons: Income (+), Expense (—), Transfer (⇄)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionPill(
                        label = if (isBengali) "আয়" else "Income",
                        icon = Icons.Default.Add,
                        onClick = onIncomeClick,
                        modifier = Modifier.weight(1f),
                        testTag = "hero_income_btn",
                        accentColor = Color(0xFF00E676)
                    )
                    QuickActionPill(
                        label = if (isBengali) "ব্যয়" else "Expense",
                        icon = Icons.Default.Remove,
                        onClick = onExpenseClick,
                        modifier = Modifier.weight(1f),
                        testTag = "hero_expense_btn",
                        accentColor = Color(0xFFEF4444)
                    )
                    QuickActionPill(
                        label = if (isBengali) "ট্রান্সফার" else "Transfer",
                        icon = Icons.Default.SyncAlt,
                        onClick = onTransferClick,
                        modifier = Modifier.weight(1.1f),
                        testTag = "hero_transfer_btn",
                        accentColor = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionPill(
    label: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "quick_pill",
    accentColor: Color = Color.White,
    isDark: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150),
        label = "pill_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .height(48.dp)
            .scale(animatedScale)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF132035).copy(alpha = 0.90f),
        border = BorderStroke(1.2.dp, Color(0xFF2C3E55))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "quick_action_btn"
) {
    AmarButton(
        onClick = onClick,
        text = label,
        icon = icon,
        type = AmarButtonType.SECONDARY,
        modifier = modifier,
        testTag = testTag
    )
}

// -------------------------------------------------------------------------
// ACCOUNT BALANCE PILL (PART 5)
// -------------------------------------------------------------------------

@Composable
fun AccountBalancePill(
    accountType: AccountType,
    balance: Double,
    isBengali: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val accentColor = when (accountType) {
        AccountType.BKASH -> AccountBkash
        AccountType.NAGAD -> AccountNagad
        AccountType.BANK -> BankBlue
        AccountType.CASH -> CashGreen
        else -> EmeraldPrimary
    }

    val icon = when (accountType) {
        AccountType.BKASH, AccountType.NAGAD -> Icons.Default.Smartphone
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.CASH -> Icons.Default.Payments
        else -> Icons.Default.Wallet
    }

    Card(
        modifier = modifier
            .testTag("account_pill_${accountType.code}")
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBengali) accountType.displayNameBn else accountType.displayNameEn,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = CurrencyFormatter.format(balance, isBengali),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// -------------------------------------------------------------------------
// TRANSACTION ROW ITEM (PART 7)
// -------------------------------------------------------------------------

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    isBengali: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "tx_row_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon representing type with soft rounded container
            val (icon, bgColor, tintColor) = when (transaction.type) {
                TransactionType.INCOME -> Triple(Icons.Default.ArrowDownward, IncomeGreen.copy(alpha = 0.12f), IncomeGreen)
                TransactionType.EXPENSE -> Triple(Icons.Default.ArrowUpward, ExpenseRed.copy(alpha = 0.12f), ExpenseRed)
                TransactionType.TRANSFER -> Triple(Icons.Default.SyncAlt, TransferBlue.copy(alpha = 0.12f), TransferBlue)
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val context = LocalContext.current
                val categoryDisplayName = remember(transaction.category, isBengali) {
                    CategoryManager.getInstance(context).getCategoryDisplayName(transaction.category, isBengali)
                }
                Text(
                    text = categoryDisplayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Account Tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = transaction.accountCode,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val desc = transaction.description
                    val details = if (!desc.isNullOrBlank()) desc else DateUtils.formatDisplay(transaction.date, isBengali)
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                val prefix = when (transaction.type) {
                    TransactionType.INCOME -> "+ "
                    TransactionType.EXPENSE -> "- "
                    TransactionType.TRANSFER -> ""
                }
                val amountText = prefix + CurrencyFormatter.format(transaction.amount, isBengali)
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tintColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateUtils.formatDisplay(transaction.date, isBengali),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// EMPTY STATE VIEW
// -------------------------------------------------------------------------

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(FintechPrimary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(FintechPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = FintechPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            AmarButton(
                onClick = onActionClick,
                text = actionLabel,
                type = AmarButtonType.PRIMARY
            )
        }
    }
}
