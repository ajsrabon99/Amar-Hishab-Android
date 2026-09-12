package com.ajyra.amarhishab.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormatEn = SimpleDateFormat("dd MMMM, yyyy", Locale.US)
    private val banglaMonths = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    fun todayDateString(): String = apiDateFormat.format(Date())
    fun todayIso(): String = todayDateString()

    fun formatDisplayDate(dateStr: String, isBengali: Boolean = false): String {
        return try {
            val date = apiDateFormat.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance().apply { time = date }
            val todayCal = Calendar.getInstance()

            val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

            todayCal.add(Calendar.DAY_OF_YEAR, -1)
            val isYesterday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

            if (isToday) return if (isBengali) "আজ" else "Today"
            if (isYesterday) return if (isBengali) "গতকাল" else "Yesterday"

            if (isBengali) {
                val day = CurrencyFormatter.toBengaliNumber(cal.get(Calendar.DAY_OF_MONTH).toString())
                val month = banglaMonths[cal.get(Calendar.MONTH)]
                val year = CurrencyFormatter.toBengaliNumber(cal.get(Calendar.YEAR).toString())
                "$day $month, $year"
            } else {
                displayDateFormatEn.format(date)
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatDisplay(dateStr: String, isBengali: Boolean = false): String = formatDisplayDate(dateStr, isBengali)

    fun getMonthName(month: Int, isBengali: Boolean = false): String {
        val idx = (month - 1).coerceIn(0, 11)
        return if (isBengali) {
            banglaMonths[idx]
        } else {
            val cal = Calendar.getInstance().apply { set(Calendar.MONTH, idx) }
            SimpleDateFormat("MMMM", Locale.US).format(cal.time)
        }
    }

    fun monthName(month: Int, isBengali: Boolean = false): String = getMonthName(month, isBengali)
}
