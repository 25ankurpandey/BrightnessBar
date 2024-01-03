package com.example.brightnessbar

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.brightnessbar.Constants.ACTION_CHANGE_OVERLAY_DRAWABLE
import com.example.brightnessbar.Constants.ACTION_CHANGE_OVERLAY_VISIBILITY
import com.example.brightnessbar.Constants.ACTION_TOGGLE_OVERLAY
import com.example.brightnessbar.Constants.EXTRA_OVERLAY_STATE
import com.example.brightnessbar.Utils.showPermissionRationale
import com.example.brightnessbar.databinding.BrightnessBarActivityBinding

class BrightnessBarActivity : AppCompatActivity() {

    private lateinit var binding: BrightnessBarActivityBinding
    private lateinit var grantPermissionsSwitch: Button
    private lateinit var bottomLayout: LinearLayout
    private lateinit var serviceToggleSwitch: Switch
    private lateinit var brightnessBarVisibilitySwitch: Switch
    private lateinit var changeDrawableButton: Button
    private var hasAccessibilityPermission = false
    private var hasWriteSettingsPermission = false
    private var isAccessibilityPermissionRequestedEarlier = false

    companion object {
        private const val PERMISSION_ACCESSIBILITY_SETTINGS_REQUEST_CODE = 1
        const val PERMISSION_OVERLAY_REQUEST_CODE = 2
        const val PERMISSION_WRITE_SETTINGS_REQUEST_CODE = 3
        const val DRAWABLE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BrightnessBarActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        grantPermissionsSwitch = binding.enableServiceButton
        serviceToggleSwitch = binding.serviceToggleSwitch
        changeDrawableButton = binding.changeDrawableButton
        bottomLayout = binding.bottomLayout
        brightnessBarVisibilitySwitch = binding.brightnessBarVsibilityToggleSwitch

        val sharedPrefs = getSharedPreferences("OverlaySettings", Context.MODE_PRIVATE)
        serviceToggleSwitch.isChecked =
            sharedPrefs.getBoolean("OverlayEnabled", true) // Default is true
        brightnessBarVisibilitySwitch.isChecked =
            sharedPrefs.getBoolean("OverlayVisible", true) // Default is true

        // Initial state: bottom layout is non-interactable and faded
        updateUIBasedOnPermissions()

        grantPermissionsSwitch.setOnClickListener {
            if (checkAndRequestPermissions()) {
                updateUIBasedOnPermissions()
            }
        }

        serviceToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save the new state of the switch
            with(sharedPrefs.edit()) {
                putBoolean("OverlayEnabled", isChecked)
                apply()
            }

            // Send a broadcast to MyAccessibilityService to toggle the overlay
            val intent = Intent(ACTION_TOGGLE_OVERLAY)
            intent.putExtra(
                "com.example.accessibilitysvc.MyAccessibilityService.EXTRA_OVERLAY_STATE",
                isChecked
            )
            sendBroadcast(intent)
        }

        brightnessBarVisibilitySwitch.setOnCheckedChangeListener { _, isChecked ->
            with(sharedPrefs.edit()) {
                putBoolean("OverlayVisible", isChecked)
                apply()
            }
            if (serviceToggleSwitch.isChecked) {  // Only proceed if the main service toggle is on
                val intent = Intent(ACTION_CHANGE_OVERLAY_VISIBILITY)
                intent.putExtra(
                    "com.example.accessibilitysvc.MyAccessibilityService.EXTRA_OVERLAY_VISIBILITY",
                    isChecked
                )
                sendBroadcast(intent)
            } else {
                Toast.makeText(this, "Please enable the service first", Toast.LENGTH_SHORT).show()
                brightnessBarVisibilitySwitch.isChecked =
                    false  // Reset the toggle if the service isn't enabled
            }
        }

