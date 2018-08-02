package me.saket.binoculars

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * A ViewGroup that can be dismissed by flicking it in any direction.
 */
class FlickDismissLayout(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

  // TODO: Make non-null.
  private var flickGestureListener: FlickGestureListener? = null

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    val intercepted = flickGestureListener!!.onTouch(this, ev)
    return intercepted || super.onInterceptTouchEvent(ev)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    flickGestureListener!!.onTouch(this, event)
    // Defaulting to true to avoid letting
    // parent ViewGroup receive any touch events.
    return true
  }

  override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
    super.requestDisallowInterceptTouchEvent(disallowIntercept)
  }

  fun setFlickGestureListener(flickGestureListener: FlickGestureListener) {
    this.flickGestureListener = flickGestureListener
  }
}
