package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.view.GraphicOverlay
import com.example.myapplication.view.TextGraphic
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText

// This is an arbitrary number we are using to keep tab of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts
private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var textureView: TextureView
    private lateinit var graphicOverlay: GraphicOverlay
    private var textureSize = Size(0, 0)
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            if (ableRunning) {
                analyzeView()
            }
            handler.postDelayed(this, 1000L)
        }
    }
    private var ableRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "[onCreate] savedInstanceState=$savedInstanceState")
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.view_finder)
        graphicOverlay = findViewById(R.id.graphic_overlay)

        // Request camera permissions
        if (allPermissionsGranted()) {
            textureView.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time the provided texture view changes, recompute layout
        textureView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTextureView()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[onDestroy]")

        ableRunning = true
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "[onPause]")

        ableRunning = false
    }

    override fun onDestroy() {
        Log.d(TAG, "[onDestroy]")

        handler.removeCallbacks(runnable)

        super.onDestroy()
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d(TAG, "[onRequestPermissionsResult] requestCode=$requestCode, permissions=$permissions, grantResults=$grantResults")

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                textureView.post { startCamera() }
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted(): Boolean {
        Log.d(TAG, "[allPermissionsGranted]")

        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun startCamera() {
        Log.d(TAG, "[startCamera]")

        val targetResolution = Size(600, 800)

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder()
            .setTargetResolution(targetResolution)
            .build()

        // Build the viewfinder use case
        val preview = Preview(previewConfig)
        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            (textureView.parent as ViewGroup).apply {
                removeView(textureView)
                addView(textureView, 0)
            }

            textureSize = it.textureSize

            textureView.surfaceTexture = it.surfaceTexture
            updateTextureView()
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview)
        handler.post(runnable)
    }

    /**
     * 画面の回転にあわせて、TextureViewを修正
     */
    private fun updateTextureView() {
        Log.d(TAG, "[updateTextureView]")

        val matrix = Matrix().apply {
            val displayDegree = when(textureView.display.rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> 0
            }

            val oddRotate = (Math.abs(displayDegree / 90) % 2 == 0)
            val w = (if (oddRotate) textureSize.height else textureSize.width).toFloat()
            val h = (if (oddRotate) textureSize.width else textureSize.height).toFloat()

            val sx = textureView.width.toFloat() / w
            val sy = textureView.height.toFloat() / h

            postScale(1f / textureView.width, 1f / textureView.height)
            postTranslate(-0.5f, -0.5f)
            postRotate(-displayDegree.toFloat())
            postScale(w, h)
            postScale(sx, sy)
            postTranslate(textureView.width / 2f, textureView.height / 2f)
        }

        // Finally, apply transformations to our TextureView
        textureView.setTransform(matrix)
    }

    /**
     * テキスト認識結果を画面に表示する
     *
     * @param result テキスト認識結果
     */
    private fun processTextRecognitionResult(result: FirebaseVisionText) {
        Log.d(TAG, "[processTextRecognitionResult] result.text=${result.text}")

        val blocks = result.textBlocks
        if (blocks.isNotEmpty()) {
            graphicOverlay.clear()
            for (block in blocks) {
                val lines = block.lines
                for (line in lines) {
                    val elements = line.elements
                    for (element in elements) {
                        val textGraphic = TextGraphic(graphicOverlay, element)
                        graphicOverlay.add(textGraphic)
                    }
                }
            }
        }
    }

    private fun analyzeView() {
        Log.d(TAG, "[analyzeView]")

        if (textureView.isAvailable) {
            val original = textureView.bitmap

            val matrix = Matrix().apply {
                val displayDegree = when(textureView.display.rotation) {
                    Surface.ROTATION_0 -> 0
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> 0
                }

                postScale(1f / original.width, 1f / original.height)
                postTranslate(-0.5f, -0.5f)
                postRotate(-displayDegree.toFloat())
                postScale(textureView.width.toFloat(), textureView.height.toFloat())
            }
            val bitmap = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)

            // TODO: ここから分岐
            val visionImage = FirebaseVisionImage.fromBitmap(bitmap)

            val textRecognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
            textRecognizer.processImage(visionImage)
                .addOnSuccessListener { texts -> processTextRecognitionResult(texts) }
                .addOnFailureListener { e -> e.printStackTrace() }
        }
    }
}
