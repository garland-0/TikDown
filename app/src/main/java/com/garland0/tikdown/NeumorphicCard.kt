package com.garland0.tikdown

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/**
 * A soft-UI ("neumorphic") card background: a flat surface with a light shadow
 * offset toward the top-left and a darker shadow offset toward the bottom-right,
 * giving a subtle raised/embossed look. Meant to be placed as the first child of
 * a FrameLayout, with real content (text, an EditText, etc.) layered on top.
 *
 * Uses a software rendering layer because Paint.setShadowLayer() on arbitrary
 * shapes (not text) only renders correctly off the hardware-accelerated path.
 */
class NeumorphicCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val cornerRadius = 28f * density
    private val margin = 10f * density
    private val blurRadius = 10f * density
    private val shadowOffset = 6f * density

    private val backgroundColor = ContextCompat.getColor(context, R.color.neu_background)
    private val lightColor = ContextCompat.getColor(context, R.color.neu_shadow_light)
    private val darkColor = ContextCompat.getColor(context, R.color.neu_shadow_dark)

    private val darkShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        setShadowLayer(blurRadius, shadowOffset, shadowOffset, darkColor)
    }
    private val lightShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        setShadowLayer(blurRadius, -shadowOffset, -shadowOffset, lightColor)
    }
    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
    }

    private val rect = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rect.set(margin, margin, width - margin, height - margin)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, darkShadowPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, lightShadowPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, surfacePaint)
    }
}
