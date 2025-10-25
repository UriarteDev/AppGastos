package com.smartsaldo.app.utils

import android.content.Context
import com.smartsaldo.app.R
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.getDefault())
    private val timeFormat = SimpleDateFormat(Constants.TIME_FORMAT_DISPLAY, Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat(Constants.DATE_TIME_FORMAT, Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun getStartOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfMonth(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month + 1, 0, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun getStartOfDay(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getEndOfDay(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH)
    }

    fun getCurrentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun getMonthName(context: Context, month: Int): String {
        return when (month) {
            0 -> context.getString(R.string.enero)
            1 -> context.getString(R.string.febrero)
            2 -> context.getString(R.string.marzo)
            3 -> context.getString(R.string.abril)
            4 -> context.getString(R.string.mayo)
            5 -> context.getString(R.string.junio)
            6 -> context.getString(R.string.julio)
            7 -> context.getString(R.string.agosto)
            8 -> context.getString(R.string.septiembre)
            9 -> context.getString(R.string.octubre)
            10 -> context.getString(R.string.noviembre)
            11 -> context.getString(R.string.diciembre)
            else -> ""
        }
    }

    fun getShortMonthName(context: Context, month: Int): String {
        return when (month) {
            0 -> context.getString(R.string.ene)
            1 -> context.getString(R.string.feb)
            2 -> context.getString(R.string.mar)
            3 -> context.getString(R.string.abr)
            4 -> context.getString(R.string.may)
            5 -> context.getString(R.string.jun)
            6 -> context.getString(R.string.jul)
            7 -> context.getString(R.string.ago)
            8 -> context.getString(R.string.sep)
            9 -> context.getString(R.string.oct)
            10 -> context.getString(R.string.nov)
            11 -> context.getString(R.string.dic)
            else -> ""
        }
    }

    fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = timestamp
        val day = calendar.get(Calendar.DAY_OF_YEAR)

        return today == day
    }

    fun isThisWeek(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)

        calendar.timeInMillis = timestamp
        val week = calendar.get(Calendar.WEEK_OF_YEAR)

        return currentWeek == week
    }

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