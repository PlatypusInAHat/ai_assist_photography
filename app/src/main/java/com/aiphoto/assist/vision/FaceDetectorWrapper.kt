package com.aiphoto.assist.vision

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ML Kit face detector wrapper.
 *
 * Detects the **largest** face in frame, returning a normalised bounding box
 * and the head euler-Y angle (yaw), which [LookroomPortraitPreset] uses
 * to decide where to place "look room" space.
 *
 * Uses PERFORMANCE mode (not ACCURATE) to keep latency low (~10-15 ms).
 * Results are returned asynchronously; we use a busy-flag to avoid
 * queueing multiple concurrent detections.
 */
class FaceDetectorWrapper {

    /**
     * Result of a single face-detection pass.
     *
     * @param box      Bounding box normalised to 0..1 (both axes).
     * @param yawDeg   Head euler-Y in degrees: positive = looking right.
     */
    data class FaceResult(
        val box: RectF,
        val yawDeg: Float
    )

    private val detector: FaceDetector

    /** True while an async detection is in flight — prevents queue pile-up. */
    private val busy = AtomicBoolean(false)

    /** Latest result (updated on ML Kit callback thread). */
    @Volatile
    var latestResult: FaceResult? = null
        private set

    /** Frame counter — only process every N-th frame for perf. */
    private var frameCounter = 0
    private val frameSkip = 2   // process every 2nd frame

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setMinFaceSize(0.15f)   // skip tiny distant faces
            .build()
        detector = FaceDetection.getClient(options)
    }

    /**
     * Submit a frame for face detection.
     *
     * This is **non-blocking**: ML Kit processes async, and [latestResult]
     * is updated when ready. Callers should read [latestResult] on the next
     * analysis frame.
     */
    @OptIn(ExperimentalGetImage::class)
    fun detect(imageProxy: ImageProxy) {
        frameCounter++
        if (frameCounter % frameSkip != 0) return
        if (busy.get()) return   // previous frame still in flight

        val mediaImage = imageProxy.image ?: return
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        val imgWidth = inputImage.width.toFloat()
        val imgHeight = inputImage.height.toFloat()

        if (imgWidth <= 0 || imgHeight <= 0) return

        busy.set(true)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                latestResult = pickBestFace(faces, imgWidth, imgHeight)
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

    /**
     * Pick the largest face and normalise its bounding box to 0..1.
     */
    private fun pickBestFace(
        faces: List<Face>,
        imgW: Float,
        imgH: Float
    ): FaceResult? {
        if (faces.isEmpty()) return null

        val best = faces.maxByOrNull {
            it.boundingBox.width() * it.boundingBox.height()
        } ?: return null

        val bb = best.boundingBox
        val normBox = RectF(
            (bb.left / imgW).coerceIn(0f, 1f),
            (bb.top / imgH).coerceIn(0f, 1f),
            (bb.right / imgW).coerceIn(0f, 1f),
            (bb.bottom / imgH).coerceIn(0f, 1f)
        )

        // headEulerAngleY: positive = face turned to their left (camera's right)
        val yaw = best.headEulerAngleY

        return FaceResult(box = normBox, yawDeg = yaw)
    }
}
