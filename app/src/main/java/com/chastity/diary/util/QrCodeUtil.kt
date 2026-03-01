package com.chastity.diary.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Generates QR code bitmaps using the ZXing core library.
 */
object QrCodeUtil {

    /**
     * Encodes [content] into a QR code bitmap.
     *
     * @param content         The string to encode (typically a URL).
     * @param size            Width and height in pixels of the output bitmap.
     * @param foregroundColor ARGB int for the dark modules (default: white for dark card backgrounds).
     * @param backgroundColor ARGB int for the light modules (default: transparent).
     * @param margin          Quiet-zone size in QR modules (0 = tight, 1 = minimal padding).
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 256,
        foregroundColor: Int = Color.WHITE,
        backgroundColor: Int = Color.TRANSPARENT,
        margin: Int = 1,
    ): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to margin)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) foregroundColor else backgroundColor)
            }
        }
        return bitmap
    }
}
