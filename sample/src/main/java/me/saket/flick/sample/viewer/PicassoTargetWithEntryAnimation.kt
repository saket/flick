package me.saket.flick.sample.viewer

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.Exception

class PicassoTargetWithEntryAnimation(private val imageView: ImageView) : Target {

  override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

  override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

  override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
    imageView.alpha = 0f
    imageView.translationY = (bitmap.height / 20).toFloat()
    imageView.rotation = -2F

    imageView.setImageBitmap(bitmap)

    imageView.animate()
        .alpha(1f)
        .translationY(0F)
        .rotation(0F)
        .setInterpolator(FastOutSlowInInterpolator())
        .start()
  }
}
