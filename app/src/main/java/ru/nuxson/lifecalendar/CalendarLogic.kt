package ru.nuxson.lifecalendar

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

data class CalendarEvent(
    val date: LocalDate,
    val color: Int,
    val label: String? = null
)

object CalendarRenderer {
    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        type: CalendarType,
        today: LocalDate,
        prefs: CalendarDrawParams,
        events: List<CalendarEvent> = emptyList()
    ) {
        val titlePaint = Paint().apply { color = prefs.textColor; textSize = 50f; isAntiAlias = true; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val miniLabelPaint = Paint().apply { color = prefs.textColor; textSize = 18f; textAlign = Paint.Align.CENTER; isAntiAlias = true; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val sideLabelPaint = Paint().apply { color = prefs.textColor; textSize = 16f; textAlign = Paint.Align.RIGHT; isAntiAlias = true }
        val dayNamePaint = Paint().apply { color = prefs.textColor; textSize = 22f; textAlign = Paint.Align.CENTER; isAntiAlias = true; alpha = 180 }
        
        val livedDayPaint = Paint().apply { color = prefs.accentColor; isAntiAlias = true }
        val futureDayPaint = Paint().apply { color = prefs.futureColor; isAntiAlias = true }
        val weekendDayPaint = Paint().apply { color = prefs.weekendColor; isAntiAlias = true }
        val todayPaint = Paint().apply { color = Color.RED; isAntiAlias = true }
        val eventPaint = Paint().apply { isAntiAlias = true }

        canvas.save()
        canvas.translate((prefs.offsetX - 0.5f) * width, (prefs.offsetY - 0.5f) * height)
        canvas.scale(prefs.scale, prefs.scale, width / 2f, height / 2f)

        when (type) {
            CalendarType.MONTH -> {
                val month = today.month
                val firstDayOfMonth = LocalDate.of(today.year, month, 1)
                val daysInMonth = month.length(firstDayOfMonth.isLeapYear)
                val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1
                val dotSpacing = width / 8.5f
                val dotRadius = dotSpacing / 3.2f
                val gridStartY = height * 0.35f
                val startX = (width - (6 * dotSpacing)) / 2
                
                if (prefs.showTitle) {
                    canvas.drawText(month.getDisplayName(TextStyle.FULL, Locale("ru")).uppercase(), startX, gridStartY - 120f, titlePaint)
                }

                if (prefs.showDayNames) {
                    val days = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
                    days.forEachIndexed { index, name ->
                        val dx = startX + (index * dotSpacing)
                        val dy = gridStartY - 50f
                        dayNamePaint.color = if (index >= 5) prefs.weekendColor else prefs.textColor
                        canvas.drawText(name, dx, dy, dayNamePaint)
                    }
                }
                
                for (day in 1..daysInMonth) {
                    val dayIdx = day + dayOfWeekOffset - 1
                    val dx = startX + (dayIdx % 7 * dotSpacing)
                    val dy = gridStartY + (dayIdx / 7 * dotSpacing)
                    val date = LocalDate.of(today.year, month, day)
                    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                    
                    val event = events.find { it.date == date }
                    val paint = when {
                        event != null -> { eventPaint.color = event.color; eventPaint }
                        date.isEqual(today) -> todayPaint
                        date.isBefore(today) -> livedDayPaint
                        isWeekend -> weekendDayPaint
                        else -> futureDayPaint
                    }
                    
                    canvas.drawCircle(dx, dy, dotRadius, paint)
                    
                    if (prefs.showDates) {
                        miniLabelPaint.textSize = dotRadius * 0.8f
                        val text = day.toString()
                        miniLabelPaint.color = if (shouldUseDarkText(paint.color)) Color.BLACK else Color.WHITE
                        val bounds = Rect()
                        miniLabelPaint.getTextBounds(text, 0, text.length, bounds)
                        canvas.drawText(text, dx, dy + bounds.height() / 2f, miniLabelPaint)
                    }
                }
            }
            CalendarType.YEAR -> {
                val mWidth = width / 3.5f
                val mHeight = height * 0.12f
                val dotSpacing = mWidth / 8.5f
                val dotRadius = dotSpacing / 3.8f
                val startX_global = (width - mWidth * 3) / 2
                val startY_global = (height - mHeight * 4) / 2

                if (prefs.showTitle) {
                    canvas.drawText("${today.year} ГОД", startX_global, startY_global - 80f, titlePaint)
                }

                for (m in 0 until 12) {
                    val col = m % 3; val row = m / 3
                    val startX = col * mWidth + startX_global; val startY = row * mHeight + startY_global
                    val month = java.time.Month.of(m + 1)
                    if (prefs.showTitle) {
                        canvas.drawText(month.getDisplayName(TextStyle.SHORT, Locale("ru")).uppercase(), startX, startY - 15f, miniLabelPaint.apply { textAlign = Paint.Align.LEFT; color = prefs.textColor; textSize = 18f })
                    }
                    
                    val firstDay = LocalDate.of(today.year, m + 1, 1)
                    val offset = firstDay.dayOfWeek.value - 1
                    for (day in 1..month.length(firstDay.isLeapYear)) {
                        val dIdx = day + offset - 1
                        val dx = startX + (dIdx % 7 * dotSpacing); val dy = startY + (dIdx / 7 * dotSpacing)
                        val date = LocalDate.of(today.year, m + 1, day)
                        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                        val event = events.find { it.date == date }
                        val paint = when {
                            event != null -> { eventPaint.color = event.color; eventPaint }
                            date.isEqual(today) -> todayPaint
                            date.isBefore(today) -> livedDayPaint
                            isWeekend -> weekendDayPaint
                            else -> futureDayPaint
                        }
                        canvas.drawCircle(dx, dy, dotRadius, paint)
                        
                        if (prefs.showDates) {
                            miniLabelPaint.textSize = 12f
                            miniLabelPaint.textAlign = Paint.Align.CENTER
                            miniLabelPaint.color = if (shouldUseDarkText(paint.color)) Color.BLACK else Color.WHITE
                            canvas.drawText(day.toString(), dx, dy + 4f, miniLabelPaint)
                        }
                    }
                }
            }
            CalendarType.LIFE -> {
                val birthDate = LocalDate.parse(prefs.birthDateStr)
                val totalWeeks = (prefs.lifeExpectancy * 52).toInt()
                val weeksLived = ChronoUnit.WEEKS.between(birthDate, today).toInt()
                val dotSpacing = (width - width * 0.2f) / 105f
                val dotRadius = dotSpacing / 2.8f
                val startX = width * 0.12f
                val startY = height * 0.2f
                
                if (prefs.showTitle) {
                    canvas.drawText("LifeDots", startX, startY - 80f, titlePaint)
                }
                
                for (week in 0 until totalWeeks) {
                    val dx = startX + (week % 104 * dotSpacing); val dy = startY + (week / 104 * dotSpacing * 1.8f)
                    if (week % 104 == 0 && (week / 52) % 10 == 0 && prefs.showTitle) {
                        canvas.drawText("${week / 52}", startX - 25f, dy + dotRadius, sideLabelPaint)
                    }
                    val paint = if (week < weeksLived) livedDayPaint else if (week == weeksLived) todayPaint else futureDayPaint
                    canvas.drawCircle(dx, dy, dotRadius, paint)
                }
            }
        }
        canvas.restore()
    }

    private fun shouldUseDarkText(backgroundColor: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(backgroundColor)) / 255
        return darkness < 0.5
    }
}

data class CalendarDrawParams(
    val accentColor: Int,
    val bgColor: Int,
    val futureColor: Int,
    val weekendColor: Int,
    val textColor: Int,
    val birthDateStr: String,
    val lifeExpectancy: Float,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val showTitle: Boolean,
    val showStats: Boolean,
    val showDates: Boolean,
    val showDayNames: Boolean
)
