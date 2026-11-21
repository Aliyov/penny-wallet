package com.penny.wallet.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.penny.wallet.domain.receipt.OcrLine
import com.penny.wallet.domain.receipt.ReceiptParser
import com.penny.wallet.domain.receipt.ReceiptResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reads a receipt photo on the device (ML Kit, bundled Latin model: no network) and returns the parsed total.
 * Photos taken through [newPhotoUri] live in the app cache only and are deleted by [discardPhoto].
 */
class ReceiptReader(private val context: Context) {
    private val dir get() = File(context.cacheDir, "receipts").apply { mkdirs() }

    /** A fresh file the camera app can write to. */
    fun newPhotoUri(): Uri {
        dir.listFiles()?.forEach { it.delete() } // leftovers from an interrupted scan
        val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.receipts", file)
    }

    /** Deletes our own temporary photos; gallery images picked by the user are never touched. */
    fun discardPhoto() {
        dir.listFiles()?.forEach { it.delete() }
    }

    suspend fun read(uri: Uri): ReceiptResult = ReceiptParser.parse(readLines(uri))

    /** Raw OCR lines with their boxes (also used by the debug-only OCR probe). */
    suspend fun readLines(uri: Uri): List<OcrLine> = withContext(Dispatchers.Default) {
        val image = load(uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val text = recognizer.process(image).await()
            val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                val pts = line.cornerPoints
                val slope = if (pts != null && pts.size >= 2 && pts[1].x != pts[0].x) {
                    (pts[1].y - pts[0].y).toFloat() / (pts[1].x - pts[0].x)
                } else 0f
                OcrLine(line.text, box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat(), slope)
            }
            lines
        } finally {
            recognizer.close()
        }
    }

    /** Decodes upright (EXIF applied) and scaled to at most [MAX_SIDE] px, so 50 MP photos don't exhaust memory. */
    private fun load(uri: Uri): InputImage {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return InputImage.fromFilePath(context, uri)
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val scale = MAX_SIDE.toFloat() / maxOf(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
        }
        val argb = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        return InputImage.fromBitmap(argb, 0)
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }

    private companion object {
        const val MAX_SIDE = 3000
    }
}
