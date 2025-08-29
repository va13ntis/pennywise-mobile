package com.pennywise.app.data.local.converter

import androidx.room.TypeConverter
import com.pennywise.app.domain.model.UserRole

/**
 * Type converter for UserRole enum
 */
class UserRoleConverter {
    @TypeConverter
    fun fromUserRole(role: UserRole?): String? {
        return role?.name
    }
    
    @TypeConverter
    fun toUserRole(roleString: String?): UserRole? {
        return roleString?.let { UserRole.valueOf(it) }
    }
}
