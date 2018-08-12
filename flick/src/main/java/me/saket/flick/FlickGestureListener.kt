package me.saket.flick

import android.annotation.SuppressLint
import android.support.annotation.FloatRange
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration

class FlickGestureListener(viewConfiguration: ViewConfiguration) : View.OnTouchListener {

  /**
   * Minimum distance the user's finger should move (as a ratio to the View's
   * dimensions) after which a flick can be registered.
   */
  @FloatRange(from = 0.0, to = 1.0)
  var flickThresholdSlop = DEFAULT_FLICK_THRESHOLD
    set(value) {
      if ((value in 0F..1F).not()) {
        throw AssertionError("Must be between 0F and 1F")
      }
      field = value
    }

  /** Min. distance to move before registering a gesture. */
  private val touchSlop: Int = viewConfiguration.scaledTouchSlop

  /** Px per second. */
  private val maximumFlingVelocity: Int = viewConfiguration.scaledMaximumFlingVelocity

  var onGestureInterceptor: OnGestureInterceptor = OnGestureInterceptor.Default()
  var gestureCallbacks: GestureCallbacks? = null
  var contentHeightProvider: ContentHeightProvider? = null

  private var downX = 0F
  private var downY = 0F
  private var lastTouchX = 0F
  private var lastTouchY = 0F
  private var lastAction = -1
  private var touchStartedOnLeftSide: Boolean = false
  private var velocityTracker: VelocityTracker? = null
  private var verticalScrollRegistered: Boolean = false
  private var gestureCanceledUntilNextTouchDown: Boolean = false
  private var gestureInterceptedUntilNextTouchDown: Boolean = false

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouch(view: View, event: MotionEvent): Boolean {
    val touchX = event.rawX
    val touchY = event.rawY

    val distanceX = touchX - downX
    val distanceY = touchY - downY
    val distanceXAbs = Math.abs(distanceX)
    val distanceYAbs = Math.abs(distanceY)
    val deltaX = touchX - lastTouchX
    val deltaY = touchY - lastTouchY

    // Since both intercept() and touch() call this listener, we get duplicate ACTION_DOWNs.
    // This is probably not the best way to handle touch events, but it works and managing
    // both intercept() and touch() becomes easy.
    if (touchX == lastTouchX && touchY == lastTouchY && lastAction == event.action) {
      return false
    }

    lastTouchX = touchX
    lastTouchY = touchY
    lastAction = event.action

    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        downX = touchX
        downY = touchY
        touchStartedOnLeftSide = touchX < view.width / 2
        if (velocityTracker == null) {
          velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker!!.addMovement(event)
        return false
      }

      MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
        if (verticalScrollRegistered) {
          val flickRegistered = hasFingerMovedEnoughToFlick(distanceYAbs)
          val wasSwipedDownwards = distanceY > 0

          if (flickRegistered) {
            animateViewFlick(view, wasSwipedDownwards)

          } else {
            // Check if the View was fling'd and if the velocity + swipe distance is enough to dismiss.
            velocityTracker!!.computeCurrentVelocity(1000 /* px per second */)
            val yVelocityAbs = Math.abs(velocityTracker!!.yVelocity)
            val requiredYVelocity = view.height * 6 / 10
            val minSwipeDistanceForFling = view.height / 10

            if (yVelocityAbs > requiredYVelocity
                && distanceYAbs >= minSwipeDistanceForFling
                && yVelocityAbs < maximumFlingVelocity) {
              // Flick detected!
              animateViewFlick(view, wasSwipedDownwards, 100)

            } else {
              // Distance moved wasn't enough to dismiss.
              animateViewBackToPosition(view)
            }
          }
        }

        velocityTracker!!.recycle()
        velocityTracker = null
        verticalScrollRegistered = false
        gestureInterceptedUntilNextTouchDown = false
        gestureCanceledUntilNextTouchDown = false
        return false
      }

