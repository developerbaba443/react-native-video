package com.brentvatne.exoplayer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Process
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.core.app.AppOpsManagerCompat
import com.brentvatne.receiver.PictureInPictureReceiver
import com.facebook.react.uimanager.ThemedReactContext

object PictureInPictureUtil {
    private const val FLAG_SUPPORTS_PICTURE_IN_PICTURE = 0x400000

    @JvmStatic
    fun enterPictureInPictureMode(context: ThemedReactContext, pictureInPictureParams: PictureInPictureParams?) {
        if (!isSupportPictureInPicture(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pictureInPictureParams != null) {
            context.currentActivity?.enterPictureInPictureMode(pictureInPictureParams)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.currentActivity?.enterPictureInPictureMode()
        }
    }

    @JvmStatic
    fun updatePictureInPictureActions(context: ThemedReactContext, pictureInPictureParams: PictureInPictureParams) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isSupportPictureInPicture(context)) return
            context.currentActivity?.setPictureInPictureParams(pictureInPictureParams)
        }
    }

    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.O)
    fun getPictureInPictureActions(context: ThemedReactContext, isPaused: Boolean, receiver: PictureInPictureReceiver): ArrayList<RemoteAction> {
        val intent = receiver.getPipActionIntent(isPaused)
        val resource =
            if (isPaused) androidx.media3.ui.R.drawable.exo_icon_play else androidx.media3.ui.R.drawable.exo_icon_pause
        val icon = Icon.createWithResource(context, resource)
        val title = if (isPaused) "play" else "pause"
        return arrayListOf(RemoteAction(icon, title, title, intent))
    }

    private fun isSupportPictureInPicture(context: ThemedReactContext): Boolean =
        checkIsApiSupport() && checkIsSystemSupportPIP(context) && checkIsUserAllowPIP(context)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    private fun checkIsApiSupport(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkIsSystemSupportPIP(context: ThemedReactContext): Boolean {
        val activity = context.currentActivity ?: return false

        val activityInfo = activity.packageManager.getActivityInfo(activity.componentName, PackageManager.GET_META_DATA)
        // detect current activity's android:supportsPictureInPicture value defined within AndroidManifest.xml
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/ActivityInfo.java;l=1090-1093;drc=7651f0a4c059a98f32b0ba30cd64500bf135385f
        val isActivitySupportPip = activityInfo.flags and FLAG_SUPPORTS_PICTURE_IN_PICTURE != 0

        // PIP might be disabled on devices that have low RAM.
        val isPipAvailable = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

        return isActivitySupportPip && isPipAvailable
    }

    private fun checkIsUserAllowPIP(context: ThemedReactContext): Boolean {
        val activity = context.currentActivity ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("InlinedApi")
            val result = AppOpsManagerCompat.noteOpNoThrow(
                activity,
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                Process.myUid(),
                activity.packageName
            )
            // In Android 10 Google Pixel, If allow,MODE_ALLOWED. If not allow, MODE_ERRORED
            // Log.d(TAG, "isSupportPIP: OPSTR_PICTURE_IN_PICTURE=" + result); // MODE_ERRORED
            AppOpsManager.MODE_ALLOWED == result
        } else {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        }
    }
}