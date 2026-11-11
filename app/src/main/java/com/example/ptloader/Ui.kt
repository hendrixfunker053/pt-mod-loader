package com.example.ptloader

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object Prefs {
    fun get(c: Context): SharedPreferences = c.getSharedPreferences("ptloader", Context.MODE_PRIVATE)
}

/** Estilo del launcher: tarjetas blancas con borde rosa, botones rosa claro, pasos numerados. */
object Ui {
    val PINK = 0xFFFF9A9A.toInt()
    val PINK_LIGHT = 0xFFFFCCCC.toInt()
    val INK = 0xFF1B1B1B.toInt()

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    /** Fuente opcional: pon un .ttf en res/font/ui_font.ttf y se usa sola. */
    fun font(c: Context): Typeface? {
        val id = c.resources.getIdentifier("ui_font", "font", c.packageName)
        return if (id == 0) null else try { c.resources.getFont(id) } catch (e: Exception) { null }
    }

    fun box(c: Context, fill: Int, stroke: Int, radius: Int = 22, strokeW: Int = 3) = GradientDrawable().apply {
        setColor(fill); cornerRadius = dp(c, radius).toFloat(); setStroke(dp(c, strokeW), stroke)
    }

    fun lp(c: Context, top: Int = 0, w: Int = ViewGroup.LayoutParams.MATCH_PARENT,
           h: Int = ViewGroup.LayoutParams.WRAP_CONTENT, weight: Float = 0f, start: Int = 0) =
        LinearLayout.LayoutParams(w, h, weight).apply { topMargin = dp(c, top); marginStart = dp(c, start) }

    fun card(c: Context) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        background = box(c, Color.WHITE, PINK, 26, 3)
        setPadding(dp(c, 18), dp(c, 14), dp(c, 18), dp(c, 14))
    }

    fun text(c: Context, s: String, sp: Float = 14f, bold: Boolean = false) = TextView(c).apply {
        text = s; textSize = sp; setTextColor(INK)
        typeface = font(c)?.let { Typeface.create(it, if (bold) Typeface.BOLD else Typeface.NORMAL) }
            ?: if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        setLineSpacing(0f, 1.25f)
    }

    fun button(c: Context, s: String, onClick: () -> Unit) = Button(c).apply {
        text = s; textSize = 15f; isAllCaps = false; stateListAnimator = null
        minHeight = 0; minimumHeight = 0
        setPadding(dp(c, 14), dp(c, 14), dp(c, 14), dp(c, 14))
        typeface = font(c) ?: Typeface.DEFAULT
        setEnabledStyle(this, true)
        setOnClickListener { onClick() }
    }

    fun setEnabledStyle(b: Button, enabled: Boolean) {
        b.isEnabled = enabled
        b.background = box(b.context, if (enabled) PINK_LIGHT else 0xFFFFE3E3.toInt(), PINK, 22, 3)
        b.setTextColor(if (enabled) INK else 0xFF999999.toInt())
    }

    fun step(c: Context, n: Int) = TextView(c).apply {
        text = n.toString(); textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(PINK) }
        layoutParams = LinearLayout.LayoutParams(dp(c, 38), dp(c, 38))
    }

    /** Pantalla completa, también bajo el notch. */
    fun fullscreen(a: Activity) {
        if (Build.VERSION.SDK_INT >= 28) {
            a.window.attributes = a.window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        a.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}
