package com.pennywise.app.data.util

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for PasswordHasher utility class
 */
class PasswordHasherTest {
    
    private val passwordHasher = PasswordHasher()
    
    @Test
    fun `hashPassword should generate different hashes for same password`() {
        val password = "testPassword123"
        val hash1 = passwordHasher.hashPassword(password)
        val hash2 = passwordHasher.hashPassword(password)
        
        // Hashes should be different due to random salt
        assertNotEquals("Hashes should be different due to random salt", hash1, hash2)
        
        // Both hashes should be valid
        assertTrue("First hash should be valid", passwordHasher.verifyPassword(password, hash1))
        assertTrue("Second hash should be valid", passwordHasher.verifyPassword(password, hash2))
    }
    
    @Test
    fun `verifyPassword should return true for correct password`() {
        val password = "correctPassword"
        val hash = passwordHasher.hashPassword(password)
        
        assertTrue("Password verification should succeed", passwordHasher.verifyPassword(password, hash))
    }
    
    @Test
    fun `verifyPassword should return false for incorrect password`() {
        val correctPassword = "correctPassword"
        val incorrectPassword = "wrongPassword"
        val hash = passwordHasher.hashPassword(correctPassword)
        
        assertFalse("Password verification should fail", passwordHasher.verifyPassword(incorrectPassword, hash))
    }
    
    @Test
    fun `verifyPassword should return false for empty password`() {
        val password = "testPassword"
        val hash = passwordHasher.hashPassword(password)
        
        assertFalse("Empty password should not verify", passwordHasher.verifyPassword("", hash))
    }
    
    @Test
    fun `hashPassword should handle empty password`() {
        val emptyPassword = ""
        val hash = passwordHasher.hashPassword(emptyPassword)
        
        assertTrue("Empty password hash should be valid", passwordHasher.verifyPassword(emptyPassword, hash))
    }
    
    @Test
    fun `hashPassword should handle special characters`() {
        val passwordWithSpecialChars = "p@ssw0rd!@#$%^&*()"
        val hash = passwordHasher.hashPassword(passwordWithSpecialChars)
        
        assertTrue("Password with special characters should verify", 
            passwordHasher.verifyPassword(passwordWithSpecialChars, hash))
    }
    
    @Test
    fun `hashPassword should handle very long password`() {
        val longPassword = "a".repeat(1000)
        val hash = passwordHasher.hashPassword(longPassword)
        
        assertTrue("Long password should verify", passwordHasher.verifyPassword(longPassword, hash))
    }
    
    @Test
    fun `verifyPassword should return false for invalid hash format`() {
        val password = "testPassword"
        val invalidHash = "invalidHashFormat"
        
        assertFalse("Invalid hash format should not verify", passwordHasher.verifyPassword(password, invalidHash))
    }
    
    @Test
    fun `verifyPassword should return false for null hash`() {
        val password = "testPassword"
        
        assertFalse("Null hash should not verify", passwordHasher.verifyPassword(password, null))
    }
    
    @Test
    fun `hashPassword should generate hash with salt format`() {
        val password = "testPassword"
        val hash = passwordHasher.hashPassword(password)
        
        // Hash should contain salt and hash separated by colon
        assertTrue("Hash should contain colon separator", hash.contains(":"))
        
        val parts = hash.split(":")
        assertEquals("Hash should have exactly 2 parts", 2, parts.size)
        assertTrue("First part should be salt", parts[0].isNotEmpty())
        assertTrue("Second part should be hash", parts[1].isNotEmpty())
    }
}
