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

    private fun startBrightnessBarActivity() {
        val intent = Intent(this, BrightnessBarActivity::class.java)
        startActivity(intent)
    }
}
