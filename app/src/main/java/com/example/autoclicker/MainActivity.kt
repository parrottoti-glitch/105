package com.example.autoclicker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("cfg", MODE_PRIVATE) }

    private lateinit var fInterval: EditText
    private lateinit var fCount: EditText
    private lateinit var fH: EditText
    private lateinit var fM: EditText
    private lateinit var fS: EditText
    private lateinit var fMs: EditText
    private lateinit var fPre: EditText
    private lateinit var swSched: Switch
    private lateinit var tvSvc: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun EditText.num(def: Long): Long = text.toString().trim().toLongOrNull() ?: def

    private fun label(t: String) = TextView(this).apply {
        text = t
        setTextColor(0xFFB0B0C0.toInt())
        textSize = 14f
        setPadding(0, dp(16), 0, dp(4))
    }

    private fun field(hint: String, v: Long) = EditText(this).apply {
        setText(v.toString())
        this.hint = hint
        inputType = InputType.TYPE_CLASS_NUMBER
        setTextColor(Color.WHITE)
        setHintTextColor(0xFF777788.toInt())
        gravity = Gravity.CENTER
        textSize = 18f
    }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        setOnClickListener { onClick() }
    }

    private fun col(title: String, f: EditText) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(TextView(this@MainActivity).apply {
            text = title
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFF8888A0.toInt())
        })
        addView(f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        root.addView(TextView(this).apply {
            text = "اتوکلیکر"
            textSize = 28f
            setTextColor(Color.WHITE)
        })

        tvSvc = TextView(this).apply { textSize = 15f; setPadding(0, dp(12), 0, dp(4)) }
        root.addView(tvSvc)
        root.addView(button("فعال‌سازی سرویس دسترسی") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })

        root.addView(label("فاصله‌ی بین کلیک‌ها (میلی‌ثانیه)"))
        fInterval = field("100", prefs.getLong("interval", 100L))
        root.addView(fInterval)

        root.addView(label("تعداد ضربه‌ها (۰ = بی‌نهایت)"))
        fCount = field("0", prefs.getInt("count", 0).toLong())
        val countRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        countRow.addView(button("−") {
            fCount.setText(maxOf(0L, fCount.num(0) - 1).toString())
        })
        countRow.addView(fCount, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        countRow.addView(button("+") {
            fCount.setText((fCount.num(0) + 1).toString())
        })
        root.addView(countRow)

        root.addView(label("زمان‌بندی شروع"))
        swSched = Switch(this).apply {
            text = "شروع خودکار در ساعت مشخص"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("sched", false)
        }
        root.addView(swSched)

        fH = field("ساعت", prefs.getInt("h", 8).toLong())
        fM = field("دقیقه", prefs.getInt("m", 30).toLong())
        fS = field("ثانیه", prefs.getInt("s", 0).toLong())
        fMs = field("ms", prefs.getInt("ms", 50).toLong())
        val timeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val lp = { LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        timeRow.addView(col("ساعت", fH), lp())
        timeRow.addView(col("دقیقه", fM), lp())
        timeRow.addView(col("ثانیه", fS), lp())
        timeRow.addView(col("میلی‌ثانیه", fMs), lp())
        root.addView(timeRow)

        root.addView(label("پیش‌دستی / جبران تأخیر (میلی‌ثانیه، کلیک زودتر شروع می‌شود)"))
        fPre = field("0", prefs.getLong("pre", 0L))
        root.addView(fPre)

        root.addView(View(this), LinearLayout.LayoutParams(1, dp(20)))
        root.addView(button("نمایش نوار شناور و هدف") {
            val s = ClickService.instance
            if (s == null) {
                Toast.makeText(this, "اول سرویس دسترسی را فعال کن", Toast.LENGTH_LONG).show()
            } else {
                save()
                s.showOverlay()
                moveTaskToBack(true)
            }
        })

        root.addView(TextView(this).apply {
            text = "دایره‌ی قرمز را روی نقطه‌ی دلخواه ببر و با دکمه‌ی ▶ شروع کن."
            textSize = 13f
            setTextColor(0xFF8888A0.toInt())
            setPadding(0, dp(12), 0, 0)
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(0xFF0B0B0F.toInt())
            isFillViewport = true
            addView(root)
        })
    }

    private fun save() {
        prefs.edit()
            .putLong("interval", fInterval.num(100).coerceAtLeast(1))
            .putInt("count", fCount.num(0).toInt().coerceAtLeast(0))
            .putBoolean("sched", swSched.isChecked)
            .putInt("h", fH.num(0).toInt().coerceIn(0, 23))
            .putInt("m", fM.num(0).toInt().coerceIn(0, 59))
            .putInt("s", fS.num(0).toInt().coerceIn(0, 59))
            .putInt("ms", fMs.num(0).toInt().coerceIn(0, 999))
            .putLong("pre", fPre.num(0).coerceAtLeast(0))
            .apply()
    }

    override fun onResume() {
        super.onResume()
        val on = ClickService.instance != null
        tvSvc.text = if (on) "سرویس دسترسی فعال است ✓" else "سرویس دسترسی غیرفعال است ✗"
        tvSvc.setTextColor(if (on) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt())
    }

    override fun onPause() {
        super.onPause()
        save()
    }
}
