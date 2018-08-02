package me.saket.binoculars


import android.annotation.SuppressLint
import android.support.annotation.FloatRange
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration

/**
 * Listeners for a flick gesture and also moves around the View with user's finger.
 */
class FlickGestureListener(viewConfiguration: ViewConfiguration) : View.OnTouchListener {

  @FloatRange(from = 0.0, to = 1.0)
  private var flickThresholdSlop = 0F

  private val touchSlop: Int = viewConfiguration.scaledTouchSlop                        // Min. distance to move before registering a gesture.
  private val maximumFlingVelocity: Int = viewConfiguration.scaledMaximumFlingVelocity  // Px per second.
  private var gestureCallbacks: GestureCallbacks? = null
  private var downX: Float = 0.toFloat()
  private var downY: Float = 0.toFloat()
  private var lastTouchX: Float = 0.toFloat()
  private var lastTouchY: Float = 0.toFloat()
  private var lastAction = -1
  private var touchStartedOnLeftSide: Boolean = false
  private var velocityTracker: VelocityTracker? = null
  private var verticalScrollRegistered: Boolean = false
  private var gestureCanceledUntilNextTouchDown: Boolean = false
  private var onGestureInterceptor: OnGestureInterceptor? = null
  private var contentHeightProvider: ContentHeightProvider? = null
  private var gestureInterceptedUntilNextTouchDown: Boolean = false

  interface OnGestureInterceptor {
    /**
     * Called once every-time a scroll gesture is registered. When this returns true, gesture detection is
     * skipped until the next touch-down is registered.
     *
     * @return True to intercept the gesture, false otherwise to let it go.
     */
    fun shouldIntercept(deltaY: Float): Boolean
  }

  interface ContentHeightProvider {
    /**
     * Height of the media content multiplied by its zoomed in ratio. Only used for animating the content out
     * of the window when a flick is registered.
     */
    val contentHeightForDismissAnimation: Int

    /**
     * Used for calculating if the content can be dismissed on finger up.
     */
    val contentHeightForCalculatingThreshold: Int
  }

  interface GestureCallbacks {
    /**
     * Called when the View has been flicked and the Activity should be dismissed.
     *
     * @param flickAnimationDuration Time the Activity should wait to finish for the flick animation to complete.
     */
    fun onFlickDismissEnd(flickAnimationDuration: Long)

    /**
     * Called while this View is being moved around.
     *
     * @param moveRatio Distance moved (from the View's original position) as a ratio of the View's height.
     */
    fun onMoveMedia(@FloatRange(from = -1.0, to = 1.0) moveRatio: Float)
  }

  init {
    // Default gesture interceptor: don't intercept anything.
    onGestureInterceptor = object : OnGestureInterceptor {
      override fun shouldIntercept(deltaY: Float): Boolean {
        return false
      }
    }
  }

  fun setOnGestureInterceptor(interceptor: OnGestureInterceptor) {
    onGestureInterceptor = interceptor
  }

  /**
   * Set minimum distance the user's finger should move (in percentage of the View's dimensions) after which a flick
   * can be registered.
   */
  fun setFlickThresholdSlop(@FloatRange(from = 0.0, to = 1.0) flickThresholdSlop: Float) {
    this.flickThresholdSlop = flickThresholdSlop
  }

  fun setGestureCallbacks(gestureCallbacks: GestureCallbacks) {
    this.gestureCallbacks = gestureCallbacks
  }

  fun setContentHeightProvider(contentHeightProvider: ContentHeightProvider) {
    this.contentHeightProvider = contentHeightProvider
  }

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
    // This is probably not the best way to handle touch events, but it's just easier this way.
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
            // Figure out if the View was fling'd and if the velocity + swiped distance is enough to dismiss this View.
            velocityTracker!!.computeCurrentVelocity(1000 /* px per second */)
            val yVelocityAbs = Math.abs(velocityTracker!!.yVelocity)
            val requiredYVelocity = view.height * 6 / 10
            val minSwipeDistanceForFling = view.height / 10

            if (yVelocityAbs > requiredYVelocity && yVelocityAbs < maximumFlingVelocity && distanceYAbs >= minSwipeDistanceForFling) {
              // Fling detected!
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
        if (!verticalScrollRegistered && onGestureInterceptor != null && onGestureInterceptor!!.shouldIntercept(deltaY)) {
          gestureInterceptedUntilNextTouchDown = true
          return false
        }

        val isScrollingVertically = distanceYAbs > touchSlop && distanceYAbs > distanceXAbs
        val isScrollingHorizontally = distanceXAbs > touchSlop && distanceYAbs < distanceXAbs

        // Avoid reading the gesture if the user is scrolling horizontally (like a ViewPager).
        if (!verticalScrollRegistered && isScrollingHorizontally) {
          gestureCanceledUntilNextTouchDown = true
          return false
        }

        if (verticalScrollRegistered || isScrollingVertically) {
          verticalScrollRegistered = true

          view.translationX = view.translationX + deltaX
          view.translationY = view.translationY + deltaY

          view.parent.requestDisallowInterceptTouchEvent(true)

          // Rotate the card because we naturally make a swipe gesture in a circular path while holding our phones.
          if (ROTATION_ENABLED) {
            val moveRatioDelta = deltaY / view.height
            view.pivotY = 0f
            view.rotation = view.rotation + moveRatioDelta * (if (touchStartedOnLeftSide) -20F else 20F)
          }

          // Send callback so that the background dim can be faded in/out.
          dispatchOnPhotoMoveCallback(view)

          // Track the velocity so that we can later figure out if this View was fling'd (instead of dragged).
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
    gestureCallbacks!!.onMoveMedia(moveRatio)
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

    val rotationAngle = view.rotation
    val distanceRotated = Math.ceil(Math.abs(Math.sin(Math.toRadians(rotationAngle.toDouble())) * view.width / 2)).toInt()
    val throwDistance = distanceRotated + Math.max(contentHeightProvider!!.contentHeightForDismissAnimation, view.rootView.height)

    view.animate().cancel()
    view.animate()
        .translationY((if (downwards) throwDistance else -throwDistance).toFloat())
        .withStartAction { gestureCallbacks!!.onFlickDismissEnd(flickAnimDuration) }
        .setDuration(flickAnimDuration)
        .setInterpolator(ANIM_INTERPOLATOR)
        .setUpdateListener { dispatchOnPhotoMoveCallback(view) }
        .start()
  }

  private fun hasFingerMovedEnoughToFlick(distanceYAbs: Float): Boolean {
    //Timber.d("hasFingerMovedEnoughToFlick()");
    //Timber.i("Content h: %s", contentHeightProvider.getContentHeightForCalculatingThreshold());
    //Timber.i("flickThresholdSlop: %s", flickThresholdSlop);
    //Timber.i("distanceYAbs: %s", distanceYAbs);
    val thresholdDistanceY = contentHeightProvider!!.contentHeightForCalculatingThreshold * flickThresholdSlop
    return distanceYAbs > thresholdDistanceY
  }

  companion object {
    private val ANIM_INTERPOLATOR = FastOutSlowInInterpolator()
    private const val ROTATION_ENABLED = true
    val DEFAULT_FLICK_THRESHOLD = 0.3f
  }
}
