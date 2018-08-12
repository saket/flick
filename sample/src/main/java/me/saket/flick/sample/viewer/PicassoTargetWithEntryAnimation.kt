package me.saket.flick.sample.viewer

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import me.saket.flick.FlickGestureListener
import java.lang.Exception

class PicassoTargetWithEntryAnimation(private val imageView: ImageView) : Target {

  override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

  override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}

  override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
    imageView.apply {
      alpha = 0F
      translationY = bitmap.height / 20F
      rotation = -2F
    }

    imageView.setImageBitmap(bitmap)

    imageView.animate()
        .alpha(1F)
        .translationY(0F)
        .rotation(0F)
        .setInterpolator(FlickGestureListener.ANIM_INTERPOLATOR)
        .start()
  }
}
