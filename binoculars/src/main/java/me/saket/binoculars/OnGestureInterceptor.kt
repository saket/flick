package me.saket.binoculars

interface OnGestureInterceptor {

  /**
   * Called once every-time a scroll gesture is registered. When this returns
   * true, gesture detection is skipped until the next touch-down.
   *
   * @return True to intercept the gesture, false otherwise to let it go.
   */
  fun shouldIntercept(deltaY: Float): Boolean

  class Default : OnGestureInterceptor {
    // Default gesture interceptor: don't intercept anything.
    override fun shouldIntercept(deltaY: Float): Boolean {
      return false
    }
  }
}
