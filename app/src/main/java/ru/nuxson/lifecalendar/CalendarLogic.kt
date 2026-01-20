package ru.nuxson.lifecalendar

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CalendarStats(
    val year: Int,
    val totalDays: Int,
    val passedDays: Int,
    val remainingDays: Int,
    val progressPercentage: Float
)

object CalendarLogic {
    fun getStats(date: LocalDate = LocalDate.now()): CalendarStats {
        val year = date.year
        val firstDayOfYear = LocalDate.of(year, 1, 1)
        val lastDayOfYear = LocalDate.of(year, 12, 31)
        
        val totalDays = ChronoUnit.DAYS.between(firstDayOfYear, lastDayOfYear).toInt() + 1
        val passedDays = ChronoUnit.DAYS.between(firstDayOfYear, date).toInt() // Не включая сегодняшний
        val remainingDays = totalDays - passedDays - 1 // Исключая сегодняшний
        
        val progressPercentage = (passedDays.toFloat() / totalDays.toFloat()) * 100f
        
        return CalendarStats(year, totalDays, passedDays, remainingDays, progressPercentage)
    }
}
