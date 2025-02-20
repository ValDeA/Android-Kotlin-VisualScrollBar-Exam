package com.valdea.assistantwriter.minimapscrollview

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

class MinimapScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private var isMinimapMode = false
    private var minimapPaint = Paint()
    private var minimapTextPaint = Paint()
    private var contentText = ""

    private var touchIndicatorPaint = Paint()
    private var touchY = 0f

    private var scrollProgress = 0f
    private var contentHeight = 0
    private var viewportHeight = 0
    private var scrollBarPaint = Paint()
    private var scrollThumbPaint = Paint()

    private var currentWidth = 6.dpToPx()
    private val normalWidth = 6.dpToPx()
    private val minimapWidth = 100.dpToPx()

    private var onScrollListener: ((Float) -> Unit)? = null
    private var onScrollProgressListener: ((Float) -> Unit)? = null

    private var lastTouchY = 0f
    private var longPressRunnable: Runnable? = null

    private val widthAnimator = ValueAnimator.ofFloat(normalWidth.toFloat(), minimapWidth.toFloat())

    init {
        minimapPaint.color = Color.WHITE
        minimapTextPaint.color = Color.BLACK
        minimapTextPaint.textSize = 16f // 미니맵 텍스트 크기

        touchIndicatorPaint.color = Color.RED
        touchIndicatorPaint.style = Paint.Style.STROKE
        touchIndicatorPaint.strokeWidth = 2f

        scrollBarPaint.color = Color.LTGRAY
        scrollThumbPaint.color = Color.GRAY

        setupTouchListener()
        setupWidthAnimator()
    }

    private fun setupTouchListener() {
        setOnTouchListener(object : OnTouchListener {
            override fun onTouch(p0: View?, event: MotionEvent?): Boolean {
                if (event != null) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            postDelayed({
                                if (event.downTime == event.eventTime) {
                                    lastTouchY = event.y
                                    switchToMinimapMode()
                                    updateScrollPosition(lastTouchY)
                                }
                            }, ViewConfiguration.getLongPressTimeout().toLong())
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if(isMinimapMode) {
                                touchY = event.y
                                updateScrollPosition(touchY)
                                invalidate()
                            } else {
                                if(abs(event.y - lastTouchY) >
                                    ViewConfiguration.get(context).scaledTouchSlop) {
                                    longPressRunnable?.let { removeCallbacks(it) }
                                }
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            longPressRunnable?.let { removeCallbacks(it) }
                            if(isMinimapMode) {
                                switchToNomrmalMode()
                            }
                        }
                    }
                    return true
                } else
                    return false
            }
        })
    }
    fun setOnScrollListener(listener: (Float) -> Unit) {
        onScrollListener = listener
    }

    private fun updateScrollPosition(y: Float) {
        val lines = contentText.split("\n")
        val totalContentHeight = lines.size * (minimapTextPaint.textSize + 2)
        val scale = height.toFloat() / totalContentHeight

        val actualY = y / scale
        val newProgress = (actualY / totalContentHeight).coerceIn(0f, 1f)

//        setScrollInfo(newProgress, contentHeight, viewportHeight, contentText)
        onScrollListener?.invoke(newProgress)
        onScrollProgressListener?.invoke(newProgress)
    }
    fun setOnScrollProgressListener(listener: (Float) -> Unit) {
        onScrollProgressListener = listener
    }

    private fun setupWidthAnimator() {
        widthAnimator.duration = 300
        widthAnimator.addUpdateListener { animation ->
            currentWidth = (animation.animatedValue as Float).toInt()
            requestLayout()
            invalidate()
        }
    }

    private fun switchToMinimapMode() {
        isMinimapMode = true
        widthAnimator.setFloatValues(currentWidth.toFloat(), minimapWidth.toFloat())
        widthAnimator.start()
    }

    private fun switchToNomrmalMode() {
        isMinimapMode = false
        widthAnimator.setFloatValues(currentWidth.toFloat(), normalWidth.toFloat())
        widthAnimator.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(currentWidth, MeasureSpec.getSize(heightMeasureSpec))
    }

    fun setScrollInfo(progress: Float, contentHeight: Int, viewportHeight: Int, text: String) {
        this.scrollProgress = progress.coerceIn(0f, 1f)
        this.contentHeight = contentHeight
        this.viewportHeight = viewportHeight
        this.contentText = text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (contentHeight <= viewportHeight) return
        if(isMinimapMode) {
            // 미니맵 그리기 로직
            drawMinimap(canvas)
        } else {
            // 일반 스크롤 그리기 로직
            drawScrollbar(canvas)
        }
    }

    private fun drawMinimap(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), minimapPaint)

        val lines = contentText.split("\n")
        val lineHeight = minimapTextPaint.textSize + 2 // 줄 간격 추가
        val totalContentHeight = lines.size * lineHeight
        val scale = height.toFloat() / totalContentHeight

        // 텍스트 그리기
        var y = 0f
        for (line in lines) {
            canvas.drawText(line, 0f, y * scale, minimapTextPaint)
            y += lineHeight
        }

        // 뷰포트 Thumb 그리기
        val thumbHeight = (viewportHeight.toFloat() / contentHeight) * height
        val thumbTop = scrollProgress * (height - thumbHeight)
        val viewportRect = RectF(0f, thumbTop, width.toFloat(), thumbTop + thumbHeight)
        scrollThumbPaint.alpha = 100
        canvas.drawRect(viewportRect, scrollThumbPaint)
    }

    private fun drawScrollbar(canvas: Canvas) {
        // 스크롤 배경 그리기
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrollBarPaint)

        // 스크롤 썸 그리기
        val thumbHeight = (viewportHeight.toFloat() / contentHeight) * height
        val scrollableHeight = height - thumbHeight
        val thumbTop = scrollProgress * scrollableHeight
        canvas.drawRect(0f, thumbTop, width.toFloat(), thumbTop + thumbHeight, scrollThumbPaint)
    }


    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
}