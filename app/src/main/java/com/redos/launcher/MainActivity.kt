package com.redos.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val backgroundColor = Color.rgb(8, 8, 10)
    private val red = Color.rgb(235, 35, 55)
    private val redDark = Color.rgb(105, 18, 30)
    private val grey = Color.rgb(48, 48, 54)
    private val greyLight = Color.rgb(68, 68, 76)
    private val white = Color.WHITE
    private val muted = Color.rgb(185, 185, 192)

    private lateinit var root: LinearLayout
    private lateinit var tiles: GridLayout
    private lateinit var batteryValue: TextView
    private lateinit var clockValue: TextView
    private lateinit var dateValue: TextView
    private lateinit var appList: List<AppEntry>
    private val handler = Handler(Looper.getMainLooper())
    private var tileAnimationIndex = 0

    private data class AppEntry(
        val label: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable
    )

    private val clockUpdater = object : Runnable {
        override fun run() {
            if (::clockValue.isInitialized) {
                clockValue.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            }
            if (::dateValue.isInitialized) {
                dateValue.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                    .format(Date()).replaceFirstChar { it.uppercase() }
            }
            handler.postDelayed(this, 1000L)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            if (level >= 0 && scale > 0 && ::batteryValue.isInitialized) {
                batteryValue.text = "${level * 100 / scale}%"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        appList = loadApps()
        showHome()
        handler.post(clockUpdater)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockUpdater)
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun loadApps(): List<AppEntry> {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .map {
                AppEntry(
                    it.loadLabel(packageManager).toString(),
                    it.activityInfo.packageName,
                    it.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 22): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun label(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun showHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(20), dp(24), dp(20), dp(14))
        }
        setContentView(root)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = label("REDOS", 14f, red, true)
        header.addView(brand, LinearLayout.LayoutParams(0, dp(34), 1f))
        header.addView(label("LIVE • TILES", 10f, muted, true))
        root.addView(header)

        clockValue = label(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            58f, white, true
        )
        root.addView(clockValue, LinearLayout.LayoutParams(-1, dp(76)))

        dateValue = label(
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            13f, muted
        )
        root.addView(dateValue, LinearLayout.LayoutParams(-1, dp(25)))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        tiles = GridLayout(this).apply {
            columnCount = 2
            useDefaultMargins = false
            setPadding(0, dp(18), 0, dp(14))
        }
        scroll.addView(tiles)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        addTile(
            title = "CALENDAR",
            subtitle = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date()).uppercase(),
            color = grey,
            height = 142,
            span = 1,
            icon = "▦",
            onClick = { openCalendar() }
        ) { box ->
            box.addView(label("YOUR DAY", 10f, muted, true))
            box.addView(label("Plan something\nwonderful.", 17f, white, true))
        }

        addTile(
            title = "WEATHER",
            subtitle = "OPEN WEATHER",
            color = redDark,
            height = 142,
            span = 1,
            icon = "☀",
            onClick = { openWeather() }
        ) { box ->
            box.addView(label("☀", 30f, white))
            box.addView(label("Tap to check forecast", 11f, Color.rgb(255, 210, 215)))
        }

        addTile(
            title = "PHOTOS",
            subtitle = "YOUR GALLERY",
            color = greyLight,
            height = 112,
            span = 1,
            icon = "▧",
            onClick = { openPhotos() }
        ) { box ->
            box.addView(label("Memories", 19f, white, true))
            box.addView(label("Open your photos", 11f, muted))
        }

        addTile(
            title = "MUSIC",
            subtitle = "NOW PLAYING",
            color = red,
            height = 112,
            span = 1,
            icon = "♫",
            onClick = { openMusic() }
        ) { box ->
            box.addView(label("Play something.", 18f, white, true))
            box.addView(label("Open music player", 11f, Color.rgb(255, 225, 228)))
        }

        addTile(
            title = "BATTERY",
            subtitle = "DEVICE STATUS",
            color = grey,
            height = 112,
            span = 1,
            icon = "ϟ",
            onClick = { showBatteryDetails() }
        ) { box ->
            batteryValue = label("--%", 28f, white, true)
            box.addView(batteryValue)
            box.addView(label("Battery level", 11f, muted))
        }

        addTile(
            title = "ALL APPS",
            subtitle = "${appList.size} APPLICATIONS",
            color = Color.rgb(34, 34, 39),
            height = 112,
            span = 1,
            icon = "⊞",
            onClick = { showDrawer() }
        ) { box ->
            box.addView(label("Explore", 20f, white, true))
            box.addView(label("Open app library  ›", 11f, muted))
        }

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(27, 27, 32), 25)
            setPadding(dp(8), dp(5), dp(8), dp(5))
        }
        val dockApps = appList.take(3)
        dockApps.forEach { app ->
            val item = ImageView(this).apply {
                setImageDrawable(app.icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = app.label
                setOnClickListener { launch(app.packageName) }
            }
            dock.addView(item, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        val appButton = TextView(this).apply {
            text = "APPS"
            textSize = 10f
            setTextColor(red)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setOnClickListener { showDrawer() }
        }
        dock.addView(appButton, LinearLayout.LayoutParams(0, dp(44), 1f))
        val dockParams = LinearLayout.LayoutParams(-1, dp(58))
        dockParams.topMargin = dp(8)
        root.addView(dock, dockParams)
    }

    private fun addTile(
        title: String,
        subtitle: String,
        color: Int,
        height: Int,
        span: Int,
        icon: String,
        onClick: () -> Unit,
        content: (LinearLayout) -> Unit
    ) {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(color, 20)
            setPadding(dp(15), dp(12), dp(15), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(label(title, 10f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(22), 1f))
        top.addView(label(icon, 19f, Color.WHITE, true))
        tile.addView(top)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tile.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        content(body)
        tile.addView(label(subtitle, 9f, Color.rgb(220, 220, 225), true))

        // Windows Phone-inspired tile motion with a subtle iOS-style press response.
        tile.cameraDistance = 12000f * resources.displayMetrics.density
        tile.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.96f).scaleY(0.96f)
                        .setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(160)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
            false
        }

        val animationDelay = (tileAnimationIndex++ * 650L) + 3500L
        tile.postDelayed(object : Runnable {
            override fun run() {
                if (!isFinishing && tile.isAttachedToWindow) {
                    tile.animate()
                        .rotationY(90f)
                        .setDuration(180)
                        .withEndAction {
                            tile.rotationY = -90f
                            tile.animate()
                                .rotationY(0f)
                                .setDuration(220)
                                .setInterpolator(DecelerateInterpolator())
                                .start()
                        }
                        .start()
                    tile.postDelayed(this, 9000L + (tileAnimationIndex % 3) * 1200L)
                }
            }
        }, animationDelay)

        val params = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED),
            GridLayout.spec(GridLayout.UNDEFINED, span, 1f)
        ).apply {
            width = 0
            height = dp(height)
            setMargins(dp(4), dp(4), dp(4), dp(4))
        }
        tiles.addView(tile, params)
    }

    private fun showDrawer() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(20), dp(26), dp(20), dp(12))
        }
        setContentView(page)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("APP LIBRARY", 20f, white, true),
            LinearLayout.LayoutParams(0, dp(48), 1f))
        val close = label("✕", 22f, red, true)
        close.setOnClickListener { showHome() }
        header.addView(close)
        page.addView(header)

        val search = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(Color.GRAY)
            setTextColor(white)
            textSize = 15f
            setSingleLine(true)
            background = rounded(grey, 16)
            setPadding(dp(14), 0, dp(14), 0)
        }
        val searchParams = LinearLayout.LayoutParams(-1, dp(50))
        searchParams.topMargin = dp(12)
        page.addView(search, searchParams)

        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply {
            columnCount = 4
            setPadding(0, dp(16), 0, 0)
        }
        scroll.addView(grid)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        fun render(query: String) {
            grid.removeAllViews()
            appList.filter { it.label.contains(query, true) }.forEach { app ->
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(3), dp(6), dp(3), dp(6))
                    setOnClickListener { launch(app.packageName) }
                }
                val icon = ImageView(this).apply {
                    setImageDrawable(app.icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                item.addView(icon, LinearLayout.LayoutParams(dp(46), dp(46)))
                val name = label(app.label, 10f, muted)
                name.gravity = Gravity.CENTER
                name.maxLines = 1
                name.ellipsize = android.text.TextUtils.TruncateAt.END
                item.addView(name, LinearLayout.LayoutParams(-1, dp(26)))
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
                ).apply {
                    width = 0
                    height = dp(88)
                }
                grid.addView(item, params)
            }
        }
        render("")
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun launch(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
    }

    private fun openCalendar() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        try { startActivity(intent) } catch (_: Exception) {
            toast("No calendar app found")
        }
    }

    private fun openWeather() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_WEATHER)
        try { startActivity(intent) } catch (_: Exception) {
            toast("No weather app available")
        }
    }

    private fun openPhotos() {
        val intent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try { startActivity(intent) } catch (_: Exception) {
            toast("No gallery app found")
        }
    }

    private fun openMusic() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
        try { startActivity(intent) } catch (_: Exception) {
            toast("No music app found")
        }
    }

    private fun showBatteryDetails() {
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
        toast(if (percent >= 0) "Battery: $percent%" else "Battery information unavailable")
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
