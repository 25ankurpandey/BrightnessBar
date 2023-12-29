package com.example.brightnessbar

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

object Utils {
    fun showPermissionRationale(context: Context, rationale: String, requestCode: Int) {
        AlertDialog.Builder(context)
            .setMessage(rationale)
            .setPositiveButton("Settings") { _, _ ->
                val intent = when (rationale) {
                    context.getString(R.string.overlay_permission_rationale) -> {
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    }
                    context.getString(R.string.write_settings_rationale) -> {
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    }
                    context.getString(R.string.accessibility_service_rationale) -> {
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    }
                    else -> return@setPositiveButton // or handle default case
                }
                // For Activity, use startActivityForResult; for others, use a different approach
                if (context is Activity) {
                    context.startActivityForResult(intent, requestCode)
                } else {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    fun getStatusBarHeight(resources: Resources): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    fun calculateHapticIntensity(speed: Float): Int {
        return when {
            speed < 0.5 -> 80
            speed < 1 -> 200
            else -> 255
        }
    }

    fun setSystemBrightness(context: Context, brightnessLevel: Int) {
        val contentResolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(context)) {
                try {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessLevel)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    Toast.makeText(context, "Need permission to change settings!", Toast.LENGTH_SHORT).show()
                }
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } else {
            try {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessLevel)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to change settings!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun performHapticFeedback(vibrator: Vibrator, intensity: Int) {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, intensity))
            } else {
                vibrator.vibrate(50)
            }
        }
    }
}
