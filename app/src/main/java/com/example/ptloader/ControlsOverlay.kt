package com.example.ptloader

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/** Overlay multitouch con tus sprites: joystick de pizza + botones de 2 frames (suelto | pulsado). */
class ControlsOverlay(
    ctx: Context,
    private var layout: ControlsLayout,
    private val onKey: (keyCode: Int, down: Boolean) -> Unit,
) : View(ctx) {

    var opacity = 0.85f
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val opts = BitmapFactory.Options().apply { inScaled = false }
    private val cache = HashMap<String, Bitmap?>()
    private val pressed = HashSet<Int>()
    private var joyId = -1
    private var stickX = 0f
    private var stickY = 0f

    fun setLayout(l: ControlsLayout) { layout = l; invalidate() }

    private fun bmp(name: String): Bitmap? {
        if (!cache.containsKey(name)) {
            val id = resources.getIdentifier(name, "drawable", context.packageName)
            cache[name] = if (id == 0) null else BitmapFactory.decodeResource(resources, id, opts)
        }
        return cache[name]
    }

    private fun btnRect(b: TouchButton): RectF {
        val h = b.size * height; val w = h * 95f / 102f
        val cx = b.x * width; val cy = b.y * height
        return RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
    }

    override fun onDraw(c: Canvas) {
        paint.alpha = (opacity * 255).toInt()

        layout.joystick?.let { j ->
            val d = j.size * height; val r = d / 2; val w = d * 158f / 154f
            val cx = j.x * width; val cy = j.y * height
            bmp("spr_joystick_back")?.let {
                c.drawBitmap(it, null, RectF(cx - w / 2, cy - r, cx + w / 2, cy + r), paint)
            }
            val ox = stickX * r * 0.5f; val oy = stickY * r * 0.5f
            bmp("spr_joystick_front")?.let {
                c.drawBitmap(it, null, RectF(cx - w / 2 + ox, cy - r + oy, cx + w / 2 + ox, cy + r + oy), paint)
            }
        }

        for (b in layout.buttons) {
            val bm = bmp("spr_${b.sprite}_button") ?: continue
            val half = bm.width / 2
            val x0 = if (b.keyCode in pressed) half else 0
            c.drawBitmap(bm, Rect(x0, 0, x0 + half, bm.height), btnRect(b), paint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val act = e.actionMasked
        val ending = act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL
        val upIdx = if (act == MotionEvent.ACTION_POINTER_UP) e.actionIndex else -1
        val joy = layout.joystick
        val jr = joy?.let { it.size * height / 2 } ?: 0f
        val jcx = (joy?.x ?: 0f) * width
        val jcy = (joy?.y ?: 0f) * height

        // El joystick se "engancha" al dedo que toca dentro de su zona
        if (joy != null && joyId == -1 && (act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN)) {
            val i = e.actionIndex
            if (hypot(e.getX(i) - jcx, e.getY(i) - jcy) <= jr * 1.3f) joyId = e.getPointerId(i)
        }

        val now = HashSet<Int>()
        var sx = 0f; var sy = 0f
        if (!ending) {
            for (i in 0 until e.pointerCount) {
                if (i == upIdx) continue
                val x = e.getX(i); val y = e.getY(i)
                if (e.getPointerId(i) == joyId && joy != null) {
                    var nx = (x - jcx) / jr; var ny = (y - jcy) / jr
                    val len = hypot(nx, ny)
                    if (len > 1f) { nx /= len; ny /= len }
                    sx = nx; sy = ny
                    if (nx < -joy.deadzone) now.add(joy.left)
                    if (nx > joy.deadzone) now.add(joy.right)
                    if (ny < -joy.deadzone) now.add(joy.up)
                    if (ny > joy.deadzone) now.add(joy.down)
                } else {
                    for (b in layout.buttons) {
                        val r = btnRect(b)
                        r.inset(-r.width() * 0.1f, -r.height() * 0.1f)
                        if (r.contains(x, y)) now.add(b.keyCode)
                    }
                }
            }
        }
        if (ending || (upIdx != -1 && e.getPointerId(upIdx) == joyId)) joyId = -1
        stickX = sx; stickY = sy

        (now - pressed).forEach { onKey(it, true) }
        (pressed - now).forEach { onKey(it, false) }
        pressed.clear(); pressed.addAll(now)
        invalidate()
        return true
    }
}
