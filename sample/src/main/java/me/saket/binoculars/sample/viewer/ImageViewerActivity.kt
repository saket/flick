package me.saket.binoculars.sample.viewer

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.FloatRange
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import kotterknife.bindView
import me.saket.binoculars.FlickDismissLayout
import me.saket.binoculars.FlickGestureListener
import me.saket.binoculars.sample.R
import me.saket.binoculars.sample.UnsplashPhoto
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

// TODO: Reduce configuration for flick-dismiss-layout
// TODO: Add zoom and pan
// TODO: 1px padding.
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
  private val imageView by bindView<ImageView>(R.id.imageviewer_image)
  private val flickDismissLayout by bindView<FlickDismissLayout>(R.id.imageviewer_image_container)
  private val progressView by bindView<View>(R.id.imageviewer_progress)
  private lateinit var activityBackgroundDrawable: Drawable

  override fun onCreate(savedInstanceState: Bundle?) {
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    super.onCreate(savedInstanceState)
    overridePendingTransition(0, 0)
    setContentView(R.layout.activity_image_viewer)

    animateDimmingOnEntry()
    loadImage()

    flickDismissLayout.setFlickGestureListener(flickGestureListener())
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

    val okHttpClient = OkHttpClient.Builder()
        .apply {
          val logging = HttpLoggingInterceptor()
          logging.level = HttpLoggingInterceptor.Level.BASIC
          addInterceptor(logging)
        }
        .build()

    val picasso = Picasso.Builder(this)
        .downloader(OkHttp3Downloader(okHttpClient))
        .build()

    picasso
        .load(photo.url(width = displayWidth))
        .into(targetWithProgress)

    // Picasso keeps a weak reference to targets. Avoid getting them GCed.
    imageView.setTag(R.id.picasso_target, targetWithProgress)
  }

  private fun flickGestureListener(): FlickGestureListener {
    // TODO: Don't listen for flick gestures if the image can pan further.
    //flickListener.setOnGestureInterceptor()

    return FlickGestureListener(ViewConfiguration.get(this)).apply {
      setFlickThresholdSlop(FlickGestureListener.DEFAULT_FLICK_THRESHOLD)
      setGestureCallbacks(object : FlickGestureListener.GestureCallbacks {
        override fun onFlickDismissEnd(flickAnimationDuration: Long) {
          finishInMillis(200)
        }

        override fun onMoveMedia(moveRatio: Float) {
          updateBackgroundDimmingAlpha(Math.abs(moveRatio))
        }
      })

      setContentHeightProvider(object : FlickGestureListener.ContentHeightProvider {
        override val contentHeightForDismissAnimation: Int
          get() = imageView.height
        //get() = imageView.getZoomedImageHeight() as Int

        // A non-MATCH_PARENT height is important so that the user
        // can easily dismiss the image if it's taking too long to load.
        override val contentHeightForCalculatingThreshold: Int
          get() = when {
            imageView.drawable == null -> resources.getDimensionPixelSize(R.dimen.mediaalbumviewer_image_height_when_empty)
            else -> imageView.height
            //else -> imageView.getVisibleZoomedImageHeight() as Int
          }
      })
    }
  }

  private fun animateDimmingOnEntry() {
    activityBackgroundDrawable = rootLayout.background.mutate()
    rootLayout.background = activityBackgroundDrawable

    ObjectAnimator.ofFloat(1F, 0f).apply {
      duration = 600
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
    // Increase dimming exponentially so that the background is fully transparent while the image has been moved by half.
    val dimming = 1f - Math.min(1f, transparencyFactor * 2)
    activityBackgroundDrawable.alpha = (dimming * 255).toInt()
  }
}
