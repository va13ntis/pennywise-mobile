package com.pennywise.app.data.local.converter

import androidx.room.TypeConverter
import com.pennywise.app.domain.model.RecurringPeriod

/**
 * Type converter for RecurringPeriod enum in Room database
 */
class RecurringPeriodConverter {
    @TypeConverter
    fun fromRecurringPeriod(value: RecurringPeriod?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toRecurringPeriod(value: String?): RecurringPeriod? {
        return value?.let { RecurringPeriod.valueOf(it) }
    }
}

