package me.saket.flick

/**
 * @param scaledHeight Height of the content with its scale taken
 * into account if, for e.g., it's zoomable.
 */
class ContentSizeProvider2(private val scaledHeight: () -> Int) {
  operator fun invoke(): Int = scaledHeight()
}

@Deprecated("Use ContentSizeProvider2 instead.")
interface ContentSizeProvider {
  /**
   * Height of the content with its scale taken into account. This is used
   * for animating the content out of the window when a flick is registered.
   */
  fun heightForDismissAnimation(): Int

  /**
   * Height of the content that is currently visible on screen. This will
   * be equal to content's zoomed height minus the portion of image that
   * is outside the View's bounds. This is used for calculating if the
   * content has moved enough to be dismissed on release.
   */
  fun heightForCalculatingDismissThreshold(): Int
}
