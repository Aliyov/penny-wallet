package com.penny.wallet.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.penny.wallet.data.ocr.ReceiptReader
import com.penny.wallet.domain.receipt.ReceiptParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Smoke build only: runs the real OCR + parser (with release R8 shrinking) on every image in
 * `<external files>/probe` and logs lines, rows and `RESULT file=… total=… confident=…`, then `DONE`
 * (tag OcrProbe). Driven by scripts/ocr-smoke.sh.
 */
class OcrProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dir = context.getExternalFilesDir("probe") ?: return
        val verbose = intent.getBooleanExtra("verbose", false)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val images = dir.listFiles { f -> f.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") }.orEmpty().sortedBy { it.name }
                Log.i(TAG, "START ${images.size} images")
                val reader = ReceiptReader(context.applicationContext)
                for (f in images) probe(reader, f, verbose)
            } finally {
                Log.i(TAG, "DONE")
                pending.finish()
            }
        }
    }

    private suspend fun probe(reader: ReceiptReader, f: File, verbose: Boolean) {
        try {
            val t0 = System.currentTimeMillis()
            val lines = reader.readLines(Uri.fromFile(f))
            if (verbose) {
                lines.forEach { Log.i(TAG, "LINE ${f.name} [${it.text}] box=${it.left.toInt()},${it.top.toInt()},${it.right.toInt()},${it.bottom.toInt()} slope=${"%.3f".format(it.slope)}") }
                ReceiptParser.groupRows(lines).forEach { Log.i(TAG, "ROW ${f.name} [$it]") }
            }
            val r = ReceiptParser.parse(lines)
            Log.i(TAG, "RESULT file=${f.name} total=${r.totalCents} confident=${r.confident} candidates=${r.candidates} lines=${lines.size} ms=${System.currentTimeMillis() - t0}")
        } catch (e: Throwable) {
            Log.e(TAG, "RESULT file=${f.name} total=ERROR ${e.javaClass.simpleName}: ${e.message}", e)
        }
    }

    private companion object { const val TAG = "OcrProbe" }
}
