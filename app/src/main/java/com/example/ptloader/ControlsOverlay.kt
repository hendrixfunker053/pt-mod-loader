package com.example.ptloader

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/** Overlay multitouch con tus sprites. Con editMode=true se arrastran los controles para moverlos. */
class ControlsOverlay(
    ctx: Context,
    private var layout: ControlsLayout,
    private val onKey: (keyCode: Int, down: Boolean) -> Unit,
) : View(ctx) {

    var opacity = 0.85f
    var editMode = false
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.WHITE; strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
    }
    private val opts = BitmapFactory.Options().apply { inScaled = false }
    private val cache = HashMap<String, Bitmap?>()
    private val pressed = HashSet<Int>()
    private var joyId = -1
    private var stickX = 0f
    private var stickY = 0f
    private var sel = -2        // -1 joystick, >=0 botón, -2 nada
    private var dragging = false

    fun current() = layout
    fun setLayout(l: ControlsLayout) { layout = l; sel = -2; invalidate() }

    fun resizeSelected(delta: Float) {
        layout = if (sel == -1) layout.copy(joystick = layout.joystick?.let { it.copy(size = (it.size + delta).coerceIn(0.25f, 0.9f)) })
        else layout.copy(buttons = layout.buttons.mapIndexed { i, b ->
            if (i == sel) b.copy(size = (b.size + delta).coerceIn(0.10f, 0.35f)) else b
        })
        invalidate()
    }

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

    private fun joyRect(j: Joystick): RectF {
        val d = j.size * height; val w = d * 158f / 154f
        val cx = j.x * width; val cy = j.y * height
        return RectF(cx - w / 2, cy - d / 2, cx + w / 2, cy + d / 2)
    }

    override fun onDraw(c: Canvas) {
        paint.alpha = (opacity * 255).toInt()

        layout.joystick?.let { j ->
            val r = j.size * height / 2
            val rect = joyRect(j)
            bmp("spr_joystick_back")?.let { c.drawBitmap(it, null, rect, paint) }
            val ox = stickX * r * 0.5f; val oy = stickY * r * 0.5f
            bmp("spr_joystick_front")?.let {
                c.drawBitmap(it, null, RectF(rect.left + ox, rect.top + oy, rect.right + ox, rect.bottom + oy), paint)
            }
            if (editMode && sel == -1) c.drawRect(rect, selPaint)
        }

        layout.buttons.forEachIndexed { i, b ->
            val bm = bmp("spr_${b.sprite}_button") ?: return@forEachIndexed
            val half = bm.width / 2
            val x0 = if (b.keyCode in pressed) half else 0
            val rect = btnRect(b)
            c.drawBitmap(bm, Rect(x0, 0, x0 + half, bm.height), rect, paint)
            if (editMode && sel == i) c.drawRect(rect, selPaint)
        }
    }

    // ---------- Edición ----------
    private fun pick(x: Float, y: Float): Int {
        for (i in layout.buttons.indices.reversed()) if (btnRect(layout.buttons[i]).contains(x, y)) return i
        layout.joystick?.let { if (joyRect(it).contains(x, y)) return -1 }
        return -2
    }

    private fun editTouch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val p = pick(e.x, e.y)
                dragging = p != -2
                if (p != -2) sel = p
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> if (dragging && sel != -2) {
                val nx = (e.x / width).coerceIn(0.03f, 0.97f)
                val ny = (e.y / height).coerceIn(0.03f, 0.97f)
                layout = if (sel == -1) layout.copy(joystick = layout.joystick?.copy(x = nx, y = ny))
                else layout.copy(buttons = layout.buttons.mapIndexed { i, b -> if (i == sel) b.copy(x = nx, y = ny) else b })
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }

    // ---------- Juego ----------
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (editMode) return editTouch(e)
        val act = e.actionMasked
        val ending = act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL
        val upIdx = if (act == MotionEvent.ACTION_POINTER_UP) e.actionIndex else -1
        val joy = layout.joystick
        val jr = joy?.let { it.size * height / 2 } ?: 0f
        val jcx = (joy?.x ?: 0f) * width
        val jcy = (joy?.y ?: 0f) * height

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
                        r.inset(-r.width() * 0.04f, -r.height() * 0.04f)
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
