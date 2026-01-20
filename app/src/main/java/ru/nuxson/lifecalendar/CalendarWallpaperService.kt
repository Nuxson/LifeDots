package ru.nuxson.lifecalendar

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class CalendarWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return CalendarEngine()
    }

    inner class CalendarEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val titlePaint = Paint().apply {
            textSize = 70f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        private val miniLabelPaint = Paint().apply {
            textSize = 24f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        private val sideLabelPaint = Paint().apply {
            textSize = 24f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        private val accentPaint = Paint().apply {
            textSize = 38f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        private val secondaryPaint = Paint().apply {
            textSize = 32f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        private val livedDayPaint = Paint().apply { isAntiAlias = true }
        private val futureDayPaint = Paint().apply { isAntiAlias = true }
        private val todayPaint = Paint().apply { color = Color.RED; isAntiAlias = true }

        private var visible = false
        private val prefs by lazy { getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE) }

        // Cached preference values
        private var cachedBgColor = Color.BLACK
        private var cachedAccentColor = Color.parseColor("#4CAF50")
        private var cachedFutureColor = Color.parseColor("#222222")
        private var cachedTextColor = Color.WHITE
        private var cachedCalendarType = CalendarType.MONTH
        private var cachedBirthDateStr = "2000-01-01"
        private var cachedLifeExpectancy = 80
        private var cachedScale = 1.0f
        private var cachedOffsetX = 0.5f
        private var cachedOffsetY = 0.5f
        private var cachedShowTitle = true
        private var cachedShowStats = true

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
            cachedBgColor = Color.parseColor(prefs.getString("bg_color", "#000000"))
            cachedAccentColor = Color.parseColor(prefs.getString("accent_color", "#4CAF50"))
            cachedFutureColor = Color.parseColor(prefs.getString("future_color", "#222222"))
            cachedTextColor = Color.parseColor(prefs.getString("text_color", "#FFFFFF"))
            
            val typeStr = prefs.getString("calendar_type", CalendarType.MONTH.name) ?: CalendarType.MONTH.name
            cachedCalendarType = try { CalendarType.valueOf(typeStr) } catch(e: Exception) { CalendarType.MONTH }
            
            cachedBirthDateStr = prefs.getString("birth_date", "2000-01-01") ?: "2000-01-01"
            cachedLifeExpectancy = prefs.getInt("life_expectancy", 80)
            cachedScale = prefs.getFloat("scale", 1.0f)
            cachedOffsetX = prefs.getFloat("offset_x", 0.5f)
            cachedOffsetY = prefs.getFloat("offset_y", 0.5f)
            cachedShowTitle = prefs.getBoolean("show_title", true)
            cachedShowStats = prefs.getBoolean("show_stats", true)
            
            // Update paint colors
            titlePaint.color = cachedTextColor
            miniLabelPaint.color = cachedTextColor
            sideLabelPaint.color = cachedTextColor
            accentPaint.color = cachedAccentColor
            secondaryPaint.color = cachedTextColor
            livedDayPaint.color = cachedAccentColor
            futureDayPaint.color = cachedFutureColor
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
                canvas?.let { drawCalendar(it) }
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        private fun drawCalendar(canvas: Canvas) {
            canvas.drawColor(cachedBgColor)
            
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()

            canvas.save()
            canvas.translate((cachedOffsetX - 0.5f) * width, (cachedOffsetY - 0.5f) * height)
            canvas.scale(cachedScale, cachedScale, width / 2f, height / 2f)

            val today = LocalDate.now()
            
            when (cachedCalendarType) {
                CalendarType.MONTH -> drawMonthView(canvas, today, width, height)
                CalendarType.YEAR -> drawYearView(canvas, today, width, height)
                CalendarType.LIFE -> drawLifeView(canvas, today, width, height, cachedBirthDateStr, cachedLifeExpectancy)
            }
            canvas.restore()
        }

        private fun drawMonthView(canvas: Canvas, today: LocalDate, width: Float, height: Float) {
            val month = today.month
            val year = today.year
            val firstDayOfMonth = LocalDate.of(year, month, 1)
            val daysInMonth = month.length(firstDayOfMonth.isLeapYear)
            val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1

            val dotSpacing = width / 8.5f
            val dotRadius = dotSpacing / 3.2f
            val startX = (width - (6 * dotSpacing)) / 2
            val gridStartY = height * 0.35f

            if (cachedShowTitle) {
                val monthName = month.getDisplayName(TextStyle.FULL, Locale("ru")).uppercase()
                canvas.drawText(monthName, startX, gridStartY - 100f, titlePaint)
            }

            var lastY = gridStartY
            for (day in 1..daysInMonth) {
                val dayIdx = day + dayOfWeekOffset - 1
                val dx = startX + (dayIdx % 7 * dotSpacing)
                val dy = gridStartY + (dayIdx / 7 * dotSpacing)
                lastY = dy
                
                val date = LocalDate.of(year, month, day)
                val paint = when {
                    date.isBefore(today) -> livedDayPaint
                    date.isEqual(today) -> todayPaint
                    else -> futureDayPaint
                }
                canvas.drawCircle(dx, dy, dotRadius, paint)
            }

            if (cachedShowStats) {
                val progress = (today.dayOfMonth.toFloat() / daysInMonth * 100).toInt()
                drawStatsLine(canvas, startX, lastY + dotSpacing * 1.2f, "$progress%", " ПРОЖИТО  •  ", "${daysInMonth - today.dayOfMonth}", " ДНЕЙ ОСТАЛОСЬ")
            }
        }

        private fun drawYearView(canvas: Canvas, today: LocalDate, width: Float, height: Float) {
            val year = today.year
            
            val mWidth = width / 3.5f
            val mHeight = height * 0.12f
            val dotSpacing = mWidth / 8.5f
            val dotRadius = dotSpacing / 3.8f
            
            val gridWidth = mWidth * 3
            val gridHeight = mHeight * 4
            val startX_global = (width - gridWidth) / 2
            val startY_global = (height - gridHeight) / 2

            if (cachedShowTitle) {
                canvas.drawText("$year ГОД", startX_global, startY_global - 80f, titlePaint)
            }

            for (m in 0 until 12) {
                val col = m % 3
                val row = m / 3
                val startX = col * mWidth + startX_global
                val startY = row * mHeight + startY_global
                
                val month = java.time.Month.of(m + 1)
                val monthName = month.getDisplayName(TextStyle.SHORT, Locale("ru")).uppercase()
                canvas.drawText(monthName, startX, startY - 15f, miniLabelPaint)
                
                val firstDay = LocalDate.of(year, m + 1, 1)
                val offset = firstDay.dayOfWeek.value - 1
                val daysInMonth = month.length(firstDay.isLeapYear)
                
                for (day in 1..daysInMonth) {
                    val dIdx = day + offset - 1
                    val dx = startX + (dIdx % 7 * dotSpacing)
                    val dy = startY + (dIdx / 7 * dotSpacing)
                    
                    val date = LocalDate.of(year, m + 1, day)
                    val paint = when {
                        date.isBefore(today) -> livedDayPaint
                        date.isEqual(today) -> todayPaint
                        else -> futureDayPaint
                    }
                    canvas.drawCircle(dx, dy, dotRadius, paint)
                }
            }
            
            if (cachedShowStats) {
                val dayOfYear = today.dayOfYear
                val totalDays = if (java.time.Year.isLeap(year.toLong())) 366 else 365
                val progress = (dayOfYear.toFloat() / totalDays * 100).toInt()
                drawStatsLine(canvas, startX_global, startY_global + gridHeight + 40f, "$progress%", " ГОДА ПРОШЛО  •  ", "${totalDays - dayOfYear}", " ДНЕЙ ОСТАЛОСЬ")
            }
        }

        private fun drawLifeView(canvas: Canvas, today: LocalDate, width: Float, height: Float, birthDateStr: String, lifeExpectancy: Int) {
            val birthDate = LocalDate.parse(birthDateStr)
            val totalWeeks = lifeExpectancy * 52
            val weeksLived = ChronoUnit.WEEKS.between(birthDate, today).toInt()
            
            val dotSpacing = (width - width * 0.2f) / 105f
            val dotRadius = dotSpacing / 2.8f
            val startX = width * 0.12f
            val startY = height * 0.2f
            
            if (cachedShowTitle) {
                canvas.drawText("КАЛЕНДАРЬ ЖИЗНИ", startX, startY - 80f, titlePaint)
            }

            for (week in 0 until totalWeeks) {
                val dx = startX + (week % 104 * dotSpacing)
                val dy = startY + (week / 104 * dotSpacing * 1.8f)
                
                if (week % 104 == 0 && (week / 52) % 10 == 0) {
                    canvas.drawText("${week / 52}", startX - 25f, dy + dotRadius, sideLabelPaint)
                }
                
                val paint = if (week < weeksLived) livedDayPaint else if (week == weeksLived) todayPaint else futureDayPaint
                canvas.drawCircle(dx, dy, dotRadius, paint)
            }

            if (cachedShowStats) {
                val progress = (weeksLived.toFloat() / totalWeeks * 100).toInt()
                val remainingWeeks = totalWeeks - weeksLived
                drawStatsLine(canvas, startX, height * 0.95f, "$progress%", " ЖИЗНИ  •  ", "$remainingWeeks", " НЕДЕЛЬ ОСТАЛОСЬ")
            }
        }

        private fun drawStatsLine(canvas: Canvas, startX: Float, y: Float, val1: String, lab1: String, val2: String, lab2: String) {
            var x = startX
            canvas.drawText(val1, x, y, accentPaint)
            x += accentPaint.measureText(val1)
            canvas.drawText(lab1, x, y, secondaryPaint)
            x += secondaryPaint.measureText(lab1)
            canvas.drawText(val2, x, y, accentPaint)
            x += accentPaint.measureText(val2)
            canvas.drawText(lab2, x, y, secondaryPaint)
        }
    }
}
