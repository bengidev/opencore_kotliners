package io.github.bengidev.opencore.home.presenter.visuals

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.bengidev.opencore.home.theme.HomeTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val CanvasWidth = 360f
private const val CanvasHeight = 240f
private val GlyphRamp = listOf("░", "▒", "▓", "█")

/**
 * Pre-rasterized particle orb — architecture aligned with iOS HomeParticleOrbView.
 * Static layers are baked to bitmaps once per palette; only transforms animate per frame.
 */
@Composable
internal fun HomeParticleOrbView(
    modifier: Modifier = Modifier
) {
    val palette = HomeTheme.palette
    val density = LocalDensity.current
    val renderScale = maxOf(density.density, 2f)
    val reduceMotion = rememberReduceMotion()
    val shouldAnimate = !reduceMotion && rememberIsLifecycleActive()

    val pack = remember(palette.textPrimary, palette.accentPrimary, renderScale) {
        ParticleOrbAssetFactory.makePack(
            tint = palette.textPrimary,
            accent = palette.accentPrimary,
            renderScale = renderScale
        )
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val fitScale = minOf(maxWidth.value / CanvasWidth, maxHeight.value / CanvasHeight)

        Box(
            modifier = Modifier
                .size(CanvasWidth.dp, CanvasHeight.dp)
                .graphicsLayer {
                    scaleX = fitScale
                    scaleY = fitScale
                }
        ) {
            if (shouldAnimate) {
                val mainLayerTransition = rememberInfiniteTransition(label = "orb-main-layers")
                val orbitDotTransition = rememberInfiniteTransition(label = "orb-orbit-dots")
                val sparkTransition = rememberInfiniteTransition(label = "orb-sparks")

                pack.layers.forEach { descriptor ->
                    OrbMainLayer(
                        descriptor = descriptor,
                        shouldAnimate = true,
                        transition = mainLayerTransition
                    )
                }
                pack.outerOrbitDots.forEach { descriptor ->
                    OrbOrbitDotLayer(
                        descriptor = descriptor,
                        shouldAnimate = true,
                        transition = orbitDotTransition
                    )
                }
                pack.sparks.forEach { descriptor ->
                    OrbSparkLayer(
                        descriptor = descriptor,
                        shouldAnimate = true,
                        transition = sparkTransition
                    )
                }
            } else {
                pack.layers.forEach { descriptor ->
                    OrbMainLayer(
                        descriptor = descriptor,
                        shouldAnimate = false,
                        transition = null
                    )
                }
                pack.outerOrbitDots.forEach { descriptor ->
                    OrbOrbitDotLayer(
                        descriptor = descriptor,
                        shouldAnimate = false,
                        transition = null
                    )
                }
                pack.sparks.forEach { descriptor ->
                    OrbSparkLayer(
                        descriptor = descriptor,
                        shouldAnimate = false,
                        transition = null
                    )
                }
            }
        }
    }
}

@Composable
private fun OrbMainLayer(
    descriptor: ParticleOrbLayerDescriptor,
    shouldAnimate: Boolean,
    transition: InfiniteTransition?
) {
    val centerX = CanvasWidth / 2f
    val centerY = CanvasHeight / 2f

    if (!shouldAnimate) {
        Image(
            bitmap = descriptor.image,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            filterQuality = if (descriptor.crispEdges) FilterQuality.None else FilterQuality.Low,
            modifier = Modifier
                .size(CanvasWidth.dp, CanvasHeight.dp)
                .graphicsLayer {
                    alpha = descriptor.restOpacity
                    scaleX = descriptor.restScale
                    scaleY = descriptor.restScale
                }
        )
        return
    }

    val layerTransition = transition ?: rememberInfiniteTransition(label = "orb-layer-${descriptor.phaseOffset}")

    val driftProgress by layerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.driftDuration * 1000).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift-${descriptor.phaseOffset}"
    )

    val rotation by layerTransition.animateFloat(
        initialValue = -descriptor.rotationRange,
        targetValue = descriptor.rotationRange,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.rotationDuration * 1000).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation-${descriptor.phaseOffset}"
    )

    val scale by layerTransition.animateFloat(
        initialValue = descriptor.restScale - descriptor.scaleRange,
        targetValue = descriptor.restScale + descriptor.scaleRange,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.scaleDuration * 1000).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale-${descriptor.phaseOffset}"
    )

    val opacity by layerTransition.animateFloat(
        initialValue = (descriptor.restOpacity - descriptor.opacityRange).coerceAtLeast(0.02f),
        targetValue = (descriptor.restOpacity + descriptor.opacityRange).coerceAtMost(1f),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.opacityDuration * 1000).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity-${descriptor.phaseOffset}"
    )

    val driftAngle = driftProgress * PI.toFloat() * 2f + descriptor.phaseOffset.toFloat()
    val driftX = cos(driftAngle) * descriptor.driftRadius
    val driftY = sin(driftAngle) * descriptor.driftRadius * descriptor.driftVerticalScale

    Image(
        bitmap = descriptor.image,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        filterQuality = if (descriptor.crispEdges) FilterQuality.None else FilterQuality.Low,
            modifier = Modifier
                .size(CanvasWidth.dp, CanvasHeight.dp)
                .offset(driftX.dp, driftY.dp)
                .graphicsLayer {
                    this.alpha = opacity
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation * (180f / PI.toFloat())
                    transformOrigin = TransformOrigin(
                        centerX / CanvasWidth,
                        centerY / CanvasHeight
                    )
                }
    )
}

