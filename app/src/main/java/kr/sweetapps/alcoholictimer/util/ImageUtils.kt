package kr.sweetapps.alcoholictimer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Phase 3: 이미지 최적화 유틸리티
 * 비용 절감을 위해 이미지를 1024px로 리사이징하고 JPEG 80% 품질로 압축
 */
object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val MAX_WIDTH = 1024
    private const val MAX_HEIGHT = 1024
    private const val JPEG_QUALITY = 80

    /**
     * 이미지를 최적화하여 압축된 파일로 저장
     *
     * @param context Context
     * @param imageUri 원본 이미지 URI
     * @return 압축된 이미지 파일 (임시 디렉토리)
     */
    fun compressImage(context: Context, imageUri: Uri): File? {
        return try {
            // 1. URI에서 Bitmap 로드
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode image from URI: $imageUri")
                return null
            }

            // 2. EXIF 방향 정보 읽기 (카메라 회전 보정)
            val rotatedBitmap = rotateImageIfNeeded(context, imageUri, originalBitmap)

            // 3. 리사이징 (가로 1024px 기준)
            val resizedBitmap = resizeBitmap(rotatedBitmap, MAX_WIDTH, MAX_HEIGHT)

            // 4. JPEG 80% 품질로 압축
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val compressedBytes = outputStream.toByteArray()

            // 5. 임시 파일로 저장
            val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val fileOutputStream = FileOutputStream(tempFile)
            fileOutputStream.write(compressedBytes)
            fileOutputStream.close()

            // 메모리 해제
            originalBitmap.recycle()
            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle()
            }
            if (resizedBitmap != rotatedBitmap) {
                resizedBitmap.recycle()
            }

            Log.d(TAG, "Image compressed: ${tempFile.length() / 1024}KB")
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            null
        }
    }

    /**
     * EXIF 방향 정보에 따라 이미지 회전
     */
    private fun rotateImageIfNeeded(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = ExifInterface(inputStream!!)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating image", e)
            bitmap
        }
    }

    /**
     * Bitmap 리사이징 (비율 유지)
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (aspectRatio > 1) {
            // 가로가 더 긴 경우
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            // 세로가 더 긴 경우
            newWidth = (maxHeight * aspectRatio).toInt()
            newHeight = maxHeight
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 압축된 이미지의 크기를 추정 (KB 단위)
     */
    fun estimateCompressedSize(context: Context, imageUri: Uri): Long {
        val compressedFile = compressImage(context, imageUri)
        val size = compressedFile?.length() ?: 0L
        compressedFile?.delete()
        return size / 1024 // KB
    }
}

