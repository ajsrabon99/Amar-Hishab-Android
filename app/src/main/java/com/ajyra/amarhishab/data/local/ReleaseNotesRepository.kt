package com.ajyra.amarhishab.data.local

import com.ajyra.amarhishab.BuildConfig

data class ReleaseNote(
    val version: String,
    val buildNumber: Int,
    val releaseDate: String,
    val highlightsEn: List<String>,
    val highlightsBn: List<String>
) {
    val versionName: String get() = version
    val featuresEn: List<String> get() = highlightsEn
    val featuresBn: List<String> get() = highlightsBn
}

object ReleaseNotesRepository {
    val releaseNotes = listOf(
        ReleaseNote(
            version = BuildConfig.VERSION_NAME,
            buildNumber = 1,
            releaseDate = "March 2025",
            highlightsEn = listOf(
                "Initial production release of Amar Hishab personal finance tracker",
                "Full multi-account support: Cash, bKash, Nagad, and Bank",
                "Zero-loss offline persistence with Room local database",
                "Secure Google authentication & backend synchronization",
                "Detailed analytics, category distribution, and monthly reports",
                "Bilingual support with fluent Bengali and English localization",
                "Material 3 adaptive light and dark themes"
            ),
            highlightsBn = listOf(
                "আমার হিসাব পার্সোনাল ফিন্যান্স অ্যাপের প্রথম প্রোডাকশন রিলিজ",
                "ক্যাশ, বিকাশ, নগদ এবং ব্যাংক অ্যাকাউন্টের সম্পূর্ণ সাপোর্ট",
                "রুম লোকাল ডেটাবেজের মাধ্যমে শতভাগ অফলাইন লেনদেন সংরক্ষণ",
                "নিরাপদ গুগল লগইন ও ব্যাকএন্ড রিয়েল-টাইম সিঙ্ক",
                "মাসিক রিপোর্ট, আয় বনাম খরচ এবং খাতভিত্তিক নিখুঁত বিশ্লেষণ",
                "বাংলা এবং ইংরেজি সম্পূর্ণ দ্বিভাষিক ইন্টারফেস",
                "আধুনিক ম্যাটেরিয়াল ৩ লাইট এবং ডার্ক থিম"
            )
        )
    )

    fun getLatest(): ReleaseNote = releaseNotes.first()
}