@Composable
private fun OrbOrbitDotLayer(
    descriptor: ParticleOrbOrbitDotDescriptor,
    shouldAnimate: Boolean,
    transition: InfiniteTransition?
) {
    val imageSize = descriptor.imageSize
    val halfW = imageSize.width / 2f
    val halfH = imageSize.height / 2f

    if (!shouldAnimate) {
        val position = descriptor.position(0f)
        Image(
            bitmap = descriptor.image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.Low,
            modifier = Modifier
                .size(imageSize.width.dp, imageSize.height.dp)
                .offset((position.x - halfW).dp, (position.y - halfH).dp)
                .graphicsLayer {
                    alpha = descriptor.restOpacity
                    scaleX = descriptor.restScale
                    scaleY = descriptor.restScale
                }
        )
        return
    }

    val orbitTransition = transition ?: rememberInfiniteTransition(label = "orbit-dot-${descriptor.phaseOffset}")

    val orbitProgress by orbitTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.orbitDuration * 1000).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit-${descriptor.phaseOffset}"
    )

    val opacity by orbitTransition.animateFloat(
        initialValue = (descriptor.restOpacity * 0.64f).coerceAtLeast(0.02f),
        targetValue = (descriptor.restOpacity * 1.22f).coerceAtMost(0.30f),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.opacityDuration * 1000).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity-${descriptor.phaseOffset}"
    )

    val scale by orbitTransition.animateFloat(
        initialValue = (descriptor.restScale - descriptor.scaleRange * 0.36f).coerceAtLeast(0.01f),
        targetValue = descriptor.restScale + descriptor.scaleRange,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.scaleDuration * 1000).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale-${descriptor.phaseOffset}"
    )

    val position = descriptor.position(orbitProgress)

    Image(
        bitmap = descriptor.image,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.Low,
        modifier = Modifier
            .size(imageSize.width.dp, imageSize.height.dp)
            .offset((position.x - halfW).dp, (position.y - halfH).dp)
            .graphicsLayer {
                this.alpha = opacity
                scaleX = scale
                scaleY = scale
            }
    )
}

@Composable
private fun OrbSparkLayer(
    descriptor: ParticleOrbSparkDescriptor,
    shouldAnimate: Boolean,
    transition: InfiniteTransition?
) {
    val imageSize = descriptor.imageSize
    val halfW = imageSize.width / 2f
    val halfH = imageSize.height / 2f

    if (!shouldAnimate) {
        val position = descriptor.position(0f)
        Image(
            bitmap = descriptor.image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .size(imageSize.width.dp, imageSize.height.dp)
                .offset((position.x - halfW).dp, (position.y - halfH).dp)
                .graphicsLayer { alpha = 0f }
        )
        return
    }

    val sparkTransition = transition ?: rememberInfiniteTransition(label = "spark-${descriptor.phaseOffset}")

    val orbitProgress by sparkTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.orbitDuration * 1000).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit-${descriptor.phaseOffset}"
    )

    val opacity by sparkTransition.animateFloat(
        initialValue = (descriptor.restOpacity * 0.58f).coerceAtLeast(0.03f),
        targetValue = (descriptor.restOpacity * 1.18f).coerceAtMost(0.42f),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.opacityDuration * 1000).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity-${descriptor.phaseOffset}"
    )

    val scale by sparkTransition.animateFloat(
        initialValue = (descriptor.restScale - descriptor.scaleRange * 0.24f).coerceAtLeast(0.01f),
        targetValue = descriptor.restScale + descriptor.scaleRange * 0.46f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (descriptor.scaleDuration * 1000).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale-${descriptor.phaseOffset}"
    )

    val position = descriptor.position(orbitProgress)

    Image(
        bitmap = descriptor.image,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = Modifier
            .size(imageSize.width.dp, imageSize.height.dp)
            .offset((position.x - halfW).dp, (position.y - halfH).dp)
            .graphicsLayer {
                this.alpha = opacity
                scaleX = scale
                scaleY = scale
            }
    )
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

