package com.ajyra.amarhishab.model

object DefaultCategories {
    val expenseCategories = listOf(
        CategoryItem("food", "Food & Dining", "খাবার ও রেস্তোরাঁ", TransactionType.EXPENSE, "Restaurant", 0xFFE65100L),
        CategoryItem("groceries", "Groceries", "বাজার ও মুদি", TransactionType.EXPENSE, "ShoppingCart", 0xFFF57F17L),
        CategoryItem("shopping", "Shopping", "কেনাকাটা", TransactionType.EXPENSE, "ShoppingBag", 0xFFE91E63L),
        CategoryItem("transport", "Transportation", "যাতায়াত ও ভাড়া", TransactionType.EXPENSE, "DirectionsCar", 0xFF1E88E5L),
        CategoryItem("bills", "Bills & Utilities", "বিল ও বিদ্যুৎ", TransactionType.EXPENSE, "Receipt", 0xFF6E1876L),
        CategoryItem("rent", "House Rent", "বাড়ি ভাড়া", TransactionType.EXPENSE, "Home", 0xFF43A047L),
        CategoryItem("health", "Healthcare & Medicine", "চিকিৎসা ও ওষুধ", TransactionType.EXPENSE, "LocalHospital", 0xFF00897BL),
        CategoryItem("education", "Education", "পড়াশোনা ও বই", TransactionType.EXPENSE, "School", 0xFF039BE5L),
        CategoryItem("entertainment", "Entertainment", "বিনোদন ও ঘুরোঘুরি", TransactionType.EXPENSE, "Movie", 0xFFF4511EL),
        CategoryItem("family", "Family & Gifts", "পরিবার ও দান", TransactionType.EXPENSE, "CardGiftcard", 0xFF00ACC1L),
        CategoryItem("other_expense", "Other Expense", "অন্যান্য খরচ", TransactionType.EXPENSE, "MoreHoriz", 0xFF546E7AL)
    )

    val incomeCategories = listOf(
        CategoryItem("salary", "Salary", "মাসিক বেতন", TransactionType.INCOME, "Work", 0xFF00897BL),
        CategoryItem("freelance", "Freelancing", "ফ্রিল্যান্সিং", TransactionType.INCOME, "Laptop", 0xFF1E88E5L),
        CategoryItem("business", "Business", "ব্যবসা ও বিক্রয়", TransactionType.INCOME, "Store", 0xFF6E1876L),
        CategoryItem("investment", "Investment & Profit", "বিনিয়োগ ও মুনাফা", TransactionType.INCOME, "TrendingUp", 0xFFF4511EL),
        CategoryItem("gift_income", "Gift / Allowance", "উপহার ও অনুদান", TransactionType.INCOME, "CardGiftcard", 0xFFE91E63L),
        CategoryItem("other_income", "Other Income", "অন্যান্য আয়", TransactionType.INCOME, "AddCircle", 0xFF546E7AL)
    )

    fun getCategoryName(id: String, isBengali: Boolean): String {
        val all = expenseCategories + incomeCategories
        val item = all.firstOrNull { it.id.equals(id, ignoreCase = true) }
        return if (item != null) {
            if (isBengali) item.nameBn else item.nameEn
        } else {
            id
        }
    }

    fun getCategoryColor(id: String): Long {
        val all = expenseCategories + incomeCategories
        return all.firstOrNull { it.id.equals(id, ignoreCase = true) }?.colorHex ?: 0xFF00897BL
    }
}
