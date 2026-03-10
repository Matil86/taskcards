package de.hipp.app.taskcards.qr

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates QR codes from deep link URLs.
 * Uses ZXing library for QR code encoding.
 */
class QRCodeGenerator {
    companion object {
        private const val TAG = "QRCodeGenerator"
        private const val DEFAULT_SIZE = 512
        private const val MIN_SIZE = 256
        private const val MAX_SIZE = 2048
    }

    /**
     * Generate a QR code bitmap from the given content.
     * @param content The text content to encode (typically a deep link URL)
     * @param size The width and height of the QR code in pixels (default: 512)
     * @return A Bitmap containing the QR code, or null if generation fails
     */
    suspend fun generateQRCode(
        content: String,
        size: Int = DEFAULT_SIZE
    ): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val validSize = size.coerceIn(MIN_SIZE, MAX_SIZE)

            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, validSize, validSize, hints)

            val bitmap = Bitmap.createBitmap(validSize, validSize, Bitmap.Config.RGB_565)

            for (x in 0 until validSize) {
                for (y in 0 until validSize) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                    )
                }
            }

            Log.d(TAG, "QR code generated successfully: ${validSize}x${validSize}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR code", e)
            null
        }
    }

    /**
     * Save a QR code bitmap to the device's gallery.
     * @param context Android context for accessing MediaStore
     * @param bitmap The QR code bitmap to save
     * @param filename The filename (without extension) to save as
     * @return True if save was successful, false otherwise
     */
    suspend fun saveQRCodeToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val displayName = if (filename.endsWith(".png", ignoreCase = true)) {
                filename
            } else {
                "$filename.png"
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TaskCards")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (uri == null) {
                Log.e(TAG, "Failed to create MediaStore entry")
                return@withContext false
            }

            val success = try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error writing QR code to gallery", e)
                false
            }

            // Mark as not pending
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)

            if (success) {
                Log.d(TAG, "QR code saved to gallery: $displayName")
            } else {
                Log.e(TAG, "Failed to save QR code to gallery")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error saving QR code to gallery", e)
            false
        }
    }
}