@Composable
private fun rememberIsLifecycleActive(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isActive by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isActive = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return isActive
}

private data class ParticleOrbAssetPack(
    val layers: List<ParticleOrbLayerDescriptor>,
    val outerOrbitDots: List<ParticleOrbOrbitDotDescriptor>,
    val sparks: List<ParticleOrbSparkDescriptor>
)

private data class ParticleOrbLayerDescriptor(
    val image: androidx.compose.ui.graphics.ImageBitmap,
    val restOpacity: Float,
    val opacityRange: Float,
    val opacityDuration: Double,
    val restScale: Float,
    val scaleRange: Float,
    val scaleDuration: Double,
    val rotationRange: Float,
    val rotationDuration: Double,
    val phaseOffset: Double,
    val crispEdges: Boolean,
    val driftRadius: Float = 0f,
    val driftVerticalScale: Float = 0.72f,
    val driftDuration: Double = 1.0
)

private data class ParticleOrbOrbitDotDescriptor(
    val image: androidx.compose.ui.graphics.ImageBitmap,
    val imageSize: androidx.compose.ui.geometry.Size,
    val orbitRadius: Float,
    val verticalScale: Float,
    val angleOffset: Float,
    val radialPulse: Float,
    val orbitDuration: Double,
    val opacityDuration: Double,
    val scaleDuration: Double,
    val phaseOffset: Double,
    val restOpacity: Float,
    val restScale: Float,
    val scaleRange: Float
) {
    fun position(progress: Float): androidx.compose.ui.geometry.Offset {
        val angle = angleOffset + progress * PI.toFloat() * 2f
        val radius = orbitRadius + sin(progress * PI.toFloat() * 2f + angleOffset) * radialPulse
        return androidx.compose.ui.geometry.Offset(
            x = ParticleOrbMetrics.center.x + cos(angle) * radius,
            y = ParticleOrbMetrics.center.y + sin(angle) * radius * verticalScale
        )
    }
}

private data class ParticleOrbSparkDescriptor(
    val image: androidx.compose.ui.graphics.ImageBitmap,
    val imageSize: androidx.compose.ui.geometry.Size,
    val orbitRadius: Float,
    val verticalScale: Float,
    val angleOffset: Float,
    val radialPulse: Float,
    val orbitDuration: Double,
    val opacityDuration: Double,
    val scaleDuration: Double,
    val phaseOffset: Double,
    val restOpacity: Float,
    val restScale: Float,
    val scaleRange: Float
) {
    fun position(progress: Float): androidx.compose.ui.geometry.Offset {
        val angle = angleOffset + progress * PI.toFloat() * 2f
        val radius = orbitRadius + sin(progress * PI.toFloat() * 2f + angleOffset * 0.6f) * radialPulse
        return androidx.compose.ui.geometry.Offset(
            x = ParticleOrbMetrics.center.x + cos(angle) * radius,
            y = ParticleOrbMetrics.center.y + sin(angle) * radius * verticalScale
        )
    }
}

private object ParticleOrbMetrics {
    val canvasSize = androidx.compose.ui.geometry.Size(CanvasWidth, CanvasHeight)
    val center = androidx.compose.ui.geometry.Offset(CanvasWidth * 0.5f, CanvasHeight * 0.5f)
    val outerField = androidx.compose.ui.geometry.Size(324f, 204f)
    val coreField = androidx.compose.ui.geometry.Size(156f, 146f)
    const val SnapGrid = 3f
}

private data class ParticleDot(
    val point: androidx.compose.ui.geometry.Offset,
    val size: Float,
    val opacity: Float
)

private data class ParticleBlock(
    val point: androidx.compose.ui.geometry.Offset,
    val glyph: String,
    val size: Float,
    val opacity: Float
)

private data class ParticleOrbitDotSeed(
    val orbitRadius: Float,
    val verticalScale: Float,
    val angleOffset: Float,
    val radialPulse: Float,
    val orbitDuration: Double,
    val opacityDuration: Double,
    val scaleDuration: Double,
    val phaseOffset: Double,
    val restOpacity: Float,
    val restScale: Float,
    val scaleRange: Float
)

