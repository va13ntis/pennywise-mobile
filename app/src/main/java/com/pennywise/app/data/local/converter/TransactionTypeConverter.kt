package com.pennywise.app.data.local.converter

import androidx.room.TypeConverter
import com.pennywise.app.domain.model.TransactionType

/**
 * Type converter for TransactionType enum in Room database
 */
class TransactionTypeConverter {
    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { 
            try {
                TransactionType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

