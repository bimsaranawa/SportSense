/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.poselandmarker

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.google.mediapipe.examples.poselandmarker.PoseLandmarkerHelper.Companion.TAG
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.max
import kotlin.math.min
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.atan2

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    private var linePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var redLinePaint = Paint()
    private var range = 30


    private var textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.LEFT
    }

    private val allJoints = mutableListOf<Map<String, Any>>()









    init {
        initPaints()
        fetchAndStoreData("Sprint", "Technique1")
        addNewTechnique()
    }

    private fun fetchAndStoreData(sportName: String, techniqueName: String) {
        FirebaseManager.fetchJointsAndAngles(sportName, techniqueName) { joints ->
            allJoints.clear()
            allJoints.addAll(joints.map { joint ->
                mapOf(
                    "joint1" to (joint["joint1"] as Long).toInt(),
                    "joint2" to (joint["joint2"] as Long).toInt(),
                    "joint3" to (joint["joint3"] as Long).toInt(),
                    "expectedAngle" to joint["expectedAngle"] as Long // Convert Long to Double
                )
            })
            invalidate() // Call this to redraw the view with the new data
            Log.d(TAG, "Updated joints data: $allJoints")
        }
    }

    private fun addNewTechnique() {
        val sportName = "Sprint"
        val techniqueName = "Technique1"
        val jointsData = listOf(
            mapOf("joint1" to 24, "joint2" to 26, "joint3" to 28, "expectedAngle" to 90),
            mapOf("joint1" to 28, "joint2" to 26, "joint3" to 24, "expectedAngle" to 90),
            mapOf("joint1" to 23, "joint2" to 25, "joint3" to 27, "expectedAngle" to 120)
            // Add more joint sets as needed
        )
        FirebaseManager.addTechnique(sportName, techniqueName, jointsData)
    }

    fun clear() {
        results = null
        pointPaint.reset()
        linePaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = Color.GREEN
            //ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        redLinePaint.color = Color.RED
        redLinePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        redLinePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL

        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun getLineColor(angle: Long, expectedAngle: Double, range: Int): Int {
        val deviation = kotlin.math.abs(angle - expectedAngle).toFloat()
        val maxDeviation = range.toFloat()
        val fraction = deviation / maxDeviation
        val green = Color.GREEN
        val red = Color.RED

        return ArgbEvaluator().evaluate(fraction, green, red) as Int
    }










    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            for (landmark in poseLandmarkerResult.landmarks()) {
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                for (angleSet in allJoints) {
                    val joint1 = angleSet["joint1"] as Int
                    val joint2 = angleSet["joint2"] as Int
                    val joint3 = angleSet["joint3"] as Int
                    val expectedAngle = angleSet["expectedAngle"] as Long






                    // Calculate angle between landmarks 28, 26, 24




                    val angle = getAngle(landmark, joint1, joint2, joint3)
                    val useRedPaint = (angle < expectedAngle - range || angle >expectedAngle + range)

                    Log.d(TAG, "Angle: $angle")


                    PoseLandmarker.POSE_LANDMARKS.forEach { poseLandmark ->
                        val startX = poseLandmarkerResult.landmarks()[0][poseLandmark!!.start()].x() * imageWidth * scaleFactor
                        val startY = poseLandmarkerResult.landmarks()[0][poseLandmark.start()].y() * imageHeight * scaleFactor
                        val endX = poseLandmarkerResult.landmarks()[0][poseLandmark.end()].x() * imageWidth * scaleFactor
                        val endY = poseLandmarkerResult.landmarks()[0][poseLandmark.end()].y() * imageHeight * scaleFactor

                        Log.d(TAG, "Angle: $poseLandmark.start()")

                        if ((poseLandmark.start() == joint2 && poseLandmark.end() == joint3) ||
                            (poseLandmark.start() == joint1 && poseLandmark.end() == joint2) ||
                            (poseLandmark.start() == joint3 && poseLandmark.end() == joint1)
                        ) {
                            canvas.drawText(String.format("%.1f", angle), startX, startY, textPaint)
                            linePaint.color= getLineColor(expectedAngle, angle, range)
                            canvas.drawLine(startX, startY, endX, endY, if (useRedPaint) redLinePaint else linePaint)


                        } else {
                            linePaint.color=Color.GREEN
                            canvas.drawLine(startX, startY, endX, endY, linePaint)
                    }
                }
            }
        }
    }}

    // Function to calculate the angle between three points
    private fun getAngle(landmarks: MutableList<NormalizedLandmark>, index1: Int, index2: Int, index3: Int): Double {
        if (landmarks.size > index1 && landmarks.size > index2 && landmarks.size > index3) {
            return calculateAngle(
                landmarks[index1].x(), landmarks[index2].x(), landmarks[index3].x(),
                landmarks[index1].y(), landmarks[index2].y(), landmarks[index3].y()
            )
        } else {
            Log.e(PoseLandmarkerHelper.TAG, "Invalid landmark indices provided.")
            return -1.0 // Return -1.0 to indicate an error
        }
    }

    // Function to calculate the angle
    private fun calculateAngle(x1: Float, x2: Float, x3: Float, y1: Float, y2: Float, y3: Float): Double {
        val angle = Math.toDegrees(
            (atan2(y3 - y2, x3 - x2) -
                    atan2(y1 - y2, x1 - x2)).toDouble()
        )

        val absAngle = kotlin.math.abs(angle) % 360
        return if (absAngle > 180) 360 - absAngle else absAngle
    }



    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 12F
    }
}