private data class ParticleSparkSeed(
    val glyph: String,
    val pointSize: Float,
    val orbitRadius: Float,
    val verticalScale: Float,
    val angleOffset: Float,
    val radialPulse: Float,
    val orbitDuration: Double,
    val opacityDuration: Double,
    val scaleDuration: Double,
    val phaseOffset: Double,
    val restOpacity: Float,
    val restScale: Float,
    val scaleRange: Float
)

private object ParticleOrbAssetFactory {
    fun makePack(tint: Color, accent: Color, renderScale: Float): ParticleOrbAssetPack {
        val outerOrbitDotImage = ParticleOrbRenderer.renderOrbitDot(tint, renderScale)
        val sparkSeeds = ParticleOrbLayoutFactory.makeSparkSeeds(seedOffset = 11200, count = 28)
        val outerOrbitDotSeeds = ParticleOrbLayoutFactory.makeOuterOrbitDotSeeds(seedOffset = 12800, count = 42)

        return ParticleOrbAssetPack(
            layers = listOf(
                layer(
                    tint = tint,
                    accent = accent,
                    renderScale = renderScale,
                    dots = ParticleOrbLayoutFactory.makeOuterDots(seedOffset = 0, count = 138, radiusBias = 0.72),
                    restOpacity = 0.36f,
                    opacityRange = 0.05f,
                    opacityDuration = 6.8,
                    restScale = 1f,
                    scaleRange = 0.018f,
                    scaleDuration = 8.4,
                    rotationRange = 0.09f,
                    rotationDuration = 21.0,
                    phaseOffset = 0.0,
                    crispEdges = false,
                    driftRadius = 5f,
                    driftVerticalScale = 0.72f,
                    driftDuration = 16.0,
                    isBlocks = false
                ),
                layer(
                    tint = tint,
                    accent = accent,
                    renderScale = renderScale,
                    dots = ParticleOrbLayoutFactory.makeOuterDots(seedOffset = 1200, count = 126, radiusBias = 0.66),
                    restOpacity = 0.28f,
                    opacityRange = 0.05f,
                    opacityDuration = 7.6,
                    restScale = 0.98f,
                    scaleRange = 0.022f,
                    scaleDuration = 9.2,
                    rotationRange = 0.07f,
                    rotationDuration = 17.5,
                    phaseOffset = 1.9,
                    crispEdges = false,
                    driftRadius = 7f,
                    driftVerticalScale = 0.56f,
                    driftDuration = 19.0,
                    isBlocks = false
                ),
                layer(
                    tint = tint,
                    accent = accent,
                    renderScale = renderScale,
                    dots = ParticleOrbLayoutFactory.makePulseDots(seedOffset = 1800, count = 86),
                    restOpacity = 0.20f,
                    opacityRange = 0.10f,
                    opacityDuration = 5.2,
                    restScale = 0.78f,
                    scaleRange = 0.12f,
                    scaleDuration = 5.8,
                    rotationRange = 0.04f,
                    rotationDuration = 13.0,
                    phaseOffset = 0.4,
                    crispEdges = false,
                    driftRadius = 2f,
                    driftVerticalScale = 0.7f,
                    driftDuration = 11.0,
                    isBlocks = false
                ),
                layer(
                    tint = tint,
                    accent = accent,
                    renderScale = renderScale,
                    blocks = ParticleOrbLayoutFactory.makeCoreBlocks(seedOffset = 2400, count = 236, prominence = 0.96),
                    restOpacity = 0.78f,
                    opacityRange = 0.10f,
                    opacityDuration = 5.8,
                    restScale = 1f,
                    scaleRange = 0.028f,
                    scaleDuration = 6.6,
                    rotationRange = 0.10f,
                    rotationDuration = 12.0,
                    phaseOffset = 0.8,
                    crispEdges = true,
                    driftRadius = 4f,
                    driftVerticalScale = 0.74f,
                    driftDuration = 8.5,
                    isBlocks = true
                ),
                layer(
                    tint = tint,
                    accent = accent,
                    renderScale = renderScale,
                    blocks = ParticleOrbLayoutFactory.makeCoreBlocks(seedOffset = 4800, count = 220, prominence = 0.78),
                    restOpacity = 0.52f,
                    opacityRange = 0.09f,
                    opacityDuration = 6.4,
                    restScale = 1.02f,
                    scaleRange = 0.024f,
                    scaleDuration = 6.1,
                    rotationRange = 0.14f,
                    rotationDuration = 9.8,
                    phaseOffset = 2.2,
                    crispEdges = true,
                    driftRadius = 5f,
                    driftVerticalScale = 0.68f,
                    driftDuration = 7.8,
                    isBlocks = true
                ),
                layer(
                    tint = tint,
                    accent = accent,
                    renderScale = renderScale,
                    blocks = ParticleOrbLayoutFactory.makeCoreBlocks(seedOffset = 7200, count = 158, prominence = 0.58),
                    restOpacity = 0.30f,
                    opacityRange = 0.06f,
                    opacityDuration = 6.4,
                    restScale = 1.04f,
                    scaleRange = 0.018f,
                    scaleDuration = 6.1,
                    rotationRange = 0.18f,
                    rotationDuration = 7.4,
                    phaseOffset = 3.1,
                    crispEdges = true,
                    driftRadius = 6f,
                    driftVerticalScale = 0.64f,
                    driftDuration = 6.9,
                    isBlocks = true
                ),
                layer(
                    tint = tint,
                    accent = accent,
                    renderScale = renderScale,
                    dots = ParticleOrbLayoutFactory.makeOrbDust(seedOffset = 9600, count = 132),
                    restOpacity = 0.30f,
                    opacityRange = 0.08f,
                    opacityDuration = 4.4,
                    restScale = 1.01f,
                    scaleRange = 0.032f,
                    scaleDuration = 5.6,
                    rotationRange = 0.12f,
                    rotationDuration = 10.6,
                    phaseOffset = 1.5,
                    crispEdges = false,
                    driftRadius = 8f,
                    driftVerticalScale = 0.62f,
                    driftDuration = 12.4,
                    isBlocks = false
                )
            ),
            outerOrbitDots = outerOrbitDotSeeds.map { seed ->
                ParticleOrbOrbitDotDescriptor(
                    image = outerOrbitDotImage,
                    imageSize = androidx.compose.ui.geometry.Size(10f, 10f),
                    orbitRadius = seed.orbitRadius,
                    verticalScale = seed.verticalScale,
                    angleOffset = seed.angleOffset,
                    radialPulse = seed.radialPulse,
                    orbitDuration = seed.orbitDuration,
                    opacityDuration = seed.opacityDuration,
                    scaleDuration = seed.scaleDuration,
                    phaseOffset = seed.phaseOffset,
                    restOpacity = seed.restOpacity,
                    restScale = seed.restScale,
                    scaleRange = seed.scaleRange
                )
            },
            sparks = sparkSeeds.map { seed ->
                ParticleOrbSparkDescriptor(
                    image = ParticleOrbRenderer.renderSpark(tint, seed.glyph, seed.pointSize, renderScale),
                    imageSize = androidx.compose.ui.geometry.Size(18f, 18f),
                    orbitRadius = seed.orbitRadius,
                    verticalScale = seed.verticalScale,
                    angleOffset = seed.angleOffset,
                    radialPulse = seed.radialPulse,
                    orbitDuration = seed.orbitDuration,
                    opacityDuration = seed.opacityDuration,
                    scaleDuration = seed.scaleDuration,
                    phaseOffset = seed.phaseOffset,
                    restOpacity = seed.restOpacity,
                    restScale = seed.restScale,
                    scaleRange = seed.scaleRange
                )
            }
        )
    }

