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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferences
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)

        // Check if it's the first launch
        if (sharedPref.getBoolean("isFirstLaunch", true)) {
            // If first launch, update the flag
            with(sharedPref.edit()) {
                putBoolean("isFirstLaunch", false)
                apply()
            }

            // Proceed with MainActivity's original onCreate code
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.buttonStart.setOnClickListener {
                startBrightnessBarActivity()
            }

        } else {
            // If not first launch, redirect to BrightnessBarActivity
            startBrightnessBarActivity()
            finish() // Close MainActivity
        }
    }

    private fun startBrightnessBarActivity() {
        val intent = Intent(this, BrightnessBarActivity::class.java)
        startActivity(intent)
    }
}
