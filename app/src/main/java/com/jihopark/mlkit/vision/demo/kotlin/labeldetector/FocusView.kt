package com.jihopark.mlkit.vision.demo.kotlin.labeldetector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class FocusView @JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr){
    override fun onDraw(canvas: Canvas?) {
        val focusPaint = Paint()
        focusPaint.color = Color.GRAY
        focusPaint.strokeWidth = this.width / 50f
        val halfStrokeWidth = focusPaint.strokeWidth / 2f
        val padding = this.width / 15f
        val x = this.width / 2.0f
        val y = this.height / 2.0f
        val leftX = padding
        val length = padding * 2
        val rightX = this.width - padding
        canvas?.drawLine(leftX - halfStrokeWidth, y - x + padding, leftX + length - halfStrokeWidth, y - x + padding, focusPaint)
        canvas?.drawLine(leftX, y - x + padding, leftX, y - x + length + padding, focusPaint)
        canvas?.drawLine(leftX - halfStrokeWidth, y + x - padding, leftX + length - halfStrokeWidth, y + x - padding, focusPaint)
        canvas?.drawLine(leftX, y + x - padding, leftX, y + x - length - padding, focusPaint)
        canvas?.drawLine(rightX + halfStrokeWidth, y - x + padding, rightX - length + halfStrokeWidth, y - x + padding, focusPaint)
        canvas?.drawLine(rightX, y - x + padding, rightX, y - x + length + padding, focusPaint)
        canvas?.drawLine(rightX + halfStrokeWidth, y + x - padding, rightX - length + halfStrokeWidth, y + x - padding, focusPaint)
        canvas?.drawLine(rightX, y + x - padding, rightX, y + x - length - padding, focusPaint)
    }
}