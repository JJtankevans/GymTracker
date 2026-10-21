package com.vitoroliveira.gymtracker.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import com.vitoroliveira.gymtracker.domain.AvatarRules
import java.io.ByteArrayOutputStream
import kotlin.math.min

/** Prepara a foto escolhida na galeria: corrige a rotação (EXIF), recorta o centro em quadrado, reduz e comprime em JPEG. */
object AvatarImageProcessor {

    /** Devolve o JPEG pronto para enviar ou null se a imagem não puder ser lida. Roda fora da thread principal. */
    fun toAvatarJpeg(context: Context, uri: Uri): ByteArray? {
        return try {
            val resolver = context.contentResolver
            val size = AvatarRules.SIZE_PX

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            // decodifica já reduzido (potência de 2) para não estourar memória com foto de 12 MP
            var sample = 1
            while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= size) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return null

            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val upright = rotate(decoded, orientation)

            // recorta o centro em quadrado e desenha sobre fundo branco (PNG transparente não vira preto no JPEG)
            val side = min(upright.width, upright.height)
            val left = (upright.width - side) / 2
            val top = (upright.height - side) / 2
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            Canvas(out).apply {
                drawColor(Color.WHITE)
                drawBitmap(upright, Rect(left, top, left + side, top + side), Rect(0, 0, size, size), Paint(Paint.FILTER_BITMAP_FLAG))
            }
            val bytes = ByteArrayOutputStream().also { out.compress(Bitmap.CompressFormat.JPEG, AvatarRules.JPEG_QUALITY, it) }.toByteArray()
            if (upright !== decoded) upright.recycle()
            decoded.recycle()
            out.recycle()
            bytes
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun rotate(bitmap: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }
}
