package com.example.brightnessbar

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.ToggleButton
import com.example.brightnessbar.databinding.ActivityMainBinding


class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private var hasOverlayPermission = false
    private var hasWriteSettingsPermission = false
    private var isWritePermissionRequestedEarlier = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonStart.setOnClickListener {
                startBrightnessBarActivity()
        }
    }

//    private fun checkAndRequestPermissions(): Boolean {
//        hasOverlayPermission = Settings.canDrawOverlays(this)
//        if (!hasOverlayPermission) {
//            val intent =
//                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
//            startActivityForResult(intent, PERMISSION_OVERLAY_REQUEST_CODE)
//            return false
//        }
//
//        hasWriteSettingsPermission = Settings.System.canWrite(this)
//        if (!hasWriteSettingsPermission) {
//            isWritePermissionRequestedEarlier = true
//            val intent =
//                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
//            startActivityForResult(intent, PERMISSION_WRITE_SETTINGS_REQUEST_CODE)
//            return false
//        }
//
//        return true
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == PERMISSION_OVERLAY_REQUEST_CODE) {
//            hasOverlayPermission = Settings.canDrawOverlays(this)
//            if (!hasOverlayPermission) {
//                showPermissionRationale(getString(R.string.overlay_permission_rationale))
//            }
//        } else if (requestCode == PERMISSION_WRITE_SETTINGS_REQUEST_CODE) {
//            hasWriteSettingsPermission = Settings.System.canWrite(this)
//        }
//
//        if (hasOverlayPermission && hasWriteSettingsPermission) {
//            startBrightnessBarActivity()
//        } else if (hasOverlayPermission && !hasWriteSettingsPermission) {
//            if (isWritePermissionRequestedEarlier) {
//                showPermissionRationale(getString(R.string.write_settings_rationale))
//            } else {
//                checkAndRequestPermissions()
//            }
//        }
//    }
//
//    private fun showPermissionRationale(rationale: String) {
//        AlertDialog.Builder(this)
//            .setMessage(rationale)
//            .setPositiveButton("Settings") { _, _ ->
//                when (rationale) {
//                    getString(R.string.overlay_permission_rationale) -> {
//                        startActivityForResult(
//                            Intent(
//                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                                Uri.parse("package:$packageName")
//                            ), PERMISSION_OVERLAY_REQUEST_CODE
//                        )
//                    }
//
//                    getString(R.string.write_settings_rationale) -> {
//                        startActivityForResult(
//                            Intent(
//                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
//                                Uri.parse("package:$packageName")
//                            ), PERMISSION_WRITE_SETTINGS_REQUEST_CODE
//                        )
//                    }
//                }
//            }
//            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
//            .create()
//            .show()
//    }

    private fun startBrightnessBarActivity() {
        val intent = Intent(this, BrightnessBarActivity::class.java)
        startActivity(intent)
    }
}
