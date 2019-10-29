package me.saket.flick

import androidx.annotation.FloatRange

interface FlickCallbacks {

  /**
   * Called while this View is being moved around.
   *
   * @param moveRatio Distance moved (from the View's original position) as a
   * ratio of the View's (not content) height.
   */
  fun onMove(@FloatRange(from = -1.0, to = 1.0) moveRatio: Float)

  /**
   * Called when the View has been flicked and the Activity should be dismissed.
   *
   * @param flickAnimationDuration Time the Activity should wait to finish for
   * the flick animation to complete.
   */
  fun onFlickDismiss(flickAnimationDuration: Long)

  companion object {
    /**
     * Convenience function for passing lambdas instead of creating an object
     * of [FlickCallbacks].
     *
     * @param onMove See [FlickCallbacks.onMove]
     * @param onFlickDismiss See [FlickCallbacks.onFlickDismiss]
     */
    operator fun invoke(
      onMove: (moveRatio: Float) -> Unit = {},
      onFlickDismiss: (flickAnimationDuration: Long) -> Unit = {}
    ): FlickCallbacks {
      return object : FlickCallbacks {
        override fun onFlickDismiss(flickAnimationDuration: Long) {
          return onFlickDismiss(flickAnimationDuration)
        }

        override fun onMove(moveRatio: Float) {
          onMove(moveRatio)
        }
      }
    }
  }
}
