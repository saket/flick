package me.saket.flick

internal class ContentSizeProviderCompat private constructor(
  private val v1: ContentSizeProvider?,
  private val v2: ContentSizeProvider2?
) {
  fun heightForDismissAnimation(): Int {
    return when {
      v2 != null -> v2.invoke()
      else -> v1!!.heightForDismissAnimation()
    }
  }

  fun heightForCalculatingDismissThreshold(maxHeight: () -> Int): Int {
    return when {
      v2 != null -> minOf(v2.invoke(), maxHeight())
      else -> v1!!.heightForCalculatingDismissThreshold()
    }
  }

  companion object {
    fun v1(v1: ContentSizeProvider): ContentSizeProviderCompat {
      return ContentSizeProviderCompat(v1 = v1, v2 = null)
    }

    fun v2(v2: ContentSizeProvider2): ContentSizeProviderCompat {
      return ContentSizeProviderCompat(v1 = null, v2 = v2)
    }
  }
}