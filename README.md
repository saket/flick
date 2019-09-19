# Flick

![Flick in action](https://github.com/saket/Flick/blob/master/screenshots/flick_demo.gif)

Flick is a tiny library for flick dismissing images (or anything actually). You can read the announcement [blog post](http://saket.me/?p=707) to learn how Flick was created.

```
implementation 'me.saket:flick:1.5.0'
```

## Usage

The [sample project](https://github.com/saket/Flick/tree/master/sample/src/main/java/me/saket/flick/sample) contains best practices for using Flick. You can [download its APK from here](https://github.com/saket/Flick/releases) for trying it out on your phone.

```xml
<me.saket.flick.FlickDismissLayout
  android:layout_width="match_parent"
  android:layout_height="wrap_content">

  <ImageView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"/>
</me.saket.flick.FlickDismissLayout>
```

Flick requires you to manually provide the content dimensions instead of it relying on the content View's dimensions. This is useful for scalable `ImageViews`, where the height will always be set to match-parent, but the actual image may or may not be consuming the entire space.

```kotlin
val callbacks = object : FlickCallbacks {
  override fun onFlickDismiss(animationDuration: Long) {
    // Called when the View has been flicked
    // and the Activity should be dismissed.
    flickDismissLayout.postDelayed({ finish() }, animationDuration)
  }

  override fun onMove(@FloatRange(from = -1.0, to = 1.0) moveRatio: Float) {
    // Called while this View is being moved around. Updating
    // background dimming is a good usecase for this callback.
  }
}

val contentSizeProvider = object : ContentSizeProvider {
  override fun heightForDismissAnimation(): Int =
    imageView.zoomedImageHeight()

  override fun heightForCalculatingDismissThreshold(): Int =
    // Zoomed in height minus the portions of image that has gone
    // outside display bounds, because they are longer visible.
    imageView.visibleZoomedImageHeight()
}

val flickDismissLayout = findViewById<FlickDismissLayout>(...)
flickDismissLayout.gestureListener = FlickGestureListener(context, contentSizeProvider, callbacks)
```

**Intercepting flicks**

For usecases where the content can be scrolled further in the direction of the gesture, Flick exposes a way for intercepting flick detection,

```kotlin
// Block flick gestures if the image can pan further.
gestureListener.gestureInterceptor = { scrollY ->
  val isScrollingUpwards = scrollY < 0
  val directionInt = if (isScrollingUpwards) -1 else +1
  val canPanFurther = imageView.canScrollVertically(directionInt)

  when {
    canPanFurther -> InterceptResult.INTERCEPTED
    else -> InterceptResult.IGNORED
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
