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
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.demo.GraphicOverlay
import com.google.mlkit.vision.demo.GraphicOverlay.Graphic
import com.google.mlkit.vision.demo.preference.PreferenceUtils
import com.google.mlkit.vision.label.ImageLabel
import java.util.*
import kotlin.math.max

/** Graphic instance for rendering a label within an associated graphic overlay view.  */
class LabelGraphic(
  private val overlay: GraphicOverlay,
  private val labels: List<ImageLabel>
) : Graphic(overlay) {
  private val borderPaint = Paint()
  private val backgroundPaint = Paint()
  private val labelPaint =  Paint()
  private val focusPaint = Paint()
  private val dataPaint = Paint()
  private var test = 0
  init {
    borderPaint.color = Color.GRAY
    borderPaint.strokeWidth = 5f
    borderPaint.style = Paint.Style.STROKE
    borderPaint.isAntiAlias = true
    borderPaint.setShadowLayer(20.0f, 0f, 0f, Color.BLACK);
    backgroundPaint.color = Color.parseColor("#2DB400")
    backgroundPaint.textSize = TEXT_SIZE
    backgroundPaint.isAntiAlias = true
    labelPaint.color = Color.WHITE
    labelPaint.style = Paint.Style.FILL
    labelPaint.isAntiAlias = true
    focusPaint.color = Color.parseColor("#2DB400")
    focusPaint.strokeWidth = overlay.width / 50f
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
      backgroundPaint.color = Color.RED
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
    var mask = 0F
    var incorrect_mask = 0F
    var no_mask = 0F

    if(labels[0].text == "background") {
      PreferenceUtils.setInferenceResult(applicationContext, DETECTION_FAILED)
      return
    }

    for (label in labels) {
      when (label.text) {
          "incorrect_mask" -> incorrect_mask = label.confidence
          "mask" -> mask = label.confidence
          "no_mask" -> no_mask = label.confidence
      }
    }

    PreferenceUtils.setInferenceResult(applicationContext, DETECTION_SUCCESS)
//    val result = if(mask < incorrect_mask) WEAR_MASK else AUTHORIZED
    val result = if(no_mask > 0){
      if(no_mask > mask) WEAR_MASK else AUTHORIZED
    } else {
      if(incorrect_mask > mask) WEAR_MASK else AUTHORIZED
    }
    val resultWidth = backgroundPaint.measureText(result)
    maxWidth = max(maxWidth, resultWidth)
    changeFocusColor(canvas, result == AUTHORIZED)

    val x = max(0f, overlay.width / 2.0f - maxWidth / 2.0f)
    var y = max(200f, overlay.height / 2.0f - totalHeight / 2.0f)

    if (labels.isNotEmpty()) {
      val padding = 20f
      canvas.drawRoundRect(
              x - padding,
              y - padding,
              x + maxWidth + padding,
              y + totalHeight + padding,
              20f,
              20f,
              borderPaint
      )
      canvas.drawRoundRect(
        x - padding,
        y - padding,
        x + maxWidth + padding,
        y + totalHeight + padding,
        20f,
        20f,
        labelPaint
      )
    }
    canvas.drawText(result, (overlay.width / 2.0f) - (resultWidth / 2.0f), y + TEXT_SIZE - 10f, backgroundPaint)

    if(!PreferenceUtils.shouldHideDetectionInfo(applicationContext)) {
      val dataX = DATA_TEXT_SIZE * 0.5f;
      val dataY = DATA_TEXT_SIZE * 1.5f;
      for (i in labels.indices) {
        canvas.drawText(
                labels[i].text + " : " +
                        String.format(Locale.US, LABEL_FORMAT, labels[i].confidence * 100),
                dataX, dataY + DATA_TEXT_SIZE * (i + 2), dataPaint
        )
      }
    }
  }

  companion object {
    private const val TEXT_SIZE = 70.0f
    private const val DATA_TEXT_SIZE = 60.0f
    private const val LABEL_FORMAT = "%.2f%%"
    private const val WEAR_MASK = "마스크를 착용해주세요"
    private const val AUTHORIZED = "인증되었습니다"
    private const val DETECTION_SUCCESS = "Detection Success"
    private const val DETECTION_FAILED = "Detection Failed"
  }
}
