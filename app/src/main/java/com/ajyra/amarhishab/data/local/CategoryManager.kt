package com.ajyra.amarhishab.data.local

import android.content.Context
import android.content.SharedPreferences
import com.ajyra.amarhishab.model.CategoryItem
import com.ajyra.amarhishab.model.DefaultCategories
import com.ajyra.amarhishab.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class CategoryManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("amar_hishab_custom_categories", Context.MODE_PRIVATE)

    private val _customCategories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val customCategories: StateFlow<List<CategoryItem>> = _customCategories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        val jsonStr = prefs.getString(KEY_CATEGORIES, null)
        if (jsonStr.isNullOrBlank()) {
            _customCategories.value = emptyList()
            return
        }

        try {
            val list = mutableListOf<CategoryItem>()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val nameEn = obj.getString("nameEn")
                val nameBn = obj.optString("nameBn", nameEn)
                val typeStr = obj.getString("type")
                val iconName = obj.optString("iconName", "Category")
                val colorHex = obj.optLong("colorHex", 0xFF00897BL)

                val type = TransactionType.fromString(typeStr)
                list.add(
                    CategoryItem(
                        id = id,
                        nameEn = nameEn,
                        nameBn = nameBn,
                        type = type,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                )
            }
            _customCategories.value = list
        } catch (e: Exception) {
            _customCategories.value = emptyList()
        }
    }

    fun isDuplicateName(name: String, type: TransactionType, excludeId: String? = null): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false

        // Check defaults
        val defaultList = if (type == TransactionType.INCOME) DefaultCategories.incomeCategories else DefaultCategories.expenseCategories
        val inDefaults = defaultList.any {
            it.nameEn.equals(trimmed, ignoreCase = true) || it.nameBn.equals(trimmed, ignoreCase = true)
        }
        if (inDefaults) return true

        // Check customs
        return _customCategories.value.any {
            it.id != excludeId && it.type == type &&
                    (it.nameEn.equals(trimmed, ignoreCase = true) || it.nameBn.equals(trimmed, ignoreCase = true))
        }
    }

    @Synchronized
    fun addCategory(
        name: String,
        type: TransactionType,
        colorHex: Long = if (type == TransactionType.INCOME) 0xFF00897BL else 0xFFE65100L,
        iconName: String = "Category"
    ): CategoryItem {
        val trimmed = name.trim()
        val id = "custom_" + trimmed.lowercase().replace("\\s+".toRegex(), "_").replace("[^a-z0-9_]".toRegex(), "") + "_" + (System.currentTimeMillis() % 100000)
        val newItem = CategoryItem(
            id = id,
            nameEn = trimmed,
            nameBn = trimmed,
            type = type,
            iconName = iconName,
            colorHex = colorHex
        )

        val updatedList = _customCategories.value.toMutableList().apply { add(newItem) }
        saveList(updatedList)
        _customCategories.value = updatedList
        return newItem
    }

    @Synchronized
    fun editCategory(categoryId: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false

        val existing = _customCategories.value.firstOrNull { it.id == categoryId } ?: return false
        if (isDuplicateName(trimmed, existing.type, excludeId = categoryId)) return false

        val updatedList = _customCategories.value.map { item ->
            if (item.id == categoryId) {
                item.copy(nameEn = trimmed, nameBn = trimmed)
            } else {
                item
            }
        }
        saveList(updatedList)
        _customCategories.value = updatedList
        return true
    }

    @Synchronized
    fun deleteCategory(categoryId: String): Boolean {
        // Only custom categories can be deleted
        if (!categoryId.startsWith("custom_")) return false
        val updatedList = _customCategories.value.filterNot { it.id == categoryId }
        saveList(updatedList)
        _customCategories.value = updatedList
        return true
    }

    fun isCustom(categoryId: String): Boolean {
        return _customCategories.value.any { it.id == categoryId } || categoryId.startsWith("custom_")
    }

    private fun saveList(list: List<CategoryItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("nameEn", item.nameEn)
                put("nameBn", item.nameBn)
                put("type", item.type.value)
                put("iconName", item.iconName)
                put("colorHex", item.colorHex)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CATEGORIES, array.toString()).apply()
    }

    fun getAllCategories(type: TransactionType? = null): List<CategoryItem> {
        val defaultList = when (type) {
            TransactionType.INCOME -> DefaultCategories.incomeCategories
            TransactionType.EXPENSE -> DefaultCategories.expenseCategories
            null -> DefaultCategories.expenseCategories + DefaultCategories.incomeCategories
            else -> emptyList()
        }
        val customs = if (type != null) {
            _customCategories.value.filter { it.type == type }
        } else {
            _customCategories.value
        }
        return defaultList + customs
    }

    fun getCategoryDisplayName(idOrName: String, isBengali: Boolean): String {
        val custom = _customCategories.value.firstOrNull {
            it.id.equals(idOrName, ignoreCase = true) || it.nameEn.equals(idOrName, ignoreCase = true)
        }
        if (custom != null) {
            return if (isBengali) custom.nameBn else custom.nameEn
        }
        return DefaultCategories.getCategoryName(idOrName, isBengali)
    }

    fun getCategoryColor(idOrName: String): Long {
        val custom = _customCategories.value.firstOrNull {
            it.id.equals(idOrName, ignoreCase = true) || it.nameEn.equals(idOrName, ignoreCase = true)
        }
        if (custom != null) {
            return custom.colorHex
        }
        return DefaultCategories.getCategoryColor(idOrName)
    }

    companion object {
        private const val KEY_CATEGORIES = "custom_categories_json"

        @Volatile
        private var INSTANCE: CategoryManager? = null

        fun getInstance(context: Context): CategoryManager {
            return INSTANCE ?: synchronized(this) {
                val inst = CategoryManager(context.applicationContext)
                INSTANCE = inst
                inst
            }
        }
    }
}
