package me.saket.binoculars.sample.viewer

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.Exception

class PicassoTargetWithProgress(
    private val delegate: Target,
    private val progressView: View
) : Target {

  override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
    delegate.onPrepareLoad(placeHolderDrawable)
    progressView.visibility = View.VISIBLE
  }

  override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
    delegate.onBitmapFailed(e, errorDrawable)
    progressView.visibility = View.GONE
  }

  override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
    delegate.onBitmapLoaded(bitmap, from)
    progressView.visibility = View.GONE
  }
}
