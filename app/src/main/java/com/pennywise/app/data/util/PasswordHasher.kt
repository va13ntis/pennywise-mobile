package com.pennywise.app.data.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Utility class for secure password hashing and verification
 * 
 * Note: This is a simplified implementation for the prototype.
 * In production, consider using BCrypt or Argon2 for better security.
 */
class PasswordHasher {
    
    private val random = SecureRandom()
    
    /**
     * Hash a password using SHA-256 with salt
     * @param password The plain text password to hash
     * @return The hashed password with salt
     */
    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val hashedPassword = hashWithSalt(password, salt)
        return "$salt:$hashedPassword"
    }
    
    /**
     * Verify a password against a stored hash
     * @param password The plain text password to verify
     * @param storedHash The stored hash to verify against
     * @return true if the password matches, false otherwise
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        return try {
            val parts = storedHash.split(":")
            if (parts.size != 2) return false
            
            val salt = parts[0]
            val storedPasswordHash = parts[1]
            val computedHash = hashWithSalt(password, salt)
            
            storedPasswordHash == computedHash
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generate a random salt
     * @return Base64 encoded salt
     */
    private fun generateSalt(): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }
    
    /**
     * Hash a password with a given salt
     * @param password The plain text password
     * @param salt The salt to use
     * @return The hashed password
     */
    private fun hashWithSalt(password: String, salt: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val saltedPassword = "$password$salt"
        val hashedBytes = messageDigest.digest(saltedPassword.toByteArray())
        return Base64.getEncoder().encodeToString(hashedBytes)
    }
}
