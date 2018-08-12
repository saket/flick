package me.saket.flick.sample

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UnsplashPhoto(val photoId: String) : Parcelable {

  fun url(width: Int): String = "https://images.unsplash.com/photo-$photoId?w=$width&q=80"
}
