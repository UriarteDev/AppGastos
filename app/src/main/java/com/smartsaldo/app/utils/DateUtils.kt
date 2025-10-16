package com.smartsaldo.app.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.getDefault())
    private val timeFormat = SimpleDateFormat(Constants.TIME_FORMAT_DISPLAY, Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.getDefault())

    /**
     * Formatea un timestamp a fecha (dd/MM/yyyy)
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Formatea un timestamp a hora (HH:mm)
     */
    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    /**
     * Formatea un timestamp a fecha y hora
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    /**
     * Obtiene el inicio del mes actual
     */
    fun getStartOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Obtiene el fin del mes actual
     */
    fun getEndOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month + 1, 0, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Obtiene el inicio del día
     */
    fun getStartOfDay(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Obtiene el fin del día
     */
    fun getEndOfDay(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Obtiene el mes actual
     */
    fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH)
    }

    /**
     * Obtiene el año actual
     */
    fun getCurrentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    /**
     * Obtiene el nombre del mes
     */
    fun getMonthName(month: Int): String {
        return when (month) {
            0 -> "Enero"
            1 -> "Febrero"
            2 -> "Marzo"
            3 -> "Abril"
            4 -> "Mayo"
            5 -> "Junio"
            6 -> "Julio"
            7 -> "Agosto"
            8 -> "Septiembre"
            9 -> "Octubre"
            10 -> "Noviembre"
            11 -> "Diciembre"
            else -> ""
        }
    }

    /**
     * Obtiene el nombre corto del mes
     */
    fun getShortMonthName(month: Int): String {
        return when (month) {
            0 -> "Ene"
            1 -> "Feb"
            2 -> "Mar"
            3 -> "Abr"
            4 -> "May"
            5 -> "Jun"
            6 -> "Jul"
            7 -> "Ago"
            8 -> "Sep"
            9 -> "Oct"
            10 -> "Nov"
            11 -> "Dic"
            else -> ""
        }
    }

    /**
     * Verifica si una fecha es hoy
     */
    fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = timestamp
        val day = calendar.get(Calendar.DAY_OF_YEAR)

        return today == day
    }

    /**
     * Verifica si una fecha es de esta semana
     */
    fun isThisWeek(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)

        calendar.timeInMillis = timestamp
        val week = calendar.get(Calendar.WEEK_OF_YEAR)

        return currentWeek == week
    }

    /**
     * Verifica si una fecha es de este mes
     */
    fun isThisMonth(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = timestamp
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        return currentMonth == month && currentYear == year
    }
}