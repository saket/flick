package me.saket.flick.sample.viewer

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.FloatRange
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import com.squareup.picasso.Picasso
import kotterknife.bindView
import me.saket.flick.ContentHeightProvider
import me.saket.flick.FlickDismissLayout
import me.saket.flick.FlickGestureListener
import me.saket.flick.GestureCallbacks
import me.saket.flick.OnGestureInterceptor
import me.saket.flick.sample.R
import me.saket.flick.sample.UnsplashPhoto
import me.saket.flick.sample.viewer.immersive.SystemUiHelper

// TODO: Reduce configuration for flick-dismiss-layout
class ImageViewerActivity : AppCompatActivity() {

  companion object {
    fun intent(context: Context, photo: UnsplashPhoto): Intent {
      return Intent(context, ImageViewerActivity::class.java).putExtra("photo", photo)
    }

    fun unsplashPhoto(intent: Intent): UnsplashPhoto {
      return intent.getParcelableExtra("photo")
    }
  }

  private val rootLayout by bindView<ViewGroup>(R.id.imageviewer_root)
  private val imageView by bindView<ZoomableGestureImageView>(R.id.imageviewer_image)
  private val flickDismissLayout by bindView<FlickDismissLayout>(R.id.imageviewer_image_container)
  private val progressView by bindView<View>(R.id.imageviewer_progress)

  private lateinit var systemUiHelper: SystemUiHelper
  private lateinit var activityBackgroundDrawable: Drawable

  override fun onCreate(savedInstanceState: Bundle?) {
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    super.onCreate(savedInstanceState)
    overridePendingTransition(0, 0)
    setContentView(R.layout.activity_image_viewer)

    animateDimmingOnEntry()
    loadImage()

    flickDismissLayout.flickGestureListener = flickGestureListener()

    systemUiHelper = SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, 0, null)
    imageView.setOnClickListener { systemUiHelper.toggle() }
  }

  override fun finish() {
    super.finish()
    overridePendingTransition(0, 0)
  }

  override fun onBackPressed() {
    animateExit {
      super.onBackPressed()
    }
  }

  private fun finishInMillis(millis: Long) {
    rootLayout.postDelayed({ finish() }, millis)
  }

  private fun loadImage() {
    val photo = unsplashPhoto(intent)
    val displayWidth = resources.displayMetrics.widthPixels

    val target = PicassoTargetWithEntryAnimation(imageView)
    val targetWithProgress = PicassoTargetWithProgress(target, progressView)

    // Adding a 1px transparent border improves anti-aliasing
    // when the image rotates while being dragged.
    val paddingTransformation = PicassoPaddingTransformation(
        paddingPx = 1F,
        paddingColor = Color.TRANSPARENT)

    Picasso.get()
        .load(photo.url(width = displayWidth))
        .transform(paddingTransformation)
        .priority(Picasso.Priority.HIGH)
        .into(targetWithProgress)

    // Picasso keeps a weak reference to targets. Avoid getting them GCed.
    imageView.setTag(R.id.picasso_target, targetWithProgress)
  }

  private fun flickGestureListener(): FlickGestureListener {
    return FlickGestureListener(ViewConfiguration.get(this)).apply {
      onGestureInterceptor = object : OnGestureInterceptor {
        override fun shouldIntercept(deltaY: Float): Boolean {
          // Don't listen for flick gestures if the image can pan further.
          val isScrollingUpwards = deltaY < 0
          val directionInt = if (isScrollingUpwards) -1 else +1
          return imageView.canScrollVertically(directionInt)
        }
      }

      gestureCallbacks = object : GestureCallbacks {
        override fun onFlickDismissed(flickAnimationDuration: Long) {
          finishInMillis(flickAnimationDuration)
        }

        override fun onMove(moveRatio: Float) {
          updateBackgroundDimmingAlpha(Math.abs(moveRatio))
        }
      }

      contentHeightProvider = object : ContentHeightProvider {
        override val heightForDismissAnimation: Int
          get() = imageView.zoomedImageHeight.toInt()

        // A positive height value is important so that the user
        // can dismiss even while the progress indicator is visible.
        override val heightForCalculatingDismissThreshold: Int
          get() = when {
            imageView.drawable == null -> resources.getDimensionPixelSize(R.dimen.mediaalbumviewer_image_height_when_empty)
            else -> imageView.visibleZoomedImageHeight.toInt()
          }
      }
    }
  }

  private fun animateDimmingOnEntry() {
    activityBackgroundDrawable = rootLayout.background.mutate()
    rootLayout.background = activityBackgroundDrawable

    ObjectAnimator.ofFloat(1F, 0f).apply {
      duration = 200
      interpolator = FastOutSlowInInterpolator()
      addUpdateListener { animation ->
        updateBackgroundDimmingAlpha(animation.animatedValue as Float)
      }
      start()
    }
  }

  private fun animateExit(onEndAction: () -> Unit) {
    val animDuration: Long = 200
    flickDismissLayout.animate()
        .alpha(0f)
        .translationY(flickDismissLayout.height / 20F)
        .rotation(-2F)
        .setDuration(animDuration)
        .setInterpolator(FastOutSlowInInterpolator())
        .withEndAction(onEndAction)
        .start()

    ObjectAnimator.ofFloat(0F, 1F).apply {
      duration = animDuration
      interpolator = FastOutSlowInInterpolator()
      addUpdateListener { animation ->
        updateBackgroundDimmingAlpha(animation.animatedValue as Float)
      }
      start()
    }
  }

  private fun updateBackgroundDimmingAlpha(@FloatRange(from = 0.0, to = 1.0) transparencyFactor: Float) {
    // Increase dimming exponentially so that the background is
    // fully transparent while the image has been moved by half.
    val dimming = 1f - Math.min(1f, transparencyFactor * 2)
    activityBackgroundDrawable.alpha = (dimming * 255).toInt()
  }
}
