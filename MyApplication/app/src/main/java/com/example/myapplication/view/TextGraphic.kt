// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.example.myapplication.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.google.firebase.ml.vision.text.FirebaseVisionText

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
class TextGraphic(overlay: GraphicOverlay, private val element: FirebaseVisionText.Element?) : GraphicOverlay.Companion.Graphic(overlay) {

    companion object {

        private val TAG = TextGraphic::class.java.simpleName
    }

    private val rectPaint: Paint = Paint().apply {
        color = RECT_COLOR
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
    }
    private val textPaint: Paint = Paint().apply {
        color = TEXT_COLOR
        textSize = TEXT_SIZE
    }

    init {
        // Redraw the overlay, as this graphic has been added.
        postInvalidate()
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        Log.d(TAG, "on draw text graphic")
        if (element == null) {
            throw IllegalStateException("Attempting to draw a null text.")
        }

        // Draws the bounding box around the TextBlock.
        val rect = RectF(element.boundingBox)
        canvas.drawRect(rect, rectPaint)

        val n = element.text.length
        val fontSize = Math.min(rect.width() / n, rect.height()) * 1.8f

        // Renders the text at the bottom of the box.
        textPaint.textSize = fontSize
        canvas.drawText(element.text, rect.left, rect.bottom, textPaint)
    }
}