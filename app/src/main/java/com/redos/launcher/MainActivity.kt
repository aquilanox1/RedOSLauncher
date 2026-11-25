
package com.redos.launcher

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val bg = Color.rgb(8, 8, 10)
    private val panel = Color.rgb(25, 25, 29)
    private val red = Color.rgb(235, 35, 55)

    private lateinit var root: LinearLayout
    private lateinit var appGrid: GridLayout
    private lateinit var search: EditText
    private lateinit var drawer: LinearLayout

    private var allApps: List<AppEntry> = emptyList()

    data class AppEntry(
        val label: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

        allApps = loadApps()
        showHome()
    }

    private fun loadApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN, null)
            .addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
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

    private fun rounded(
        color: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }

            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun showHome() {

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(22),
                dp(26),
                dp(22),
                dp(18)
            )
            setBackgroundColor(bg)
        }

        setContentView(root)

        // TOP BAR

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = text("REDOS", 13f, red, true)

        top.addView(
            brand,
            LinearLayout.LayoutParams(0, dp(34), 1f)
        )

        top.addView(
            text(
                SimpleDateFormat(
                    "EEE, d MMM",
                    Locale.getDefault()
                ).format(Date()).uppercase(),
                12f,
                Color.LTGRAY
            )
        )

        root.addView(top)

        // CLOCK

        val clock = text(
            SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date()),
            54f,
            Color.WHITE,
            true
        )

        root.addView(
            clock,
            LinearLayout.LayoutParams(-1, dp(88))
        )

        root.addView(
            text("MAKE IT YOURS.", 11f, Color.GRAY, true)
        )

        // SEARCH BAR

        val searchWrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, dp(18).toFloat())
            setPadding(dp(15), 0, dp(12), 0)
        }

        search = EditText(this).apply {
            hint = "Search apps"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 15f
            setSingleLine(true)
            background = null

            addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filterApps(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        searchWrap.addView(
            search,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )

        searchWrap.addView(
            text("⌕", 25f, red, true)
        )

        val searchParams =
            LinearLayout.LayoutParams(-1, dp(54))

        searchParams.topMargin = dp(24)

        root.addView(searchWrap, searchParams)

        // APP LIBRARY HEADER

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(22), 0, dp(12))
        }

        row.addView(
            text("APP LIBRARY", 12f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(30), 1f)
        )

        val all = text("VIEW ALL  ›", 11f, red, true)

        all.setOnClickListener {
            showDrawer()
        }

        row.addView(all)
        root.addView(row)

        // APP GRID

        appGrid = GridLayout(this).apply {
            columnCount = 4
            useDefaultMargins = false
        }

        root.addView(
            appGrid,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        populateGrid(allApps.take(8))

        // DOCK

        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER

            background = rounded(
                Color.rgb(30, 20, 23),
                dp(26).toFloat()
            )

            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )
        }

        val favorites = allApps.take(4)

        favorites.forEach { app ->
            dock.addView(
                appTile(app, true),
                LinearLayout.LayoutParams(0, dp(62), 1f)
            )
        }

        val dockParams =
            LinearLayout.LayoutParams(-1, dp(78))

        dockParams.topMargin = dp(8)

        root.addView(dock, dockParams)

        // SWIPE UP GESTURE

        root.setOnTouchListener(
            object : View.OnTouchListener {

                var downY = 0f

                override fun onTouch(
                    v: View?,
                    e: android.view.MotionEvent
                ): Boolean {

                    when (e.action) {

                        android.view.MotionEvent.ACTION_DOWN -> {
                            downY = e.y
                            return true
                        }

                        android.view.MotionEvent.ACTION_UP -> {
                            if (downY - e.y > dp(55)) {
                                showDrawer()
                            }
                            return true
                        }
                    }

                    return true
                }
            }
        )
    }

    // POPULATE HOME GRID

    private fun populateGrid(apps: List<AppEntry>) {

        appGrid.removeAllViews()

        apps.forEach { app ->

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(94)

                columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED,
                    1f
                )
            }

            appGrid.addView(
                appTile(app, false),
                params
            )
        }
    }

    // APPLICATION ICON

    private fun appTile(
        app: AppEntry,
        compact: Boolean
    ): View {

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            setPadding(
                dp(3),
                dp(4),
                dp(3),
                dp(4)
            )

            setOnClickListener {
                launch(app.packageName)
            }
        }

        val icon = ImageView(this).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val iconSize = if (compact) 34 else 48

        item.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(iconSize),
                dp(iconSize)
            )
        )

        if (!compact) {

            val label = text(
                app.label,
                10f,
                Color.LTGRAY
            )

            label.gravity = Gravity.CENTER
            label.maxLines = 1
            label.ellipsize =
                android.text.TextUtils.TruncateAt.END

            item.addView(
                label,
                LinearLayout.LayoutParams(-1, dp(26))
            )
        }

        return item
    }

    // APP DRAWER

    private fun showDrawer() {

        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(20),
                dp(28),
                dp(20),
                dp(12)
            )

            setBackgroundColor(bg)
        }

        setContentView(drawer)

        // HEADER

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            text("ALL APPS", 20f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        val close = text("✕", 20f, red, true)

        close.setOnClickListener {
            showHome()
        }

        header.addView(close)
        drawer.addView(header)

        // DRAWER SEARCH

        val input = EditText(this).apply {
            hint = "Find an app"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)

            background = rounded(
                panel,
                dp(14).toFloat()
            )

            setPadding(
                dp(14),
                0,
                dp(14),
                0
            )

            setSingleLine(true)
        }

        val inputParams =
            LinearLayout.LayoutParams(-1, dp(50))

        inputParams.topMargin = dp(12)

        drawer.addView(input, inputParams)

        // SCROLLABLE APP GRID

        val scroll = ScrollView(this)

        val grid = GridLayout(this).apply {
            columnCount = 4
            setPadding(0, dp(18), 0, 0)
        }

        scroll.addView(grid)

        drawer.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        // FILTER APPS

        fun render(query: String) {

            grid.removeAllViews()

            allApps
                .filter { it.label.contains(query, true) }
                .forEach { app ->

                    val params = GridLayout.LayoutParams().apply {
                        width = 0
                        height = dp(94)

                        columnSpec = GridLayout.spec(
                            GridLayout.UNDEFINED,
                            1f
                        )
                    }

                    grid.addView(
                        appTile(app, false),
                        params
                    )
                }
        }

        render("")

        input.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    render(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    // HOME SEARCH FILTER

    private fun filterApps(query: String) {

        if (::appGrid.isInitialized) {

            populateGrid(
                allApps
                    .filter { it.label.contains(query, true) }
                    .take(8)
            )
        }
    }

    // LAUNCH APPLICATION

    private fun launch(pkg: String) {

        val intent =
            packageManager.getLaunchIntentForPackage(pkg)

        if (intent != null) {
            startActivity(intent)
        }
    }

    // DP CONVERSION

    private fun dp(v: Int): Int {
        return (
            v * resources.displayMetrics.density
        ).toInt()
    }
}
