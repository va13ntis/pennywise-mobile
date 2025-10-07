package com.pennywise.app.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pennywise.app.R

/**
 * Utility class for mapping between canonical category keys and localized category names
 * This ensures categories are stored consistently regardless of the language used when adding expenses
 */
object CategoryMapper {
    
    /**
     * Canonical category keys that are stored in the database
     */
    enum class CategoryKey(val key: String) {
        FOOD("food"),
        TRANSPORT("transport"),
        SHOPPING("shopping"),
        ENTERTAINMENT("entertainment"),
        UTILITIES("utilities"),
        HEALTH("health"),
        EDUCATION("education"),
        OTHER("other")
    }
    
    /**
     * Map localized category name to canonical key
     * This is used when saving expenses to ensure consistent storage
     */
    fun getCategoryKey(localizedCategory: String): String {
        return when (localizedCategory.lowercase()) {
            // English
            "food & dining", "food", "groceries", "restaurant" -> CategoryKey.FOOD.key
            "transportation", "transport", "gas", "parking", "uber" -> CategoryKey.TRANSPORT.key
            "shopping", "clothes", "electronics" -> CategoryKey.SHOPPING.key
            "entertainment", "movies", "games" -> CategoryKey.ENTERTAINMENT.key
            "utilities", "bills", "rent", "electric" -> CategoryKey.UTILITIES.key
            "healthcare", "health", "medical", "pharmacy" -> CategoryKey.HEALTH.key
            "education", "books", "courses" -> CategoryKey.EDUCATION.key
            "other", "notes" -> CategoryKey.OTHER.key
            
            // Hebrew
            "אוכל וסעודות", "אוכל", "מזון" -> CategoryKey.FOOD.key
            "תחבורה", "נסיעות" -> CategoryKey.TRANSPORT.key
            "קניות", "קנייה" -> CategoryKey.SHOPPING.key
            "בידור", "הנאה" -> CategoryKey.ENTERTAINMENT.key
            "חשבונות", "שירותים" -> CategoryKey.UTILITIES.key
            "בריאות", "רפואה" -> CategoryKey.HEALTH.key
            "חינוך", "לימודים" -> CategoryKey.EDUCATION.key
            "אחר", "שונות" -> CategoryKey.OTHER.key
            
            // Russian
            "еда и рестораны", "еда", "рестораны" -> CategoryKey.FOOD.key
            "транспорт", "перевозки" -> CategoryKey.TRANSPORT.key
            "покупки", "шопинг" -> CategoryKey.SHOPPING.key
            "развлечения", "досуг" -> CategoryKey.ENTERTAINMENT.key
            "коммунальные услуги", "услуги" -> CategoryKey.UTILITIES.key
            "здравоохранение", "здоровье", "медицина" -> CategoryKey.HEALTH.key
            "образование", "учеба" -> CategoryKey.EDUCATION.key
            "другое", "прочее" -> CategoryKey.OTHER.key
            
            // Default case - assume it's already a canonical key
            else -> localizedCategory.lowercase()
        }
    }
    
    /**
     * Get localized category name from canonical key
     * This is used when displaying categories in the current language
     */
    @Composable
    fun getLocalizedCategory(categoryKey: String): String {
        return when (categoryKey.lowercase()) {
            CategoryKey.FOOD.key -> stringResource(R.string.category_food)
            CategoryKey.TRANSPORT.key -> stringResource(R.string.category_transport)
            CategoryKey.SHOPPING.key -> stringResource(R.string.category_shopping)
            CategoryKey.ENTERTAINMENT.key -> stringResource(R.string.category_entertainment)
            CategoryKey.UTILITIES.key -> stringResource(R.string.category_utilities)
            CategoryKey.HEALTH.key -> stringResource(R.string.category_health)
            CategoryKey.EDUCATION.key -> stringResource(R.string.category_education)
            CategoryKey.OTHER.key -> stringResource(R.string.category_other)
            else -> categoryKey // Return original if no mapping found
        }
    }
    
    /**
     * Get all available category options for the current language
     * This is used in dropdowns and selection lists
     */
    @Composable
    fun getAllCategoryOptions(): List<String> {
        return listOf(
            stringResource(R.string.category_food),
            stringResource(R.string.category_transport),
            stringResource(R.string.category_shopping),
            stringResource(R.string.category_entertainment),
            stringResource(R.string.category_utilities),
            stringResource(R.string.category_health),
            stringResource(R.string.category_education),
            stringResource(R.string.category_other)
        )
    }
    
    /**
     * Get emoji for category key
     */
    fun getCategoryEmoji(categoryKey: String): String {
        return when (categoryKey.lowercase()) {
            CategoryKey.FOOD.key -> "🍔"
            CategoryKey.TRANSPORT.key -> "🚗"
            CategoryKey.SHOPPING.key -> "🛍️"
            CategoryKey.ENTERTAINMENT.key -> "🎬"
            CategoryKey.UTILITIES.key -> "🧾"
            CategoryKey.HEALTH.key -> "🏥"
            CategoryKey.EDUCATION.key -> "📚"
            CategoryKey.OTHER.key -> "🗒️"
            else -> "💰"
        }
    }
}