    private fun layer(
        tint: Color,
        accent: Color,
        renderScale: Float,
        dots: List<ParticleDot> = emptyList(),
        blocks: List<ParticleBlock> = emptyList(),
        restOpacity: Float,
        opacityRange: Float,
        opacityDuration: Double,
        restScale: Float,
        scaleRange: Float,
        scaleDuration: Double,
        rotationRange: Float,
        rotationDuration: Double,
        phaseOffset: Double,
        crispEdges: Boolean,
        driftRadius: Float,
        driftVerticalScale: Float,
        driftDuration: Double,
        isBlocks: Boolean
    ): ParticleOrbLayerDescriptor {
        val image = if (isBlocks) {
            ParticleOrbRenderer.renderBlocks(accent, blocks, renderScale)
        } else {
            ParticleOrbRenderer.renderDots(tint, dots, renderScale)
        }
        return ParticleOrbLayerDescriptor(
            image = image,
            restOpacity = restOpacity,
            opacityRange = opacityRange,
            opacityDuration = opacityDuration,
            restScale = restScale,
            scaleRange = scaleRange,
            scaleDuration = scaleDuration,
            rotationRange = rotationRange,
            rotationDuration = rotationDuration,
            phaseOffset = phaseOffset,
            crispEdges = crispEdges,
            driftRadius = driftRadius,
            driftVerticalScale = driftVerticalScale,
            driftDuration = driftDuration
        )
    }
}

