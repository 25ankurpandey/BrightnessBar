package com.example.brightnessbar

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import com.example.brightnessbar.Constants.ACTION_CHANGE_OVERLAY_VISIBILITY
import com.example.brightnessbar.Constants.ACTION_TOGGLE_OVERLAY


class MyAccessibilityService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var gestureDetector: GestureDetector
    private lateinit var vibrator: Vibrator
    private var isLongPressing = false
    private var lastMoveTime: Long = 0
    private var lastMoveX: Float = 0f
    private var segmentsMoved: Int = 0
    private val minSegmentsBeforeFeedback = 3
    private lateinit var brightnessSeekBar: SeekBar
    private var lastSegmentIndex: Int = -1
    private var numberOfSegments: Int = 50
    private var seekbarVisibility: Boolean = true
    private val toggleOverlayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action.equals(ACTION_TOGGLE_OVERLAY)) {
                val show = intent.getBooleanExtra(
                    "com.example.accessibilitysvc.MyAccessibilityService.EXTRA_OVERLAY_STATE", true
                )
                if (show) {
                    initializeView()
                } else {
                    removeOverlayView()
                }
            } else if (intent.action.equals(ACTION_CHANGE_OVERLAY_VISIBILITY)) {
                val isVisible = intent.getBooleanExtra(
                    "com.example.accessibilitysvc.MyAccessibilityService.EXTRA_OVERLAY_VISIBILITY",
                    true
                )
                seekbarVisibility = isVisible

            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Register the BroadcastReceiver
        val filter = IntentFilter()
        filter.addAction(ACTION_TOGGLE_OVERLAY)
        filter.addAction(ACTION_CHANGE_OVERLAY_VISIBILITY)
        registerReceiver(toggleOverlayReceiver, filter, RECEIVER_EXPORTED)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val sharedPrefs = getSharedPreferences("OverlaySettings", Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean("OverlayEnabled", true)) {
            initializeView()
        }
    }

    private fun removeOverlayView() {
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView)
            overlayView = null // Ensure the reference is cleared
        }
    }

    private fun initializeView() {
        // Inflate the overlay layout
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        if (overlayView != null) {
            // Initialize the SeekBar from the overlay layout
            brightnessSeekBar = overlayView!!.findViewById(R.id.brightness_slider) as SeekBar
            brightnessSeekBar.max = 255
            brightnessSeekBar.visibility = View.INVISIBLE

            val sharedPrefs = getSharedPreferences("OverlaySettings", Context.MODE_PRIVATE)
            seekbarVisibility = sharedPrefs.getBoolean("OverlayVisible", true)

            val statusBarHeight = getStatusBarHeight()

            // Set the FrameLayout's height to status bar height
            val layoutParamsFrame = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, statusBarHeight
            )
            overlayView!!.layoutParams = layoutParamsFrame

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                statusBarHeight,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSPARENT
            )

            layoutParams.gravity = Gravity.TOP or Gravity.START
            layoutParams.x = 0
            layoutParams.y = 0

            Log.d("+++++++++++++++++++++++++++++++++++++++++", seekbarVisibility.toString())

            gestureDetector =
                GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onLongPress(e: MotionEvent) {
                        isLongPressing = true
                        performHapticFeedback(50)
                        // Initialize the SeekBar and make it visible on long press
                        if (seekbarVisibility) {
                            brightnessSeekBar.visibility =
                                View.VISIBLE // Make the SeekBar visible on long press
                        }
                    }
                })


