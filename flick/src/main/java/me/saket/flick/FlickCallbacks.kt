package me.saket.flick

import androidx.annotation.FloatRange

interface FlickCallbacks {
  /**
   * Called when [FlickDismissLayout] detects a drag. This is called once per flick gesture.
   */
  fun onMoveStart() = Unit

  /**
   * Called when the content of [FlickDismissLayout] are being moved.
   *
   * @param moveRatio Distance moved (from the View's original position) as a
   * ratio of the View's (not content) height.
   */
  fun onMove(@FloatRange(from = -1.0, to = 1.0) moveRatio: Float)

  /**
   * Called when [FlickDismissLayout]'s content have been flicked and the screen should be dismissed.
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
      onFlickDismiss: (flickAnimationDuration: Long) -> Unit = {},
      onMoveStart: () -> Unit = {}
    ): FlickCallbacks {
      return object : FlickCallbacks {
        override fun onFlickDismiss(flickAnimationDuration: Long) {
          return onFlickDismiss(flickAnimationDuration)
        }

        override fun onMoveStart() {
          onMoveStart()
        }

        override fun onMove(moveRatio: Float) {
          onMove(moveRatio)
        }
      }
    }
  }
}