private object ParticleOrbRenderer {
    fun renderDots(tint: Color, dots: List<ParticleDot>, renderScale: Float): androidx.compose.ui.graphics.ImageBitmap {
        val width = (CanvasWidth * renderScale).toInt()
        val height = (CanvasHeight * renderScale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        dots.forEach { dot ->
            paint.color = tint.copy(alpha = dot.opacity).toArgb()
            val radius = dot.size * renderScale * 0.5f
            canvas.drawCircle(
                dot.point.x * renderScale,
                dot.point.y * renderScale,
                radius,
                paint
            )
        }

        return bitmap.asImageBitmap()
    }

    fun renderBlocks(
        tint: Color,
        blocks: List<ParticleBlock>,
        renderScale: Float
    ): androidx.compose.ui.graphics.ImageBitmap {
        val width = (CanvasWidth * renderScale).toInt()
        val height = (CanvasHeight * renderScale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = false
            style = Paint.Style.FILL
        }

        blocks.forEach { block ->
            val cell = block.size * renderScale
            val fill = cell * blockFillScale(block.glyph)
            val inset = (cell - fill) / 2f
            val left = block.point.x * renderScale - cell / 2f + inset
            val top = block.point.y * renderScale - cell / 2f + inset
            paint.color = tint.copy(alpha = block.opacity).toArgb()
            canvas.drawRect(left, top, left + fill, top + fill, paint)
        }

        return bitmap.asImageBitmap()
    }

    private fun blockFillScale(glyph: String): Float = when (glyph) {
        "░" -> 0.34f
        "▒" -> 0.56f
        "▓" -> 0.78f
        else -> 1f
    }

    fun renderSpark(tint: Color, glyph: String, pointSize: Float, renderScale: Float): androidx.compose.ui.graphics.ImageBitmap {
        val size = (18f * renderScale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = false
            isSubpixelText = false
            typeface = Typeface.MONOSPACE
            color = tint.toArgb()
            textSize = pointSize * renderScale
        }
        val textWidth = paint.measureText(glyph)
        val fontMetrics = paint.fontMetrics
        canvas.drawText(
            glyph,
            (size - textWidth) / 2f,
            (size - (fontMetrics.ascent + fontMetrics.descent)) / 2f,
            paint
        )
        return bitmap.asImageBitmap()
    }

    fun renderOrbitDot(tint: Color, renderScale: Float): androidx.compose.ui.graphics.ImageBitmap {
        val size = (10f * renderScale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = tint.copy(alpha = 0.82f).toArgb()
        val inner = 6f * renderScale
        val innerOffset = 2f * renderScale
        canvas.drawOval(
            innerOffset,
            innerOffset,
            innerOffset + inner,
            innerOffset + inner,
            paint
        )

        paint.color = tint.copy(alpha = 0.18f).toArgb()
        val outer = 8.8f * renderScale
        val outerOffset = 0.6f * renderScale
        canvas.drawOval(
            outerOffset,
            outerOffset,
            outerOffset + outer,
            outerOffset + outer,
            paint
        )

        return bitmap.asImageBitmap()
    }
}

private object ParticleOrbLayoutFactory {
    fun makeOuterDots(seedOffset: Int, count: Int, radiusBias: Double): List<ParticleDot> {
        return buildList(count) {
            repeat(count) { index ->
                val seed = (seedOffset + index).toDouble()
                val orbit = 0.28 + ParticleOrbMath.noise(seed, 3.0).pow(0.82) * radiusBias
                val angle = ParticleOrbMath.noise(seed, 11.0) * PI * 2
                val jitterX = (ParticleOrbMath.noise(seed, 29.0) - 0.5) * 14
                val jitterY = (ParticleOrbMath.noise(seed, 37.0) - 0.5) * 11
                add(
                    ParticleDot(
                        point = androidx.compose.ui.geometry.Offset(
                            x = ParticleOrbMetrics.center.x + cos(angle).toFloat() * ParticleOrbMetrics.outerField.width * orbit.toFloat() * 0.5f + jitterX.toFloat(),
                            y = ParticleOrbMetrics.center.y + sin(angle).toFloat() * ParticleOrbMetrics.outerField.height * orbit.toFloat() * 0.5f + jitterY.toFloat()
                        ),
                        size = (1.2 + ParticleOrbMath.noise(seed, 47.0) * 2.4).toFloat(),
                        opacity = (0.16 + ParticleOrbMath.noise(seed, 59.0) * 0.28).toFloat()
                    )
                )
            }
        }
    }

    fun makeOrbDust(seedOffset: Int, count: Int): List<ParticleDot> {
        return buildList(count) {
            repeat(count) { index ->
                val seed = (seedOffset + index).toDouble()
                val angle = ParticleOrbMath.noise(seed, 5.0) * PI * 2
                val radial = 0.18 + ParticleOrbMath.noise(seed, 13.0).pow(0.58) * 0.50
                add(
                    ParticleDot(
                        point = androidx.compose.ui.geometry.Offset(
                            x = ParticleOrbMetrics.center.x + cos(angle).toFloat() * ParticleOrbMetrics.outerField.width * radial.toFloat() * 0.38f,
                            y = ParticleOrbMetrics.center.y + sin(angle).toFloat() * ParticleOrbMetrics.outerField.height * radial.toFloat() * 0.34f
                        ),
                        size = (1.1 + ParticleOrbMath.noise(seed, 23.0) * 2.0).toFloat(),
                        opacity = (0.10 + ParticleOrbMath.noise(seed, 31.0) * 0.18).toFloat()
                    )
                )
            }
        }
    }

    fun makePulseDots(seedOffset: Int, count: Int): List<ParticleDot> {
        return buildList(count) {
            repeat(count) { index ->
                val seed = (seedOffset + index).toDouble()
                val angle = index.toDouble() / count * PI * 2 + (ParticleOrbMath.noise(seed, 5.0) - 0.5) * 0.22
                val radius = (0.38 + ParticleOrbMath.noise(seed, 13.0) * 0.16).toFloat()
                add(
                    ParticleDot(
                        point = androidx.compose.ui.geometry.Offset(
                            x = ParticleOrbMetrics.center.x + cos(angle).toFloat() * ParticleOrbMetrics.coreField.width * radius,
                            y = ParticleOrbMetrics.center.y + sin(angle).toFloat() * ParticleOrbMetrics.coreField.height * radius * 0.70f
                        ),
                        size = (1.0 + ParticleOrbMath.noise(seed, 19.0) * 2.1).toFloat(),
                        opacity = (0.08 + ParticleOrbMath.noise(seed, 31.0) * 0.22).toFloat()
                    )
                )
            }
        }
    }

    fun makeSparkSeeds(seedOffset: Int, count: Int): List<ParticleSparkSeed> {
        return buildList(count) {
            repeat(count) { index ->
                val seed = (seedOffset + index).toDouble()
                val energy = ParticleOrbMath.noise(seed, 7.0)
                val glyphIndex = ((energy * GlyphRamp.size).roundToInt() - 1).coerceIn(0, GlyphRamp.size - 1)
                add(
                    ParticleSparkSeed(
                        glyph = GlyphRamp[glyphIndex],
                        pointSize = (5.2 + ParticleOrbMath.noise(seed, 23.0) * 4.8).toFloat(),
                        orbitRadius = (34 + ParticleOrbMath.noise(seed, 17.0) * 76).toFloat(),
                        verticalScale = (0.58 + ParticleOrbMath.noise(seed, 31.0) * 0.26).toFloat(),
                        angleOffset = (ParticleOrbMath.noise(seed, 13.0) * PI * 2).toFloat(),
                        radialPulse = (2 + ParticleOrbMath.noise(seed, 37.0) * 5).toFloat(),
                        orbitDuration = 8.5 + ParticleOrbMath.noise(seed, 41.0) * 10.0,
                        opacityDuration = 5.5 + ParticleOrbMath.noise(seed, 43.0) * 5.0,
                        scaleDuration = 6.0 + ParticleOrbMath.noise(seed, 47.0) * 5.0,
                        phaseOffset = ParticleOrbMath.noise(seed, 53.0) * 9.0,
                        restOpacity = (0.08 + ParticleOrbMath.noise(seed, 29.0) * 0.18).toFloat(),
                        restScale = (0.74 + ParticleOrbMath.noise(seed, 59.0) * 0.34).toFloat(),
                        scaleRange = (0.06 + ParticleOrbMath.noise(seed, 61.0) * 0.10).toFloat()
                    )
                )
            }
        }
    }

    fun makeOuterOrbitDotSeeds(seedOffset: Int, count: Int): List<ParticleOrbitDotSeed> {
        return buildList(count) {
            repeat(count) { index ->
                val seed = (seedOffset + index).toDouble()
                val angleOffset = (index.toDouble() / count * PI * 2 + (ParticleOrbMath.noise(seed, 13.0) - 0.5) * 0.18).toFloat()
                val ring = ParticleOrbMath.noise(seed, 17.0)
                add(
                    ParticleOrbitDotSeed(
                        orbitRadius = (94 + ring * 72).toFloat(),
                        verticalScale = (0.56 + ParticleOrbMath.noise(seed, 31.0) * 0.22).toFloat(),
                        angleOffset = angleOffset,
                        radialPulse = (1.4 + ParticleOrbMath.noise(seed, 37.0) * 4.6).toFloat(),
                        orbitDuration = 17.0 + ParticleOrbMath.noise(seed, 41.0) * 14.0,
                        opacityDuration = 7.5 + ParticleOrbMath.noise(seed, 43.0) * 7.0,
                        scaleDuration = 8.0 + ParticleOrbMath.noise(seed, 47.0) * 6.5,
                        phaseOffset = ParticleOrbMath.noise(seed, 53.0) * 13.0,
                        restOpacity = (0.07 + ParticleOrbMath.noise(seed, 29.0) * 0.15).toFloat(),
                        restScale = (0.34 + ParticleOrbMath.noise(seed, 59.0) * 0.56).toFloat(),
                        scaleRange = (0.035 + ParticleOrbMath.noise(seed, 61.0) * 0.07).toFloat()
                    )
                )
            }
        }
    }

    fun makeCoreBlocks(seedOffset: Int, count: Int, prominence: Double): List<ParticleBlock> {
        val blocks = mutableListOf<ParticleBlock>()
        val maxAttempts = count * 12
        var attempts = 0

        while (blocks.size < count && attempts < maxAttempts) {
            val seed = (seedOffset + attempts).toDouble()
            val x = ParticleOrbMath.noise(seed, 3.0) * 2 - 1
            val y = ParticleOrbMath.noise(seed, 9.0) * 2 - 1
            val density = coreDensity(x, y, seed) * prominence

            if (ParticleOrbMath.noise(seed, 15.0) < density) {
                val energy = (density + ParticleOrbMath.noise(seed, 25.0) * 0.12).coerceIn(0.0, 1.0)
                val glyphIndex = ((energy * GlyphRamp.size).roundToInt() - 1).coerceIn(0, GlyphRamp.size - 1)
                blocks += ParticleBlock(
                    point = androidx.compose.ui.geometry.Offset(
                        x = snap(ParticleOrbMetrics.center.x + x.toFloat() * ParticleOrbMetrics.coreField.width * 0.5f),
                        y = snap(ParticleOrbMetrics.center.y + y.toFloat() * ParticleOrbMetrics.coreField.height * 0.5f)
                    ),
                    glyph = GlyphRamp[glyphIndex],
                    size = (4.1 + energy * 5.8 + ParticleOrbMath.noise(seed, 21.0) * 1.1).toFloat(),
                    opacity = (0.18 + energy * 0.82 + ParticleOrbMath.noise(seed, 33.0) * 0.08).toFloat().coerceAtMost(1f)
                )
            }
            attempts++
        }
        return blocks
    }

    private fun snap(value: Float): Float {
        return (value / ParticleOrbMetrics.SnapGrid).toInt() * ParticleOrbMetrics.SnapGrid
    }

    private fun coreDensity(x: Double, y: Double, seed: Double): Double {
        val radius = sqrt(x * x + y * y)
        val angle = kotlin.math.atan2(y, x)
        val shell = maxOf(0.0, 1 - (radius / 1.05).pow(2)) * 0.36
        val ring = exp(-((radius - 0.56) / 0.24).pow(2)) * 0.34
        val centerMass = ParticleOrbMath.gaussian2D(x + 0.03, y + 0.02, 0.34, 0.30) * 0.38
        val upperLeftMass = ParticleOrbMath.gaussian2D(x + 0.24, y + 0.15, 0.28, 0.18) * 0.28
        val lowerRightMass = ParticleOrbMath.gaussian2D(x - 0.22, y - 0.20, 0.22, 0.20) * 0.24
        val spiral = (0.5 + 0.5 * sin(angle * 3.2 + radius * 8.4)) * 0.16
        val centerCut = ParticleOrbMath.gaussian2D(x - 0.02, y - 0.02, 0.18, 0.16) * 0.20
        val bite = ParticleOrbMath.gaussian2D(x + 0.34, y - 0.23, 0.18, 0.14) * 0.18
        val noise = (ParticleOrbMath.noise(seed, 41.0) - 0.5) * 0.20
        return (shell + ring + centerMass + upperLeftMass + lowerRightMass + spiral - centerCut - bite + noise)
            .coerceIn(0.0, 1.0)
    }
}

private object ParticleOrbMath {
    fun noise(value: Double, seed: Double): Double {
        val mixed = sin(value * 12.9898 + seed * 78.233) * 43_758.5453
        return mixed - kotlin.math.floor(mixed)
    }

    fun gaussian2D(x: Double, y: Double, sigmaX: Double, sigmaY: Double): Double {
        return exp(-0.5 * ((x / sigmaX).pow(2) + (y / sigmaY).pow(2)))
    }
}
