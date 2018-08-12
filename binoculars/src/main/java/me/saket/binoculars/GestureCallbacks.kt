package me.saket.binoculars

import android.support.annotation.FloatRange

interface GestureCallbacks {

  /**
   * Called when the View has been flicked and the Activity should be dismissed.
   *
   * @param flickAnimationDuration Time the Activity should wait to finish for
   * the flick animation to complete.
   */
  fun onFlickDismissed(flickAnimationDuration: Long)

  /**
   * Called while this View is being moved around.
   *
   * @param moveRatio Distance moved (from the View's original position) as a
   * ratio of the View's height.
   */
  fun onMove(@FloatRange(from = -1.0, to = 1.0) moveRatio: Float)
}
