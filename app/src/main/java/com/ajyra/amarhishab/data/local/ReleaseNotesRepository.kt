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
            version = "1.1.1",
            buildNumber = 12,
            releaseDate = "September 2026",
            highlightsEn = listOf(
                "Robust PDF Statement Import: Direct parsing of Amar Hishab and tabular PDF statements into your local database",
                "Summary Filter Protection: Total Income, Total Expense, and Net Balance report figures are smartly filtered and never imported as transactions",
                "Accurate Transfer Handling: Preserves transfer transactions with accounts and prevents balance leakage or double counting",
                "Universal Date & Excel Support: Comprehensive date format parsing with Bengali numerals and enhanced XLSX inline string extraction",
                "Official Telegram Community: Connect with the official Amar Hishab Telegram group (t.me/amarhishab) for updates and announcements",
                "Clean Dashboard Tools: Focused quick access to Goals, Statement generation, and Data Import"
            ),
            highlightsBn = listOf(
                "উন্নত PDF স্টেটমেন্ট ইমপোর্ট: আমার হিসাব ও সাধারণ PDF স্টেটমেন্ট থেকে সরাসরি লোকাল ডেটাবেজে হিসাব ইমপোর্ট",
                "সারসংক্ষেপ ফিল্টার সুরক্ষা: মোট আয়, মোট ব্যয় ও নীট ব্যালেন্স আলাদা রেখে শুধুমাত্র আসল লেনদেনগুলো যুক্ত করার নিশ্চয়তা",
                "নির্ভুল স্থানান্তর (Transfer) সংরক্ষণ: ট্রান্সফারের উৎস ও গন্তব্য হিসাব সুরক্ষিত রেখে নির্ভুল ব্যালেন্স বজায় রাখা",
                "উন্নত তারিখ ও এক্সেল সমর্থন: বাংলা সংখ্যাসহ যেকোনো তারিখ ফরম্যাট ও আধুনিক XLSX ফরম্যাট সমর্থন",
                "অফিসিয়াল টেলিগ্রাম কমিউনিটি: সরাসরি t.me/amarhishab চ্যানেলে যুক্ত হয়ে সর্বশেষ খবর ও আপডেট জানার সুবিধা",
                "পরিশীলিত ড্যাশবোর্ড টুলস: সহজে লক্ষ্য, স্টেটমেন্ট এবং ইমপোর্টে দ্রুত প্রবেশের পরিচ্ছন্ন ডিজাইন"
            )
        ),
        ReleaseNote(
            version = "1.0.8",
            buildNumber = 9,
            releaseDate = "September 2026",
            highlightsEn = listOf(
                "Official Brand Visual Consistency: Refined app icon and splash logo aligned with the official Amar Hishab brand mark",
                "Clean Quick Actions: Corrected duplicate symbol issue on Dashboard Income & Expense action pills",
                "Smooth Startup Animation: Premium, responsive intro splash presentation"
            ),
            highlightsBn = listOf(
                "অফিসিয়াল ব্র্যান্ড ভিজ্যুয়াল: আমার হিসাব এর অফিশিয়াল লোগোর সাথে সামঞ্জস্য রেখে অ্যাপ আইকন ও স্প্ল্যাশ স্ক্রিন পরিমার্জন",
                "পরিচ্ছন্ন কুইক অ্যাকশন: ড্যাশবোর্ডের আয় ও ব্যয় বাটনে ডুপ্লিকেট চিহ্নের সমাধান",
                "মসৃণ স্টার্টআপ অ্যানিমেশন: প্রিমিয়াম ও আকর্ষণীয় স্প্ল্যাশ অ্যানিমেশন"
            )
        ),
        ReleaseNote(
            version = "1.0.7",
            buildNumber = 8,
            releaseDate = "September 2026",
            highlightsEn = listOf(
                "Smart Savings Goals: Set goals, track progress, add savings, and receive automated discretionary spending reduction advice",
                "Refined Dashboard Hero: Minimalist fintech balance overview with instant visibility toggle and quick tools hub",
                "Robust Account Balance Check: Zero-balance and insufficient balance validation on transfers"
            ),
            highlightsBn = listOf(
                "স্মার্ট সঞ্চয় লক্ষ্য: সঞ্চয় লক্ষ্য নির্ধারণ, অগ্রগতি পর্যবেক্ষণ এবং অপ্রয়োজনীয় খরচ কমানোর স্বয়ংক্রিয় পরামর্শ",
                "পরিশীলিত ড্যাশবোর্ড হিরো কার্ড: ব্যালেন্স হাইড/শো এবং কুইক টুলস সমৃদ্ধ আধুনিক ফিনটেক ডিজাইন",
                "নির্ভুল ব্যালেন্স যাচাই: ট্রান্সফারের সময় অপর্যাপ্ত ব্যালেন্স ও শূন্য ব্যালেন্স অ্যাকাউন্ট নিয়ন্ত্রণ"
            )
        ),
        ReleaseNote(
            version = "1.0.6",
            buildNumber = 7,
            releaseDate = "September 2026",
            highlightsEn = listOf(
                "Unified Data Architecture: Authoritative single Room database for all imported CSV/ZIP/XLSX/PDF records, dashboard metrics, and reports",
                "Pull-to-Refresh & Instant Reload: Quick dashboard swipe and manual refresh for immediate data synchronization",
                "Dashboard Expense Summary: Interactive category breakdown with progress bars, percentages, and amounts directly on Home screen",
                "Enhanced Import Engine: Comprehensive file validation, duplicate protection, and multi-format support",
                "Refined Category Interface: Streamlined custom category action buttons across transaction entry screens"
            ),
            highlightsBn = listOf(
                "সমন্বিত ডেটা আর্কিটেকচার: ইম্পোর্ট করা সমস্ত ডেটা, ড্যাশবোর্ড এবং রিপোর্টের জন্য একক নির্ভুল ডেটাবেজ",
                "পুল-টু-রিফ্রেশ ও তাৎক্ষণিক আপডেট: ড্যাশবোর্ডে সোয়াইপ করে সাথে সাথে ডেটা রিফ্রেশ করার সুবিধা",
                "ড্যাশবোর্ড ব্যয় বিশ্লেষণ: হোম স্ক্রিনেই ক্যাটাগরিভিত্তিক ব্যয়ের শতকরা ও মোট হিসাব",
                "উন্নত ইমপোর্ট ইঞ্জিন: নিরাপদ ভ্যালিডেশন এবং ডুপ্লিকেট প্রতিরোধসহ বহুবিধ ফাইল সমর্থন",
                "পরিশীলিত ইন্টারফেস: সহজে নতুন ক্যাটাগরি নির্বাচনের পরিচ্ছন্ন বাটন"
            )
        ),
        ReleaseNote(
            version = "1.0.4",
            buildNumber = 5,
            releaseDate = "September 2026",
            highlightsEn = listOf(
                "Direct Custom Categories: Add new custom Income & Expense categories directly from the Add Transaction screen",
                "Immediate Category Selection: Newly created custom categories are selected automatically and persist permanently"
            ),
            highlightsBn = listOf(
                "সরাসরি কাস্টম খাত যোগ: লেনদেন যোগ করার স্ক্রিন থেকেই সরাসরি নিজস্ব আয় ও ব্যয়ের খাত তৈরি করুন",
                "তাত্ক্ষণিক নির্বাচন: নতুন তৈরি করা খাত সাথে সাথে নির্বাচিত হয় এবং স্থায়ীভাবে সংরক্ষিত থাকে"
            )
        ),
        ReleaseNote(
            version = "1.0.3",
            buildNumber = 4,
            releaseDate = "September 2026",
            highlightsEn = listOf(
                "Add Own Category: Create custom Income & Expense categories with customizable icons",
                "Category Management: Edit and delete custom categories anytime",
                "Hide Balance: Privacy toggle on dashboard hero card with masked balance",
                "Statement Generator: Export financial statements to PDF & CSV with custom date & account filters",
                "Data Import: Import transactions seamlessly from CSV and ZIP files with duplicate protection",
                "Profile Photo: Update and remove profile avatar with instant display on dashboard"
            ),
            highlightsBn = listOf(
                "নিজের খাত যোগ করুন: পছন্দসই আইকনসহ নিজস্ব আয় ও ব্যয়ের খাত তৈরি করুন",
                "খাত পরিচালনা: যেকোনো সময় কাস্টম খাত সম্পাদনা ও মুছে ফেলার সুবিধা",
                "ব্যালেন্স লুকান: ড্যাশবোর্ডে আইকন ট্যাপ করে মোট ব্যালেন্স গোপন করার সুবিধা",
                "স্টেটমেন্ট তৈরি: কাস্টম তারিখ ও অ্যাকাউন্ট ফিল্টারে PDF ও CSV ফরম্যাটে স্টেটমেন্ট এক্সপোর্ট",
                "ডেটা ইমপোর্ট: CSV ও ZIP ফাইল থেকে সহজে ও নিরাপদে লেনদেন ইমপোর্ট করুন",
                "প্রোফাইল ছবি: প্রোফাইল অবতার পরিবর্তন ও রিমুভ করার পূর্ণাঙ্গ সুবিধা"
            )
        ),
        ReleaseNote(
            version = "1.0.2",
            buildNumber = 3,
            releaseDate = "September 2026",
            highlightsEn = listOf(
                "Maintenance and stability improvements",
                "UI responsiveness and theme refinements"
            ),
            highlightsBn = listOf(
                "অ্যাপের কার্যক্ষমতা ও স্থায়িত্ব বৃদ্ধি",
                "ইন্টারফেস ও থিম অপ্টিমাইজেশন"
            )
        ),
        ReleaseNote(
            version = "1.0.1",
            buildNumber = 2,
            releaseDate = "September 2026",
            highlightsEn = listOf(
                "Biometric Privacy Lock: Secure application startup with fingerprint and device PIN credentials",
                "Currency & Math Optimization: Precision calculation improvements for high-volume transactions"
            ),
            highlightsBn = listOf(
                "বায়োমেট্রিক অ্যাপ সুরক্ষা: ফিঙ্গারপ্রিন্ট ও ডিভাইস লক দিয়ে ব্যক্তিগত হিসাবের গোপনীয়তা রক্ষা",
                "নিখুঁত হিসাব গণনা: বড় অংকের লেনদেনের জন্য উচ্চ গাণিতিক নির্ভুলতা নিশ্চিতকরণ"
            )
        ),
        ReleaseNote(
            version = "1.0.0",
            buildNumber = 1,
            releaseDate = "September 2026",
            highlightsEn = listOf(
                "Official Initial Launch of Amar Hishab personal finance suite",
                "Full offline transaction tracking across Cash, bKash, Nagad, and Bank accounts",
                "Interactive expense charts, monthly reports, and bilingual English & Bengali support"
            ),
            highlightsBn = listOf(
                "আমার হিসাব ব্যক্তিগত ফাইন্যান্স অ্যাপের অফিসিয়াল সূচনা",
                "ক্যাশ, বিকাশ, নগদ ও ব্যাংকের সব আর্থিক লেনদেন সম্পূর্ণ অফলাইনে নিরাপদে সংরক্ষণের সুবিধা",
                "ইন্টারেক্টিভ চার্ট, মাসিক রিপোর্ট এবং বাংলা ও ইংরেজি উভয় ভাষার পূর্ণাঙ্গ সমর্থন"
            )
        )
    )

    fun getLatest(): ReleaseNote = releaseNotes.first()
}
