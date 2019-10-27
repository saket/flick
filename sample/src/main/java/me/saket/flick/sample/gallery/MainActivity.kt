package me.saket.flick.sample.gallery

import android.os.Bundle
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import me.saket.flick.sample.R
import me.saket.flick.sample.UnsplashPhoto
import me.saket.flick.sample.viewer.ImageViewerActivity

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    loadImage(R.id.image1, UnsplashPhoto("1470390356535-d19bbf47bacb"))
    loadImage(R.id.image2, UnsplashPhoto("1523970887933-5b08c586ad76"))
    loadImage(R.id.image3, UnsplashPhoto("1508138221679-760a23a2285b"))
    loadImage(R.id.image4, UnsplashPhoto("1491884662610-dfcd28f30cfb"))
    loadImage(R.id.image5, UnsplashPhoto("1470093851219-69951fcbb533"))
    loadImage(R.id.image6, UnsplashPhoto("1504681869696-d977211a5f4c"))
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
