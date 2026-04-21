package ru.nuxson.lifecalendar

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import org.json.JSONArray
import java.time.LocalDate

class CalendarWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return CalendarEngine()
    }

    inner class CalendarEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var visible = false
        private val prefs by lazy { getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE) }
        
        private var drawParams: CalendarDrawParams? = null
        private var cachedType = CalendarType.MONTH
        private var cachedEvents: List<CalendarEvent> = emptyList()

        private val accentPaint = Paint().apply { textSize = 38f; textAlign = Paint.Align.LEFT; isAntiAlias = true; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        private val secondaryPaint = Paint().apply { textSize = 32f; textAlign = Paint.Align.LEFT; isAntiAlias = true }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            updateCaches()
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        private fun updateCaches() {
            val accentStr = prefs.getString("accent_color", "#4CAF50") ?: "#4CAF50"
            val bgStr = prefs.getString("bg_color", "#000000") ?: "#000000"
            val futureStr = prefs.getString("future_color", "#222222") ?: "#222222"
            val weekendStr = prefs.getString("weekend_color", "#FF5252") ?: "#FF5252"
            val textStr = prefs.getString("text_color", "#FFFFFF") ?: "#FFFFFF"
            
            drawParams = CalendarDrawParams(
                accentColor = Color.parseColor(accentStr),
                bgColor = Color.parseColor(bgStr),
                futureColor = Color.parseColor(futureStr),
                weekendColor = Color.parseColor(weekendStr),
                textColor = Color.parseColor(textStr),
                birthDateStr = prefs.getString("birth_date", "2000-01-01") ?: "2000-01-01",
                lifeExpectancy = prefs.getInt("life_expectancy", 80).toFloat(),
                scale = prefs.getFloat("scale", 1.0f),
                offsetX = prefs.getFloat("offset_x", 0.5f),
                offsetY = prefs.getFloat("offset_y", 0.5f),
                showTitle = prefs.getBoolean("show_title", true),
                showStats = prefs.getBoolean("show_stats", true),
                showDates = prefs.getBoolean("show_dates", false),
                showDayNames = prefs.getBoolean("show_day_names", true)
            )

            val typeStr = prefs.getString("calendar_type", CalendarType.MONTH.name) ?: CalendarType.MONTH.name
            cachedType = try { CalendarType.valueOf(typeStr) } catch(e: Exception) { CalendarType.MONTH }
            
            val eventsJson = prefs.getString("events", "[]") ?: "[]"
            cachedEvents = parseEvents(eventsJson)

            accentPaint.color = drawParams!!.accentColor
            secondaryPaint.color = drawParams!!.textColor
        }

        private fun parseEvents(json: String): List<CalendarEvent> {
            return try {
                val arr = JSONArray(json)
                List(arr.length()) { i ->
                    val obj = arr.getJSONObject(i)
                    CalendarEvent(LocalDate.parse(obj.getString("date")), obj.getInt("color"), obj.optString("label"))
                }
            } catch (e: Exception) { emptyList() }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            updateCaches()
            draw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) draw()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            draw()
        }

        private fun draw() {
            if (!visible) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.let { 
                    it.drawColor(drawParams?.bgColor ?: Color.BLACK)
                    CalendarRenderer.draw(it, it.width.toFloat(), it.height.toFloat(), cachedType, LocalDate.now(), drawParams!!, cachedEvents)
                    if (drawParams?.showStats == true) {
                        drawStats(it)
                    }
                }
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        private fun drawStats(canvas: Canvas) {
            val today = LocalDate.now()
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            val startX = w * 0.12f
            
            when (cachedType) {
                CalendarType.MONTH -> {
                    val daysInMonth = today.month.length(today.isLeapYear)
                    val progress = (today.dayOfMonth.toFloat() / daysInMonth * 100).toInt()
                    drawStatsLine(canvas, startX, h * 0.85f, "$progress%", " ПРОЖИТО  •  ", "${daysInMonth - today.dayOfMonth}", " ДНЕЙ ОСТАЛОСЬ")
                }
                CalendarType.YEAR -> {
                    val totalDays = if (today.isLeapYear) 366 else 365
                    val progress = (today.dayOfYear.toFloat() / totalDays * 100).toInt()
                    drawStatsLine(canvas, startX, h * 0.9f, "$progress%", " ГОДА ПРОШЛО  •  ", "${totalDays - today.dayOfYear}", " ДНЕЙ ОСТАЛОСЬ")
                }
                CalendarType.LIFE -> {
                    val birthDate = LocalDate.parse(drawParams!!.birthDateStr)
                    val totalWeeks = drawParams!!.lifeExpectancy * 52
                    val weeksLived = java.time.temporal.ChronoUnit.WEEKS.between(birthDate, today).toInt()
                    val progress = (weeksLived.toFloat() / totalWeeks * 100).toInt()
                    drawStatsLine(canvas, startX, h * 0.95f, "$progress%", " ЖИЗНИ  •  ", "${(totalWeeks - weeksLived).toInt()}", " НЕДЕЛЬ ОСТАЛОСЬ")
                }
            }
        }

        private fun drawStatsLine(canvas: Canvas, startX: Float, y: Float, val1: String, lab1: String, val2: String, lab2: String) {
            var x = startX
            canvas.drawText(val1, x, y, accentPaint); x += accentPaint.measureText(val1)
            canvas.drawText(lab1, x, y, secondaryPaint); x += secondaryPaint.measureText(lab1)
            canvas.drawText(val2, x, y, accentPaint); x += accentPaint.measureText(val2)
            canvas.drawText(lab2, x, y, secondaryPaint)
        }
    }
}
