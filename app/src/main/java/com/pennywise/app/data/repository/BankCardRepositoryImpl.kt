package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.BankCardDao
import com.pennywise.app.data.local.entity.BankCardEntity
import com.pennywise.app.data.security.CardEncryptionManager
import com.pennywise.app.data.security.SecureBankCard
import com.pennywise.app.domain.model.BankCard
import com.pennywise.app.domain.repository.BankCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of BankCardRepository that handles bank card data operations
 */
class BankCardRepositoryImpl @Inject constructor(
    private val bankCardDao: BankCardDao,
    private val encryptionManager: CardEncryptionManager
) : BankCardRepository {
    
    override suspend fun insertBankCard(bankCard: BankCard): Result<Long> {
        return try {
            // Check if alias already exists for this user
            if (isAliasTaken(bankCard.userId, bankCard.alias)) {
                return Result.failure(Exception("Card alias already exists"))
            }
            
            // Validate payment day
            if (bankCard.paymentDay < 1 || bankCard.paymentDay > 31) {
                return Result.failure(Exception("Payment day must be between 1 and 31"))
            }
            
            // Create secure bank card with encrypted data
            val secureBankCard = SecureBankCard.fromDomainModel(bankCard, encryptionManager)
            val bankCardEntity = BankCardEntity(
                id = secureBankCard.id,
                userId = secureBankCard.userId,
                alias = secureBankCard.alias,
                lastFourDigits = secureBankCard.encryptedLastFourDigits, // Store encrypted data
                paymentDay = secureBankCard.paymentDay,
                isActive = secureBankCard.isActive,
                createdAt = secureBankCard.createdAt,
                updatedAt = secureBankCard.updatedAt
            )
            val cardId = bankCardDao.insertBankCard(bankCardEntity)
            Result.success(cardId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateBankCard(bankCard: BankCard): Result<Unit> {
        return try {
            // Check if alias already exists for this user (excluding current card)
            if (isAliasTaken(bankCard.userId, bankCard.alias, bankCard.id)) {
                return Result.failure(Exception("Card alias already exists"))
            }
            
            // Validate payment day
            if (bankCard.paymentDay < 1 || bankCard.paymentDay > 31) {
                return Result.failure(Exception("Payment day must be between 1 and 31"))
            }
            
            // Create secure bank card with encrypted data
            val secureBankCard = SecureBankCard.fromDomainModel(bankCard, encryptionManager)
            val bankCardEntity = BankCardEntity(
                id = secureBankCard.id,
                userId = secureBankCard.userId,
                alias = secureBankCard.alias,
                lastFourDigits = secureBankCard.encryptedLastFourDigits, // Store encrypted data
                paymentDay = secureBankCard.paymentDay,
                isActive = secureBankCard.isActive,
                createdAt = secureBankCard.createdAt,
                updatedAt = secureBankCard.updatedAt
            )
            bankCardDao.updateBankCard(bankCardEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteBankCard(bankCard: BankCard): Result<Unit> {
        return try {
            // For deletion, we only need the ID, so we can create a minimal entity
            val bankCardEntity = BankCardEntity(
                id = bankCard.id,
                userId = bankCard.userId,
                alias = bankCard.alias,
                lastFourDigits = "", // Not needed for deletion
                paymentDay = bankCard.paymentDay,
                isActive = bankCard.isActive,
                createdAt = bankCard.createdAt,
                updatedAt = bankCard.updatedAt
            )
            bankCardDao.deleteBankCard(bankCardEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getBankCardById(cardId: Long): BankCard? {
        val entity = bankCardDao.getBankCardById(cardId) ?: return null
        return decryptBankCardEntity(entity)
    }
    
    override fun getBankCardsByUserId(userId: Long): Flow<List<BankCard>> {
        return bankCardDao.getBankCardsByUserId(userId).map { entities ->
            entities.map { decryptBankCardEntity(it) }
        }
    }
    
    override fun getActiveBankCardsByUserId(userId: Long): Flow<List<BankCard>> {
        return bankCardDao.getActiveBankCardsByUserId(userId).map { entities ->
            entities.map { decryptBankCardEntity(it) }
        }
    }
    
    override suspend fun getBankCardByAlias(userId: Long, alias: String): BankCard? {
        val entity = bankCardDao.getBankCardByAlias(userId, alias) ?: return null
        return decryptBankCardEntity(entity)
    }
    
    override suspend fun updateBankCardStatus(cardId: Long, isActive: Boolean): Result<Unit> {
        return try {
            bankCardDao.updateBankCardStatus(cardId, isActive, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateBankCardAlias(cardId: Long, alias: String): Result<Unit> {
        return try {
            // Check if alias already exists for this user (excluding current card)
            if (isAliasTaken(0, alias, cardId)) { // We need to get userId first
                val card = getBankCardById(cardId)
                if (card != null && isAliasTaken(card.userId, alias, cardId)) {
                    return Result.failure(Exception("Card alias already exists"))
                }
            }
            
            bankCardDao.updateBankCardAlias(cardId, alias, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateBankCardPaymentDay(cardId: Long, paymentDay: Int): Result<Unit> {
        return try {
            // Validate payment day
            if (paymentDay < 1 || paymentDay > 31) {
                return Result.failure(Exception("Payment day must be between 1 and 31"))
            }
            
            bankCardDao.updateBankCardPaymentDay(cardId, paymentDay, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isAliasTaken(userId: Long, alias: String, excludeId: Long): Boolean {
        return bankCardDao.isAliasTaken(userId, alias, excludeId) > 0
    }
    
    override suspend fun deleteAllBankCardsByUserId(userId: Long): Result<Unit> {
        return try {
            bankCardDao.deleteAllBankCardsByUserId(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Helper method to decrypt bank card entity
     */
    private fun decryptBankCardEntity(entity: BankCardEntity): BankCard {
        return BankCard(
            id = entity.id,
            userId = entity.userId,
            alias = entity.alias,
            lastFourDigits = encryptionManager.decryptLastFourDigits(entity.lastFourDigits),
            paymentDay = entity.paymentDay,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
