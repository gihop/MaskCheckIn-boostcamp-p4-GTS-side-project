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
  private val erasePaint: Paint
  private val focusPaint = Paint()

  init {
    textPaint.color = Color.WHITE
    textPaint.textSize = TEXT_SIZE
    labelPaint = Paint()
    labelPaint.color = Color.BLACK
    labelPaint.style = Paint.Style.FILL
    labelPaint.alpha = 200
    erasePaint = Paint()
    erasePaint.color = Color.TRANSPARENT
    erasePaint.style = Paint.Style.FILL
    erasePaint.alpha = 200
    focusPaint.color = Color.GREEN
    focusPaint.strokeWidth = overlay.width / 50f
  }

  private fun changeLabelText(labelText: String) : String {
    if(labelText == "normal") return NOT_MASKED
    else if(labelText == "incorrect_mask") return HALF_MASKED
    else return MASKED
  }

  private fun changeFocusColor(canvas: Canvas, authorized: Boolean){
    val halfStrokeWidth = focusPaint.strokeWidth / 2f
    val padding = overlay.width / 15f
    val x = overlay.width / 2.0f
    val y = overlay.height / 2.0f
    val leftX = padding
    val length = padding * 2
    val rightX = overlay.width - padding
    if(!authorized) focusPaint.color = Color.RED
    canvas?.drawLine(leftX - halfStrokeWidth, y - x + padding, leftX + length - halfStrokeWidth, y - x + padding, focusPaint)
    canvas?.drawLine(leftX, y - x + padding, leftX, y - x + length + padding, focusPaint)
    canvas?.drawLine(leftX - halfStrokeWidth, y + x - padding, leftX + length - halfStrokeWidth, y + x - padding, focusPaint)
    canvas?.drawLine(leftX, y + x - padding, leftX, y + x - length - padding, focusPaint)
    canvas?.drawLine(rightX + halfStrokeWidth, y - x + padding, rightX - length + halfStrokeWidth, y - x + padding, focusPaint)
    canvas?.drawLine(rightX, y - x + padding, rightX, y - x + length + padding, focusPaint)
    canvas?.drawLine(rightX + halfStrokeWidth, y + x - padding, rightX - length + halfStrokeWidth, y + x - padding, focusPaint)
    canvas?.drawLine(rightX, y + x - padding, rightX, y + x - length - padding, focusPaint)
  }

  @Synchronized
  override fun draw(canvas: Canvas) {
    // First try to find maxWidth and totalHeight in order to draw to the center of the screen.
    var maxWidth = 0f
    val totalHeight = TEXT_SIZE * 4
    var incorrectMask = 0F
    var mask = 0F
    var normal = 0F
    for (label in labels) {
      if(label.text == "normal") normal = label.confidence
      else if(label.text == "incorrect_mask") incorrectMask = label.confidence
      else mask = label.confidence

      val lineWidth = textPaint.measureText(changeLabelText(label.text) + " : " +
              String.format(Locale.US, LABEL_FORMAT, label.confidence * 100))
      maxWidth = Math.max(maxWidth, lineWidth)
    }

    val result = if(normal + incorrectMask > mask) WEAR_MASK else AUTHORIZED
    val resultWidth = textPaint.measureText(result)
    maxWidth = Math.max(maxWidth, resultWidth)
    changeFocusColor(canvas, result == AUTHORIZED)

    val x = Math.max(0f, overlay.width / 2.0f - maxWidth / 2.0f)
    var y = Math.max(200f, overlay.height / 2.0f - totalHeight / 2.0f)

    if (!labels.isEmpty()) {
      val padding = 20f
      canvas.drawRect(
        x - padding,
        y - padding,
        x + maxWidth + padding,
        y + totalHeight + padding,
        labelPaint
      )
    }
    canvas.drawText(result, x, y + TEXT_SIZE, textPaint)
    y += TEXT_SIZE

    for (label in labels) {
      if (y + TEXT_SIZE * 2 > overlay.height) {
        break
      }
      canvas.drawText(
              changeLabelText(label.text) + " : " +
                      String.format(Locale.US, LABEL_FORMAT, label.confidence * 100),
                      x, y + TEXT_SIZE, textPaint
      )
      y += TEXT_SIZE
    }
  }

  companion object {
    private const val TEXT_SIZE = 70.0f
    private const val LABEL_FORMAT = "%.2f%%"
    private const val WEAR_MASK = "마스크를 착용해주세요"
    private const val AUTHORIZED = "인증되었습니다"
    private const val MASKED = "마스크 착용"
    private const val NOT_MASKED = "마스크 미착용"
    private const val HALF_MASKED = "코스크 턱스크"
  }
}
