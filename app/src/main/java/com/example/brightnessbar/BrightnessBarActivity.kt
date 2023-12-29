package com.example.brightnessbar

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.brightnessbar.Utils.showPermissionRationale
import com.example.brightnessbar.databinding.BrightnessBarActivityBinding

class BrightnessBarActivity : AppCompatActivity() {

    private lateinit var binding: BrightnessBarActivityBinding
    private lateinit var topButton: Button
    private lateinit var bottomLayout: LinearLayout
    private lateinit var topSlider: Switch  // Add a Switch for on/off slider.
    private var hasAccessibilityPermission = false
    private var hasWriteSettingsPermission = false
    private var isAccessibilityPermissionRequestedEarlier = false

    companion object {
        private const val PERMISSION_ACCESSIBILITY_SETTINGS_REQUEST_CODE = 1
        const val PERMISSION_OVERLAY_REQUEST_CODE = 2
        const val PERMISSION_WRITE_SETTINGS_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BrightnessBarActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        topButton = binding.enableServiceButton
        topSlider = binding.serviceToggleSwitch
        bottomLayout = binding.bottomLayout

        val sharedPrefs = getSharedPreferences("OverlaySettings", Context.MODE_PRIVATE)
        val isSliderEnabled = sharedPrefs.getBoolean("SliderEnabled", true) // Default is true
        topSlider.isChecked = isSliderEnabled

        // Initial state: bottom layout is non-interactable and faded
        updateUIBasedOnPermissions()

        topButton.setOnClickListener {
            if (checkAndRequestPermissions()) {
                updateUIBasedOnPermissions()
            }
        }

        topSlider.setOnCheckedChangeListener { _, isChecked ->
            // Save the new state of the switch
            with(sharedPrefs.edit()) {
                putBoolean("SliderEnabled", isChecked)
                apply()
            }

            // Send a broadcast to MyAccessibilityService to toggle the overlay
            val intent = Intent("com.example.accessibilitysvc.MyAccessibilityService.ACTION_TOGGLE_OVERLAY")
            intent.putExtra("com.example.accessibilitysvc.MyAccessibilityService.EXTRA_OVERLAY_STATE", isChecked)
            sendBroadcast(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Check again if service is enabled when user returns to the app
        updateUIBasedOnPermissions()
    }

    private fun checkAndRequestPermissions(): Boolean {
        hasWriteSettingsPermission = Settings.System.canWrite(this)
        if (!hasWriteSettingsPermission) {
            val intent =
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
            startActivityForResult(intent, PERMISSION_WRITE_SETTINGS_REQUEST_CODE)
            return false
        }

        hasAccessibilityPermission =
            isAccessibilityServiceEnabled(this, MyAccessibilityService::class.java)
        if (!hasAccessibilityPermission) {
            isAccessibilityPermissionRequestedEarlier = true
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivityForResult(intent, PERMISSION_ACCESSIBILITY_SETTINGS_REQUEST_CODE)
            return false
        }

        return true
    }

    private fun toggleVisibility(visible: Boolean) {
        val sharedPrefs = getSharedPreferences("OverlaySettings", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean("OverlayTransparency", visible)
            apply()
        }

        // Send a broadcast indicating that the transparency has changed
        val intent = Intent("com.example.brightnessbar.TRANSPARENCY_CHANGED")
        sendBroadcast(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PERMISSION_WRITE_SETTINGS_REQUEST_CODE) {
            hasWriteSettingsPermission = Settings.System.canWrite(this)
            if (!hasWriteSettingsPermission) {
                showPermissionRationale(this, getString(R.string.write_settings_rationale), PERMISSION_WRITE_SETTINGS_REQUEST_CODE)
            }
        } else if (requestCode == PERMISSION_ACCESSIBILITY_SETTINGS_REQUEST_CODE) {
            hasAccessibilityPermission =
                isAccessibilityServiceEnabled(this, MyAccessibilityService::class.java)
        }

        if (hasWriteSettingsPermission && !hasAccessibilityPermission) {
            if (isAccessibilityPermissionRequestedEarlier) {
                showPermissionRationale(this, getString(R.string.accessibility_service_rationale), PERMISSION_ACCESSIBILITY_SETTINGS_REQUEST_CODE)
            } else {
                checkAndRequestPermissions()
            }
        }
    }

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
//
//                    getString(R.string.accessibility_service_rationale) -> {
//                        startActivityForResult(
//                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
//                            PERMISSION_ACCESSIBILITY_SETTINGS_REQUEST_CODE
//                        )
//                    }
//                }
//            }
//            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
//            .create()
//            .show()
//    }


    // Update UI based on whether the accessibility service is enabled
    private fun updateUIBasedOnPermissions() {
        val hasAllPermissions = checkAllPermissions()
        bottomLayout.isEnabled = hasAllPermissions
        bottomLayout.alpha = if (hasAllPermissions) 1f else 0.5f

        if (hasAllPermissions) {
            // If all permissions are granted, show the slider and hide the button
            topSlider.visibility = View.VISIBLE
            topButton.visibility = View.GONE
        } else {
            // If any permission is missing, show the button and hide the slider
            topSlider.visibility = View.GONE
            topButton.visibility = View.VISIBLE
        }
    }

    // Check if Accessibility Service is enabled
    private fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val serviceName = ComponentName(context, service).flattenToString()
        return enabledServices?.contains(serviceName) == true
    }

    private fun checkAllPermissions(): Boolean {
        val hasWritePermission = Settings.System.canWrite(this)
        val hasAccessibilityPermission =
            isAccessibilityServiceEnabled(this, MyAccessibilityService::class.java)

        return hasWritePermission && hasAccessibilityPermission
    }
}