package kr.sweetapps.alcoholictimer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * 이미지 압축 유틸리티
 * 원본 이미지를 1080px 최대 크기, 70% 퀄리티로 압축하여 용량 절감
 * (2025-12-19)
 */
object ImageUtils {
    /**
     * 이미지를 압축하여 ByteArray로 반환
     *
     * @param context Context
     * @param imageUri 원본 이미지 URI
     * @return 압축된 이미지 ByteArray (실패 시 null)
     */
    fun compressImage(context: Context, imageUri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // 1. 리사이징 (가로 너비 최대 1080px)
            val maxDimension = 1080
            val ratio = kotlin.math.min(
                maxDimension.toDouble() / originalBitmap.width,
                maxDimension.toDouble() / originalBitmap.height
            )
            val width = (originalBitmap.width * ratio).toInt()
            val height = (originalBitmap.height * ratio).toInt()

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)

            // 2. 압축 (JPEG, 퀄리티 70%)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