// Define padding values (in pixels)
            val padding = 200 // for example, 30 pixels on each side

            overlayView?.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                if (isLongPressing) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Reset the segment movement counter on a new touch
                            segmentsMoved = 0
                            lastSegmentIndex =
                                -1  // Also reset the last segment index to ensure fresh calculation
                            lastMoveTime = System.currentTimeMillis()
                            lastMoveX = event.rawX
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val currentTime = System.currentTimeMillis()
                            val timeDelta =
                                currentTime - lastMoveTime  // Time difference since last move
                            val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                            val adjustedWidth =
                                screenWidth - 2 * padding  // Adjust width for padding

                            // Adjust xPosition to be within the padded area
                            val xPosition = event.rawX.coerceIn(
                                padding.toFloat(), (screenWidth - padding).toFloat()
                            )

                            // Adjust progress calculation for padding, ensuring it's within SeekBar bounds
                            val progress =
                                ((xPosition - padding) / adjustedWidth * brightnessSeekBar.max).toInt()
                                    .coerceIn(0, brightnessSeekBar.max)
                            val segmentIndex = progress / (brightnessSeekBar.max / numberOfSegments)

                            // Update system brightness
                            brightnessSeekBar.progress = progress
                            setSystemBrightness(brightnessSeekBar.progress)

                            // Calculate speed for haptic feedback intensity
                            val distanceDelta = Math.abs(xPosition - lastMoveX)
                            val speed = if (timeDelta > 0) distanceDelta / timeDelta else 0f

                            if (segmentIndex != lastSegmentIndex) {
                                segmentsMoved++  // Increment the segment moved counter
                                lastSegmentIndex = segmentIndex  // Update the last segment index

                                // Trigger haptic feedback after moving minSegmentsBeforeFeedback
                                if (segmentsMoved >= minSegmentsBeforeFeedback) {
                                    performHapticFeedback(calculateHapticIntensity(speed))
                                }
                            }

                            // Update tracking variables for speed calculation
                            lastMoveTime = currentTime
                            lastMoveX = xPosition
                        }

                        MotionEvent.ACTION_UP -> {
                            brightnessSeekBar.visibility = View.INVISIBLE
                            isLongPressing = false
                            lastSegmentIndex = -1  // Reset the last segment index
                            segmentsMoved = 0  // Reset the segments moved counter
                        }
                    }
                }
                false // Consume the touch event
            }

            windowManager?.addView(overlayView, layoutParams)
        } else {
            Log.e("MyAccessibilityService", "Failed to inflate overlay layout.")
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun calculateHapticIntensity(speed: Float): Int {
        return when {
            speed < 0.5 -> 80  // Slower sliding -> standard intensity
            speed < 1 -> 200  // Medium sliding -> strong intensity
            else -> 255  // Faster sliding -> weaker intensity (or adjust as needed)
        }
    }


    private fun setSystemBrightness(brightnessLevel: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(applicationContext)) {
                try {
                    // Directly use the brightnessLevel as it matches the system's range
                    Settings.System.putInt(
                        contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessLevel
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this, "Need permission to change settings!", Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Prompt the user to grant permission
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } else {
            // For older versions, directly update system brightness
            try {
                Settings.System.putInt(
                    contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessLevel
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to change settings!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentBrightness(): Int {
        return try {
            // Get the current system brightness setting
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            -1 // Return a default or error value if the current brightness level is unknown
        }
    }


    private fun performHapticFeedback(intensity: Int) {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        50, intensity
                    )
                )  // Vibrate with calculated intensity
            } else {
                vibrator.vibrate(50)  // Older versions will use a fixed intensity
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Handle accessibility events here

        // Check for the type of accessibility event to log appropriate information
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> Log.d(
                "AccessibilityEvent", "View Clicked: " + event.contentDescription
            )

            AccessibilityEvent.TYPE_VIEW_FOCUSED -> Log.d(
                "AccessibilityEvent", "View Focused: " + event.contentDescription
            )

            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> Log.d(
                "AccessibilityEvent", "View Long Clicked: " + event.contentDescription
            )
        }

        // Log the package name for context, if needed

        // Log the package name for context, if needed
        val packageName: CharSequence = event.packageName
        Log.d("AccessibilityEvent", "Package Name: $packageName")
    }

    override fun onInterrupt() {
        // Handle interruptions here
    }

    override fun onDestroy() {
        unregisterReceiver(toggleOverlayReceiver)  // Unregister the receiver
//        if (isOverlayVisible && overlayView != null) {
//            windowManager.removeView(overlayView)  // Remove the overlay view
//        }
        super.onDestroy()
    }
}