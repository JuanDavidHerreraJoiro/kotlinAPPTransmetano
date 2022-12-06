package com.transmetano.ar.arOffline.arview

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.graphics.Shader.TileMode
import android.util.AttributeSet
import android.view.View
import com.transmetano.ar.R
import com.transmetano.ar.arOffline.arview.ARLabelUtils.adjustLowPassFilterAlphaValue
import com.transmetano.ar.arOffline.arview.ARLabelUtils.getShowUpAnimation
import com.transmetano.ar.arOffline.compass.CompassData
import kotlin.math.min

internal class ARView : View {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, attributeSetId: Int) : super(
        context,
        attrs,
        attributeSetId
    )

    private var rectanglePaint = Paint().apply {
        color = context.getColor(R.color.white)
        style = Paint.Style.FILL
    }

    private val circlePaint = Paint().apply {
        color = context.getColor(R.color.primary)
        strokeWidth = 1f
        style = Paint.Style.FILL_AND_STROKE
    }

    private var arLabels: List<ARLabelProperties>? = null
    private var animators = mutableMapOf<Int, ARLabelAnimationData>()
    private var lowPassFilterAlphaListener: ((Float) -> Unit)? = null

    companion object {
        private const val LABEL_CORNER_RADIUS = 20f
        private const val TEXT_SIZE = 500f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawLines(canvas, arLabels)

        arLabels?.forEach { drawArLabel(canvas, it) }

    }

    private fun drawLines(canvas: Canvas, arPoints: List<ARLabelProperties>?) {
        if (arPoints != null && arPoints.size > 1) {

            var startPoint = arPoints[0]

            for (index in 1 until arPoints.size) {

                val endPoint = arPoints[index]

                val left = startPoint.positionX
                val top = startPoint.positionY - 20f
                val right = endPoint.positionX
                val bottom = endPoint.positionY + 20f

                rectanglePaint.apply {
                    shader = LinearGradient(
                        right / 2, top - 10f, right / 2, arPoints[0].positionY,
                        Color.GRAY, context.getColor(R.color.white), TileMode.MIRROR
                    )
                }

                canvas.drawRoundRect(
                    left, top, right, bottom,
                    LABEL_CORNER_RADIUS,
                    LABEL_CORNER_RADIUS, rectanglePaint
                )

                startPoint = arPoints[index]

            }
        }
    }

    private fun drawArLabel(canvas: Canvas, arLabelProperties: ARLabelProperties) {

        val labelText = "Punto a \n ${arLabelProperties.distance} m"
        val circleSize = (2000 / arLabelProperties.distance).toFloat()

        circlePaint.apply {
            shader = RadialGradient(
                arLabelProperties.positionX, arLabelProperties.positionY,
                if (circleSize < 1) 1f else circleSize,
                context.getColor(R.color.primary),
                context.getColor(R.color.secondary),
                TileMode.MIRROR
            )
        }

        canvas.drawCircle(
            arLabelProperties.positionX,
            arLabelProperties.positionY,
            circleSize, circlePaint
        )

        var newY = arLabelProperties.positionY

        val textPaint = Paint().apply {
            color = context.getColor(R.color.white)
            textSize = TEXT_SIZE / arLabelProperties.distance
            textAlign = Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        for (line in labelText.split("\n")) {
            canvas.drawText(
                line,
                arLabelProperties.positionX,
                newY,
                textPaint
            )
            newY += textPaint.descent() - textPaint.ascent()
        }
    }

    fun setCompassData(compassData: CompassData) {
        val labelsThatShouldBeShown =
            ARLabelUtils.prepareLabelsProperties(compassData, width, height)

        showAnimationIfNeeded(arLabels, labelsThatShouldBeShown)

        arLabels = labelsThatShouldBeShown

        adjustAlphaFilterValue()

        invalidate()
    }

    private fun adjustAlphaFilterValue() {
        arLabels
            ?.find { isInView(it.positionX) }
            ?.let {
                lowPassFilterAlphaListener?.invoke(
                    adjustLowPassFilterAlphaValue(it.positionX, width)
                )
            }
    }

    private fun showAnimationIfNeeded(
        labelsShownBefore: List<ARLabelProperties>?,
        labelsThatShouldBeShown: List<ARLabelProperties>
    ) {
        labelsShownBefore?.let { checkForShowingUpLabels(labelsThatShouldBeShown, it) }
            ?: labelsThatShouldBeShown.forEach {
                animators[it.id] = getShowUpAnimation()
            }
    }

    private fun checkForShowingUpLabels(
        labelsThatShouldBeShown: List<ARLabelProperties>,
        labelsShownBefore: List<ARLabelProperties>
    ) {
        labelsThatShouldBeShown
            .filterNot { newlabel -> labelsShownBefore.any { oldLabel -> newlabel.id == oldLabel.id } }
            .forEach { newLabels ->
                animators[newLabels.id] = getShowUpAnimation()
            }
    }

    private fun isInView(positionX: Float) = positionX > 0 && positionX < width

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        setMeasuredDimension(
            measureDimension(desiredWidth, widthMeasureSpec),
            measureDimension(desiredHeight, heightMeasureSpec)
        )
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = desiredSize
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        return result
    }
}
