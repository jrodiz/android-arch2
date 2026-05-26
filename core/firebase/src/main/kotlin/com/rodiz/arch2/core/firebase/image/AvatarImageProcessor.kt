package com.rodiz.arch2.core.firebase.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Downscales and re-encodes a picked avatar image before it is uploaded to
 * Firebase Storage. The aspect ratio is **always preserved** — the image is
 * scaled uniformly so its longest edge is at most [MAX_DIMENSION], never
 * cropped — and EXIF orientation is applied so portrait photos don't upload
 * sideways. Output is JPEG at [QUALITY].
 *
 * Both the sign-up ([com.rodiz.arch2.feature.login]) and edit-profile
 * ([com.rodiz.arch2.feature.profile]) upload paths run their picked [Uri]
 * through [process] first, turning a multi-megabyte camera photo into a
 * ~few-hundred-KB blob without visible quality loss.
 */
@Singleton
class AvatarImageProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /**
     * Reads [source], downscales it (ratio preserved) and returns JPEG bytes
     * ready to hand to `StorageReference.putBytes`.
     *
     * @throws IllegalStateException if the source cannot be opened or decoded.
     */
    suspend fun process(source: Uri): ByteArray = withContext(io) {
        val originalBytes = sourceSizeBytes(source)
        val bitmap = decodeScaled(source)
        try {
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
                val result = out.toByteArray()
                val pct = if (originalBytes > 0) result.size * 100 / originalBytes else -1
                Log.i(
                    TAG,
                    "process: original=${originalBytes}B (${originalBytes / 1024}KB) -> " +
                        "processed=${result.size}B (${result.size / 1024}KB) " +
                        "[$pct% of original] outDims=${bitmap.width}x${bitmap.height} q=$QUALITY",
                )
                result
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun sourceSizeBytes(source: Uri): Long {
        context.contentResolver.query(source, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !cursor.isNull(idx)) return cursor.getLong(idx)
                }
            }
        return runCatching {
            context.contentResolver.openInputStream(source)?.use { it.readBytes().size.toLong() } ?: -1L
        }.getOrDefault(-1L)
    }

    private fun decodeScaled(source: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(source)
        } else {
            decodeWithBitmapFactory(source)
        }

    // API 28+: ImageDecoder decodes already-scaled (cheap on memory) and applies
    // EXIF orientation itself, so no manual rotation is needed here.
    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(source: Uri): Bitmap {
        val src = ImageDecoder.createSource(context.contentResolver, source)
        return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            decoder.isMutableRequired = false
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // compress() needs a readable bitmap
            val w = info.size.width
            val h = info.size.height
            val longest = max(w, h)
            if (longest > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / longest
                decoder.setTargetSize((w * scale).roundToInt(), (h * scale).roundToInt())
            }
        }
    }

    // API 26–27 fallback: two-pass BitmapFactory (bounds → inSampleSize) to avoid
    // OOM on huge photos, an exact scale to the target, then EXIF rotation.
    private fun decodeWithBitmapFactory(source: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source).use { stream ->
            checkNotNull(stream) { "Cannot open avatar source: $source" }
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cannot decode avatar source: $source" }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = context.contentResolver.openInputStream(source).use { stream ->
            checkNotNull(stream) { "Cannot open avatar source: $source" }
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
        checkNotNull(decoded) { "Cannot decode avatar source: $source" }

        val scaled = scaleToMaxDimension(decoded)
        return applyExifRotation(source, scaled)
    }

    private fun scaleToMaxDimension(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).roundToInt(),
            (bitmap.height * scale).roundToInt(),
            true,
        )
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private fun applyExifRotation(source: Uri, bitmap: Bitmap): Bitmap {
        val degrees = context.contentResolver.openInputStream(source)?.use { stream ->
            when (
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (degrees == 0f) return bitmap
        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply { postRotate(degrees) },
            true,
        )
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sample = 1
        var longest = max(width, height)
        // Halve until the next halving would drop below the target — keeps the
        // pre-scale bitmap just above MAX_DIMENSION so the exact scale is sharp.
        while (longest / 2 >= MAX_DIMENSION) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private companion object {
        const val TAG = "TinPet.AvatarImageProcessor"
        const val MAX_DIMENSION = 2048
        const val QUALITY = 90
    }
}
