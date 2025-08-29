package com.pennywise.app.data.local.converter

import androidx.room.TypeConverter
import com.pennywise.app.domain.model.UserStatus

/**
 * Type converter for UserStatus enum
 */
class UserStatusConverter {
    @TypeConverter
    fun fromUserStatus(status: UserStatus?): String? {
        return status?.name
    }
    
    @TypeConverter
    fun toUserStatus(statusString: String?): UserStatus? {
        return statusString?.let { UserStatus.valueOf(it) }
    }
}
