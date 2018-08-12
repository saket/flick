package me.saket.flick.sample.gallery

import android.os.Bundle
import android.support.annotation.IdRes
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import com.squareup.picasso.Picasso
import me.saket.flick.sample.R
import me.saket.flick.sample.UnsplashPhoto
import me.saket.flick.sample.viewer.ImageViewerActivity

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    loadImage(R.id.image1, UnsplashPhoto("1493928841026-e1ab0a590a61"))
    loadImage(R.id.image2, UnsplashPhoto("1523970887933-5b08c586ad76"))
    loadImage(R.id.image3, UnsplashPhoto("1531213203257-16afb0eac95e"))
    loadImage(R.id.image4, UnsplashPhoto("1516204195128-91f583dca4e4"))
    loadImage(R.id.image5, UnsplashPhoto("1470093851219-69951fcbb533"))
    loadImage(R.id.image6, UnsplashPhoto("1470390356535-d19bbf47bacb"))
  }

  private fun loadImage(@IdRes imageViewId: Int, photo: UnsplashPhoto) {
    val imageView = findViewById<ImageView>(imageViewId)
    val displayWidth = resources.displayMetrics.widthPixels

    Picasso.get()
        .load(photo.url(width = displayWidth / 2))
        .placeholder(R.color.gray_800)
        .into(imageView)

    imageView.setOnClickListener {
      startActivity(ImageViewerActivity.intent(this, photo))
    }
  }
}
