/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.kotlin.labeldetector

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.demo.GraphicOverlay
import com.google.mlkit.vision.demo.GraphicOverlay.Graphic
import com.google.mlkit.vision.label.ImageLabel
import java.util.*

/** Graphic instance for rendering a label within an associated graphic overlay view.  */
class LabelGraphic(
  private val overlay: GraphicOverlay,
  private val labels: List<ImageLabel>
) : Graphic(overlay) {
  private val textPaint: Paint = Paint()
  private val labelPaint: Paint
  private val focusPaint = Paint()
  private val dataPaint: Paint

  init {
    textPaint.color = Color.parseColor("#2DB400")
    textPaint.textSize = TEXT_SIZE
    textPaint.isAntiAlias = true
    labelPaint = Paint()
    labelPaint.color = Color.WHITE
    labelPaint.style = Paint.Style.FILL
    labelPaint.isAntiAlias = true
    focusPaint.color = Color.parseColor("#2DB400")
    focusPaint.strokeWidth = overlay.width / 50f
    dataPaint = Paint()
    dataPaint.color = Color.WHITE
    dataPaint.textSize = DATA_TEXT_SIZE
    dataPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);
  }

  private fun changeFocusColor(canvas: Canvas, authorized: Boolean){
    val halfStrokeWidth = focusPaint.strokeWidth / 2f
    val padding = overlay.width / 15f
    val x = overlay.width / 2.0f
    val y = overlay.height / 2.0f
    val leftX = padding
    val length = padding * 2
    val rightX = overlay.width - padding
    if(!authorized) {
      focusPaint.color = Color.RED
      textPaint.color = Color.RED
    }
    canvas?.drawLine(leftX - halfStrokeWidth, y - x + padding, leftX + length - halfStrokeWidth, y - x + padding, focusPaint)
    canvas?.drawLine(leftX, y - x + padding, leftX, y - x + length + padding, focusPaint)
    canvas?.drawLine(leftX - halfStrokeWidth, y + x - padding, leftX + length - halfStrokeWidth, y + x - padding, focusPaint)
    canvas?.drawLine(leftX, y + x - padding, leftX, y + x - length - padding, focusPaint)
    canvas?.drawLine(rightX + halfStrokeWidth, y - x + padding, rightX - length + halfStrokeWidth, y - x + padding, focusPaint)
    canvas?.drawLine(rightX, y - x + padding, rightX, y - x + length + padding, focusPaint)
    canvas?.drawLine(rightX + halfStrokeWidth, y + x - padding, rightX - length + halfStrokeWidth, y + x - padding, focusPaint)
    canvas?.drawLine(rightX, y + x - padding, rightX, y + x - length - padding, focusPaint)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Synchronized
  override fun draw(canvas: Canvas) {
    // First try to find maxWidth and totalHeight in order to draw to the center of the screen.
    var maxWidth = 0f
    val totalHeight = TEXT_SIZE
    var incorrectMask = 0F
    var mask = 0F
    var normal = 0F
    for (label in labels) {
      if(label.text == "normal") normal = label.confidence
      else if(label.text == "incorrect_mask") incorrectMask = label.confidence
      else mask = label.confidence
    }

    val result = if(normal + incorrectMask > mask) WEAR_MASK else AUTHORIZED
    val resultWidth = textPaint.measureText(result)
    maxWidth = Math.max(maxWidth, resultWidth)
    changeFocusColor(canvas, result == AUTHORIZED)

    val x = Math.max(0f, overlay.width / 2.0f - maxWidth / 2.0f)
    var y = Math.max(200f, overlay.height / 2.0f - totalHeight / 2.0f)

    if (!labels.isEmpty()) {
      val padding = 20f
      canvas.drawRoundRect(
        x - padding,
        y - padding,
        x + maxWidth + padding,
        y + totalHeight + padding,
        30f,
        30f,
        labelPaint
      )
    }
    canvas.drawText(result, (overlay.width / 2.0f) - (resultWidth / 2.0f), y + TEXT_SIZE - 10f, textPaint)

    val dataX = DATA_TEXT_SIZE * 0.5f;
    val dataY = DATA_TEXT_SIZE * 1.5f;
    for (i in 0..2) {
      canvas.drawText(
              labels[i].text + " : " +
                      String.format(Locale.US, LABEL_FORMAT, labels[i].confidence * 100),
              dataX, dataY + DATA_TEXT_SIZE*(i+2), dataPaint
      )
    }
  }

  companion object {
    private const val TEXT_SIZE = 70.0f
    private const val DATA_TEXT_SIZE = 60.0f
    private const val LABEL_FORMAT = "%.2f%%"
    private const val WEAR_MASK = "마스크를 착용해주세요"
    private const val AUTHORIZED = "인증되었습니다"
  }
}
