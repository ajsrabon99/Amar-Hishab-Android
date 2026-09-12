package com.ajyra.amarhishab.utils

import java.text.DecimalFormat

object CurrencyFormatter {
    private val englishFormat = DecimalFormat("#,##,##0.00")
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun format(amount: Double, isBengali: Boolean = false, showSymbol: Boolean = true): String {
        val formatted = englishFormat.format(amount)
        val symbol = if (showSymbol) "৳" else ""
        return if (isBengali) {
            val converted = formatted.map { ch ->
                if (ch in '0'..'9') banglaDigits[ch - '0'] else ch
            }.joinToString("")
            "$symbol$converted"
        } else {
            "$symbol$formatted"
        }
    }

    fun toBengaliNumber(numberStr: String): String {
        return numberStr.map { ch ->
            if (ch in '0'..'9') banglaDigits[ch - '0'] else ch
        }.joinToString("")
    }

    fun toBengaliDigits(numberStr: String): String = toBengaliNumber(numberStr)
}