        changeDrawableButton.setOnClickListener {
            showDrawableSelector()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PERMISSION_WRITE_SETTINGS_REQUEST_CODE -> {
                hasWriteSettingsPermission = Settings.System.canWrite(this)
                if (!hasWriteSettingsPermission) {
                    showPermissionRationale(
                        this,
                        getString(R.string.write_settings_rationale),
                        PERMISSION_WRITE_SETTINGS_REQUEST_CODE
                    )
                }
            }

            PERMISSION_ACCESSIBILITY_SETTINGS_REQUEST_CODE -> {
                hasAccessibilityPermission =
                    isAccessibilityServiceEnabled(this, MyAccessibilityService::class.java)
                if (hasWriteSettingsPermission && !hasAccessibilityPermission) {
                    if (isAccessibilityPermissionRequestedEarlier) {
                        showPermissionRationale(
                            this,
                            getString(R.string.accessibility_service_rationale),
                            PERMISSION_ACCESSIBILITY_SETTINGS_REQUEST_CODE
                        )
                    } else {
                        checkAndRequestPermissions()
                    }
                }
            }
        }
    }

    // Update UI based on whether the accessibility service is enabled
    private fun updateUIBasedOnPermissions() {
        val hasAllPermissions = checkAllPermissions()

        // Toggle the visibility of the grantPermissions button
        grantPermissionsSwitch.visibility = if (hasAllPermissions) View.GONE else View.VISIBLE

        // Set the entire bottomLayout (LinearLayout) and its children to be non-interactable or interactable based on permissions
        bottomLayout.isEnabled = hasAllPermissions
        bottomLayout.alpha = if (hasAllPermissions) 1f else 0.5f

        // Recursively disable or enable all child views within the layout
        setViewAndChildrenEnabled(bottomLayout, hasAllPermissions)
    }

    private fun setViewAndChildrenEnabled(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                setViewAndChildrenEnabled(child, enabled)
            }
        }
    }

    private fun showDrawableSelector() {
        // Inflate the custom AlertDialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_drawable_selector, null)

        // Prepare seek bars and drawable resource IDs mapping
        val seekBarMap = mapOf(
            dialogView.findViewById<SeekBar>(R.id.seekBar1) to R.drawable.progress_bar_fill_black,
            dialogView.findViewById<SeekBar>(R.id.seekBar2) to R.drawable.progress_bar_fill_white
        )

        // Variable to hold the current selected drawable resource ID
        var selectedDrawableResId: Int? = null

        // Set click listeners for each seek bar to handle selection
        for ((seekBar, drawableResId) in seekBarMap) {
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // Optionally update something as the user slides the seek bar
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Highlight the selected drawable when touched
                    selectedDrawableResId = drawableResId
                    highlightSelectedDrawable(seekBarMap.keys, seekBar)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Handle stop tracking if needed
                }
            })
        }

        // Create and show the AlertDialog
        AlertDialog.Builder(this)
            .setTitle("Choose Drawable")
            .setView(dialogView)
            .setPositiveButton("Apply") { dialog, which ->
                // Apply the selected drawable
                selectedDrawableResId?.let { updateSeekBarDrawable(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Helper function to highlight the selected SeekBar and remove highlight from others
    private fun highlightSelectedDrawable(seekBars: Collection<SeekBar>, selectedSeekBar: SeekBar?) {
        for (seekBar in seekBars) {
            if (seekBar == selectedSeekBar) {
                // Highlight the selected SeekBar. This can be a border or different background
                seekBar.setBackgroundResource(R.drawable.selected_drawable_background)
            } else {
                // Remove highlight from all other SeekBars
                seekBar.background = null
            }
        }
    }



    private fun updateSeekBarDrawable(drawableResId: Int) {
        val intent = Intent(ACTION_CHANGE_OVERLAY_DRAWABLE)
        intent.putExtra(
            "com.example.accessibilitysvc.MyAccessibilityService.UPDATED_OVERLAY_DRAWABLE",
            drawableResId
        )
        sendBroadcast(intent)
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