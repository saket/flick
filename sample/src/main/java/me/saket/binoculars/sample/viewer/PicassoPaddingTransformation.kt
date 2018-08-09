package me.saket.binoculars.sample.viewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.support.annotation.ColorInt
import com.squareup.picasso.Transformation

/** Adds a solid padding around an image. */
class PicassoPaddingTransformation(
    private val paddingPx: Float,
    @ColorInt private val paddingColor: Int
) : Transformation {

  override fun key() = "padding_$paddingPx"

  override fun transform(source: Bitmap): Bitmap {
    if (paddingPx == 0F) {
      return source
    }

    val targetWidth = source.width + paddingPx * 2F
    val targetHeight = source.height + paddingPx * 2F

    // It would have been nice if Picasso offered a Bitmap pool, just like Glide.
    val bitmapWithPadding = Bitmap.createBitmap(targetWidth.toInt(), targetHeight.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmapWithPadding)

    val paint = Paint()
    paint.color = paddingColor
    canvas.drawRect(0F, 0F, targetWidth, targetHeight, paint)
    canvas.drawBitmap(source, paddingPx, paddingPx, null)

    source.recycle()
    return bitmapWithPadding
  }
}