      MotionEvent.ACTION_MOVE -> {
        if (gestureInterceptedUntilNextTouchDown || gestureCanceledUntilNextTouchDown) {
          return false
        }

        // The listener only gets once chance to block the flick -- only if it's not already being moved.
        if (verticalScrollRegistered.not() && onGestureInterceptor.shouldIntercept(deltaY)) {
          gestureInterceptedUntilNextTouchDown = true
          return false
        }

        val isScrollingVertically = distanceYAbs > touchSlop && distanceYAbs > distanceXAbs
        val isScrollingHorizontally = distanceXAbs > touchSlop && distanceYAbs < distanceXAbs

        // Avoid registering a gesture if the user is scrolling horizontally (like a ViewPager).
        if (verticalScrollRegistered.not() && isScrollingHorizontally) {
          gestureCanceledUntilNextTouchDown = true
          return false
        }

        if (verticalScrollRegistered || isScrollingVertically) {
          verticalScrollRegistered = true

          view.translationX = view.translationX + deltaX
          view.translationY = view.translationY + deltaY

          view.parent.requestDisallowInterceptTouchEvent(true)

          // Rotate the content because. The idea is that we naturally
          // make drags in a circular path while holding our phones.
          val moveRatioDelta = deltaY / view.height * (if (touchStartedOnLeftSide) -20F else 20F)
          view.pivotY = 0f
          view.rotation = view.rotation + moveRatioDelta

          dispatchOnPhotoMoveCallback(view)

          // Track the velocity so that we can later figure out if
          // this View was fling'd (instead of dragged).
          velocityTracker!!.addMovement(event)
          return true

        } else {
          return false
        }
      }

      else -> return false
    }
  }

  private fun dispatchOnPhotoMoveCallback(view: View) {
    val moveRatio = view.translationY / view.height
    gestureCallbacks().onMove(moveRatio)
  }

  private fun animateViewBackToPosition(view: View) {
    view.animate().cancel()
    view.animate()
        .translationX(0f)
        .translationY(0f)
        .rotation(0f)
        .setDuration(200)
        .setUpdateListener { dispatchOnPhotoMoveCallback(view) }
        .setInterpolator(ANIM_INTERPOLATOR)
        .start()
  }

  private fun animateViewFlick(view: View, downwards: Boolean) {
    animateViewFlick(view, downwards, 200)
  }

  private fun animateViewFlick(view: View, downwards: Boolean, flickAnimDuration: Long) {
    if (view.pivotY != 0f) {
      throw AssertionError("Formula used for calculating distance rotated only works if the pivot is at (x,0")
    }

    // I no longer remember the reason behind applying so many Math functions. Help.
    val rotationAngle = view.rotation
    val distanceRotated = Math.ceil(Math.abs(Math.sin(Math.toRadians(rotationAngle.toDouble())) * view.width / 2)).toInt()
    val throwDistance = distanceRotated + Math.max(contentHeightProvider().heightForDismissAnimation, view.rootView.height)

    view.animate().cancel()
    view.animate()
        .translationY((if (downwards) throwDistance else -throwDistance).toFloat())
        .withStartAction { gestureCallbacks().onFlickDismissed(flickAnimDuration) }
        .setDuration(flickAnimDuration)
        .setInterpolator(ANIM_INTERPOLATOR)
        .setUpdateListener { dispatchOnPhotoMoveCallback(view) }
        .start()
  }

  private fun hasFingerMovedEnoughToFlick(distanceYAbs: Float): Boolean {
    val thresholdDistanceY = contentHeightProvider().heightForCalculatingDismissThreshold * flickThresholdSlop
    return distanceYAbs > thresholdDistanceY
  }

  private fun gestureCallbacks(): GestureCallbacks {
    if (gestureCallbacks == null) {
      throw AssertionError("Did you forget to set gestureCallbacks?")
    }
    return gestureCallbacks!!
  }

  private fun contentHeightProvider(): ContentHeightProvider {
    if (contentHeightProvider == null) {
      throw AssertionError("Did you forget to set contentHeightProvider?")
    }
    return contentHeightProvider!!
  }

  companion object {
    private val ANIM_INTERPOLATOR = FastOutSlowInInterpolator()
    const val DEFAULT_FLICK_THRESHOLD = 0.3f
  }
}
