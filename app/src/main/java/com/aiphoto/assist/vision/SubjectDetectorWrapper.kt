package com.aiphoto.assist.vision

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ML Kit object detector wrapper.
 *
 * Detects the **most prominent** object in the scene, returning a normalised
 * bounding box. Used as a **fallback** when [FaceDetectorWrapper] doesn't
 * find a face — so Thirds / Phi / Center / Diagonal presets still get a
 * meaningful `subjectBox`.
 *
 * Runs in SINGLE_IMAGE_MODE with stream classification disabled for speed.
 */
class SubjectDetectorWrapper {

    data class SubjectResult(
        val box: RectF    // normalised 0..1
    )

    private val detector: ObjectDetector

    private val busy = AtomicBoolean(false)

    @Volatile
    var latestResult: SubjectResult? = null
        private set

    /** Only process every N-th frame. */
    private var frameCounter = 0
    private val frameSkip = 3

    init {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()      // detect several, pick largest
            .enableClassification()        // not strictly needed, but helps ranking
            .build()
        detector = ObjectDetection.getClient(options)
    }

    /**
     * Submit frame for object detection.
     * Non-blocking — updates [latestResult] asynchronously.
     */
    @OptIn(ExperimentalGetImage::class)
    fun detect(imageProxy: ImageProxy) {
        frameCounter++
        if (frameCounter % frameSkip != 0) return
        if (busy.get()) return

        val mediaImage = imageProxy.image ?: return
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        val imgW = inputImage.width.toFloat()
        val imgH = inputImage.height.toFloat()

        if (imgW <= 0 || imgH <= 0) return

        busy.set(true)
        detector.process(inputImage)
            .addOnSuccessListener { objects ->
                latestResult = pickLargest(objects, imgW, imgH)
                busy.set(false)
            }
            .addOnFailureListener {
                busy.set(false)
            }
    }

    fun close() {
        detector.close()
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun pickLargest(
        objects: List<DetectedObject>,
        imgW: Float,
        imgH: Float
    ): SubjectResult? {
        if (objects.isEmpty()) return null

        val best = objects.maxByOrNull {
            it.boundingBox.width() * it.boundingBox.height()
        } ?: return null

        val bb = best.boundingBox

        // Skip objects that fill > 80% of frame — they're likely background
        val areaRatio = (bb.width() * bb.height()) / (imgW * imgH)
        if (areaRatio > 0.8f) return null

        val normBox = RectF(
            (bb.left / imgW).coerceIn(0f, 1f),
            (bb.top / imgH).coerceIn(0f, 1f),
            (bb.right / imgW).coerceIn(0f, 1f),
            (bb.bottom / imgH).coerceIn(0f, 1f)
        )
        return SubjectResult(box = normBox)
    }
}
