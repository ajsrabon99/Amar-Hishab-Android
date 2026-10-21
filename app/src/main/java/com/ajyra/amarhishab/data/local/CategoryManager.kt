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

    private val _deletedCategoryKeys = MutableStateFlow<Set<String>>(emptySet())
    val deletedCategoryKeys: StateFlow<Set<String>> = _deletedCategoryKeys.asStateFlow()

    private val _categoriesUpdateTrigger = MutableStateFlow(0)
    val categoriesUpdateTrigger: StateFlow<Int> = _categoriesUpdateTrigger.asStateFlow()

    // In-memory cache for historical category names so deleted categories continue to display nicely in past transactions
    private val historicalCache: MutableMap<String, Pair<String, String>> = mutableMapOf()

    init {
        loadHistoricalCache()
        loadDeletedCategoryKeys()
        loadCategories()
    }

    private fun loadDeletedCategoryKeys() {
        try {
            val jsonStr = prefs.getString(KEY_DELETED_CATEGORIES, null)
            if (!jsonStr.isNullOrBlank()) {
                val array = JSONArray(jsonStr)
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    set.add(array.getString(i).lowercase())
                }
                _deletedCategoryKeys.value = set
            }
        } catch (_: Exception) {}
    }

    private fun saveDeletedCategoryKeys() {
        try {
            val array = JSONArray()
            _deletedCategoryKeys.value.forEach { array.put(it) }
            prefs.edit().putString(KEY_DELETED_CATEGORIES, array.toString()).commit()
        } catch (_: Exception) {}
    }

    private fun loadHistoricalCache() {
        try {
            val jsonStr = prefs.getString(KEY_HISTORICAL_CACHE, null) ?: return
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = json.optJSONObject(key)
                if (obj != null) {
                    val nameEn = obj.optString("nameEn", "")
                    val nameBn = obj.optString("nameBn", nameEn)
                    if (nameEn.isNotBlank()) {
                        historicalCache[key.lowercase()] = Pair(nameEn, nameBn)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveHistoricalCache() {
        try {
            val json = JSONObject()
            for ((k, v) in historicalCache) {
                val obj = JSONObject().apply {
                    put("nameEn", v.first)
                    put("nameBn", v.second)
                }
                json.put(k, obj)
            }
            prefs.edit().putString(KEY_HISTORICAL_CACHE, json.toString()).commit()
        } catch (_: Exception) {}
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
                val workspace = obj.optString("workspace", "ALL")

                val type = TransactionType.fromString(typeStr)
                val item = CategoryItem(
                    id = id,
                    nameEn = nameEn,
                    nameBn = nameBn,
                    type = type,
                    iconName = iconName,
                    colorHex = colorHex,
                    workspace = workspace
                )
                list.add(item)
                historicalCache[id.lowercase()] = Pair(nameEn, nameBn)
                historicalCache[nameEn.lowercase()] = Pair(nameEn, nameBn)
            }
            _customCategories.value = list
        } catch (e: Exception) {
            _customCategories.value = emptyList()
        }
    }

    fun isCategoryDeleted(idOrName: String, workspace: String? = null): Boolean {
        val trimmed = idOrName.trim().lowercase()
        val deleted = _deletedCategoryKeys.value
        if (deleted.contains("all:$trimmed")) return true
        if (workspace != null) {
            val wsKey = "${workspace.lowercase()}:$trimmed"
            if (deleted.contains(wsKey)) return true
        }
        return false
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
        iconName: String = "Category",
        workspace: String = "ALL",
        customId: String? = null,
        nameBn: String? = null
    ): CategoryItem {
        val trimmed = name.trim()
        val trimmedBn = nameBn?.trim()?.ifBlank { null } ?: trimmed
        val id = customId ?: ("custom_" + trimmed.lowercase().replace("\\s+".toRegex(), "_").replace("[^a-z0-9_]".toRegex(), "") + "_" + (System.currentTimeMillis() % 100000))
        val newItem = CategoryItem(
            id = id,
            nameEn = trimmed,
            nameBn = trimmedBn,
            type = type,
            iconName = iconName,
            colorHex = colorHex,
            workspace = workspace
        )

        // Store into historical cache
        historicalCache[id.lowercase()] = Pair(trimmed, trimmedBn)
        historicalCache[trimmed.lowercase()] = Pair(trimmed, trimmedBn)
        historicalCache[trimmedBn.lowercase()] = Pair(trimmed, trimmedBn)
        saveHistoricalCache()

        // Un-delete if this category was previously deleted
        val updatedDeleted = _deletedCategoryKeys.value.toMutableSet()
        val idKey = id.lowercase()
        updatedDeleted.remove("all:$idKey")
        updatedDeleted.remove("personal:$idKey")
        updatedDeleted.remove("business:$idKey")
        updatedDeleted.remove("all:${trimmed.lowercase()}")
        updatedDeleted.remove("personal:${trimmed.lowercase()}")
        updatedDeleted.remove("business:${trimmed.lowercase()}")
        _deletedCategoryKeys.value = updatedDeleted
        saveDeletedCategoryKeys()

        val updatedList = _customCategories.value.toMutableList().apply { add(newItem) }
        saveList(updatedList)
        _customCategories.value = updatedList
        _categoriesUpdateTrigger.value += 1
        return newItem
    }

    @Synchronized
    fun editCategory(categoryId: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false

        val existing = _customCategories.value.firstOrNull { it.id == categoryId } ?: return false
        if (isDuplicateName(trimmed, existing.type, excludeId = categoryId)) return false

        historicalCache[categoryId.lowercase()] = Pair(trimmed, trimmed)
        historicalCache[trimmed.lowercase()] = Pair(trimmed, trimmed)
        saveHistoricalCache()

        val updatedList = _customCategories.value.map { item ->
            if (item.id == categoryId) {
                item.copy(nameEn = trimmed, nameBn = trimmed)
            } else {
                item
            }
        }
        saveList(updatedList)
        _customCategories.value = updatedList
        _categoriesUpdateTrigger.value += 1
        return true
    }

    @Synchronized
    fun deleteCategory(categoryIdOrName: String, workspace: String = "ALL"): Boolean {
        val trimmed = categoryIdOrName.trim()
        if (trimmed.isEmpty()) return false

        // Check if matching default category
        val allDefaults = DefaultCategories.expenseCategories + DefaultCategories.incomeCategories
        val defaultTarget = allDefaults.firstOrNull {
            it.id.equals(trimmed, ignoreCase = true) ||
            it.nameEn.equals(trimmed, ignoreCase = true) ||
            it.nameBn.equals(trimmed, ignoreCase = true)
        }

        // Check if matching custom category
        val customTarget = _customCategories.value.firstOrNull {
            it.id.equals(trimmed, ignoreCase = true) ||
            it.nameEn.equals(trimmed, ignoreCase = true) ||
            it.nameBn.equals(trimmed, ignoreCase = true)
        }

        if (defaultTarget == null && customTarget == null) {
            return false
        }

        // 1. Preserve name in historical cache so prior transactions retain readable category names
        val targetItem = defaultTarget ?: customTarget!!
        historicalCache[targetItem.id.lowercase()] = Pair(targetItem.nameEn, targetItem.nameBn)
        historicalCache[targetItem.nameEn.lowercase()] = Pair(targetItem.nameEn, targetItem.nameBn)
        historicalCache[targetItem.nameBn.lowercase()] = Pair(targetItem.nameEn, targetItem.nameBn)
        saveHistoricalCache()

        // 2. Add to deleted set with workspace isolation
        val updatedDeleted = _deletedCategoryKeys.value.toMutableSet()
        val catId = targetItem.id.lowercase()
        val nameEn = targetItem.nameEn.lowercase()
        val ws = workspace.uppercase()
        if (ws == "ALL") {
            updatedDeleted.add("all:$catId")
            updatedDeleted.add("personal:$catId")
            updatedDeleted.add("business:$catId")
            updatedDeleted.add("all:$nameEn")
            updatedDeleted.add("personal:$nameEn")
            updatedDeleted.add("business:$nameEn")
        } else {
            updatedDeleted.add("${ws.lowercase()}:$catId")
            updatedDeleted.add("${ws.lowercase()}:$nameEn")
        }
        _deletedCategoryKeys.value = updatedDeleted
        saveDeletedCategoryKeys()

        // 3. If custom, also remove from custom list if applicable
        if (customTarget != null) {
            val updatedList = _customCategories.value.filterNot {
                it.id.equals(customTarget.id, ignoreCase = true) &&
                (ws == "ALL" || it.workspace == "ALL" || it.workspace.equals(ws, ignoreCase = true))
            }
            saveList(updatedList)
            _customCategories.value = updatedList
        }

        _categoriesUpdateTrigger.value += 1
        return true
    }

    fun isCustom(categoryId: String): Boolean {
        val inDefaults = (DefaultCategories.expenseCategories + DefaultCategories.incomeCategories)
            .any { it.id.equals(categoryId, ignoreCase = true) }
        return !inDefaults
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
                put("workspace", item.workspace)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CATEGORIES, array.toString()).commit()
    }

    fun getAllCategories(type: TransactionType? = null, workspace: String? = null): List<CategoryItem> {
        val defaultList = when (type) {
            TransactionType.INCOME -> DefaultCategories.incomeCategories
            TransactionType.EXPENSE -> DefaultCategories.expenseCategories
            null -> DefaultCategories.expenseCategories + DefaultCategories.incomeCategories
            else -> emptyList()
        }
        val customs = _customCategories.value.filter { cat ->
            (type == null || cat.type == type) &&
                    (workspace == null || cat.workspace == "ALL" || cat.workspace.equals(workspace, ignoreCase = true))
        }
        val combined = defaultList + customs
        val filtered = combined.filterNot { isCategoryDeleted(it.id, workspace) || isCategoryDeleted(it.nameEn, workspace) }
        if (filtered.isEmpty()) {
            val fallbackType = type ?: TransactionType.EXPENSE
            return listOf(
                CategoryItem(
                    id = "uncategorized",
                    nameEn = "Uncategorized",
                    nameBn = "সাধারণ/বিবিধ",
                    type = fallbackType,
                    iconName = "Category",
                    colorHex = 0xFF757575L,
                    workspace = workspace ?: "ALL"
                )
            )
        }
        return filtered
    }

    fun getCategoryDisplayName(idOrName: String, isBengali: Boolean): String {
        val trimmed = idOrName.trim()
        val custom = _customCategories.value.firstOrNull {
            it.id.equals(trimmed, ignoreCase = true) || it.nameEn.equals(trimmed, ignoreCase = true)
        }
        if (custom != null) {
            return if (isBengali) custom.nameBn else custom.nameEn
        }

        // Check defaults
        val defaultName = DefaultCategories.getCategoryName(trimmed, isBengali)
        if (defaultName != trimmed) {
            return defaultName
        }

        // Check historical cache (for deleted custom categories)
        val cached = historicalCache[trimmed.lowercase()]
        if (cached != null) {
            return if (isBengali) cached.second else cached.first
        }

        // Clean up custom_id strings if raw
        if (trimmed.startsWith("custom_")) {
            val parts = trimmed.removePrefix("custom_").split("_")
            val clean = parts.filter { !it.all { ch -> ch.isDigit() } }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            if (clean.isNotBlank()) return clean
        }

        return trimmed
    }

    fun getCategoryColor(idOrName: String): Long {
        val trimmed = idOrName.trim()
        val custom = _customCategories.value.firstOrNull {
            it.id.equals(trimmed, ignoreCase = true) || it.nameEn.equals(trimmed, ignoreCase = true)
        }
        if (custom != null) {
            return custom.colorHex
        }
        return DefaultCategories.getCategoryColor(trimmed)
    }

    companion object {
        private const val KEY_CATEGORIES = "custom_categories_json"
        private const val KEY_HISTORICAL_CACHE = "historical_category_names_json"
        private const val KEY_DELETED_CATEGORIES = "deleted_categories_keys_json"

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
