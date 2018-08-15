# Flick

![Flick in action](https://github.com/saket/Flick/blob/master/screenshots/Flick.gif)

Flick is a tiny library for flick dismissing images (or anything actually). You can read the announcement [blog post](http://saket.me/?p=707) to learn how Flick was created.

    implementation 'me.saket:flick:1.2.0'

## Usage

The [sample project](https://github.com/saket/Flick/tree/master/sample/src/main/java/me/saket/flick/sample) contains best practices for using Flick. You can also [download an APK from here](https://github.com/saket/Flick/releases) for testing it on your phone.

```xml
<me.saket.flick.FlickDismissLayout
  android:layout_width="match_parent"
  android:layout_height="wrap_content">

  <ImageView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"/>
</me.saket.flick.FlickDismissLayout>
```

Flick requires you to manually provide the content dimensions instead of it checking the content's height. This is useful for scalable `ImageViews`, where the height will always be set to match-parent, but the actual image may not be consuming the entire height.

```kotlin
flickDismissLayout.gestureListener = FlickGestureListener(context).apply {
  contentHeightProvider = object : ContentHeightProvider {
    override val heightForDismissAnimation: Int
      get() = imageView.drawable * imageView.zoomRatio

    override val heightForCalculatingDismissThreshold: Int
      get() {
        // Zoomed in height minus the portions of image that has gone
        // outside display bounds, because they are longer visible.
        imageView.visibleZoomedImageHeight
      }
    }
}
```

Flick offers two callbacks for receiving updates of gestures.

```kotlin
gestureListener.gestureCallbacks = object : GestureCallbacks {
  override fun onFlickDismiss(animationDuration: Long) {
    // Called when the View has been flicked and the Activity
    // should be dismissed.
    flickDismissLayout.postDelayed({ finish() }, animationDuration)
  }

  override fun onMove(@FloatRange(from = -1.0, to = 1.0) moveRatio: Float) {
    // Called while this View is being moved around. Updating
    // background dimming is a good usecase for this callback.
  }
}
```

## License

```
Copyright 2019 Saket Narayan.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
