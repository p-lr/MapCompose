package ovh.plrapps.mapcompose.demo.utils

import android.content.res.Resources

/**
 * Convert px to dp
 */
fun pxToDp(px: Int): Int {
    return (px / Resources.getSystem().displayMetrics.density).toInt()
}

/**
 * Convert dp to px
 */
fun dpToPx(dp: Int): Int {
    return (dp * Resources.getSystem().displayMetrics.density).toInt()
}