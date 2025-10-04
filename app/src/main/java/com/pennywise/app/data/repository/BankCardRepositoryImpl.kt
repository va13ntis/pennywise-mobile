package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.BankCardDao
import com.pennywise.app.data.local.entity.BankCardEntity
import com.pennywise.app.data.security.CardEncryptionManager
import com.pennywise.app.data.security.SecureBankCard
import com.pennywise.app.domain.model.BankCard
import com.pennywise.app.domain.repository.BankCardRepository
import com.pennywise.app.domain.validation.AuthenticationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of BankCardRepository that handles bank card data operations with authentication validation.
 * All database operations require user authentication.
 */
class BankCardRepositoryImpl @Inject constructor(
    private val bankCardDao: BankCardDao,
    private val encryptionManager: CardEncryptionManager,
    authValidator: AuthenticationValidator
) : BankCardRepository, BaseAuthenticatedRepository(authValidator) {
    
    override suspend fun insertBankCard(bankCard: BankCard): Result<Long> {
        return try {
            withAuthentication {
                // Check if alias already exists
                if (isAliasTaken(bankCard.alias)) {
                    return@withAuthentication Result.failure(Exception("Card alias already exists"))
                }
                
                // Validate payment day
                if (bankCard.paymentDay < 1 || bankCard.paymentDay > 31) {
                    return@withAuthentication Result.failure(Exception("Payment day must be between 1 and 31"))
                }
                
                // Create secure bank card with encrypted data
                val secureBankCard = SecureBankCard.fromDomainModel(bankCard, encryptionManager)
                val bankCardEntity = BankCardEntity(
                    id = secureBankCard.id,
                    alias = secureBankCard.alias,
                    lastFourDigits = secureBankCard.encryptedLastFourDigits, // Store encrypted data
                    paymentDay = secureBankCard.paymentDay,
                    isActive = secureBankCard.isActive,
                    createdAt = secureBankCard.createdAt,
                    updatedAt = secureBankCard.updatedAt
                )
                val cardId = bankCardDao.insertBankCard(bankCardEntity)
                Result.success(cardId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateBankCard(bankCard: BankCard): Result<Unit> {
        return try {
            withAuthentication {
                // Check if alias already exists (excluding current card)
                if (isAliasTaken(bankCard.alias, bankCard.id)) {
                    return@withAuthentication Result.failure(Exception("Card alias already exists"))
                }
                
                // Validate payment day
                if (bankCard.paymentDay < 1 || bankCard.paymentDay > 31) {
                    return@withAuthentication Result.failure(Exception("Payment day must be between 1 and 31"))
                }
                
                // Create secure bank card with encrypted data
                val secureBankCard = SecureBankCard.fromDomainModel(bankCard, encryptionManager)
                val bankCardEntity = BankCardEntity(
                    id = secureBankCard.id,
                    alias = secureBankCard.alias,
                    lastFourDigits = secureBankCard.encryptedLastFourDigits, // Store encrypted data
                    paymentDay = secureBankCard.paymentDay,
                    isActive = secureBankCard.isActive,
                    createdAt = secureBankCard.createdAt,
                    updatedAt = secureBankCard.updatedAt
                )
                bankCardDao.updateBankCard(bankCardEntity)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteBankCard(bankCard: BankCard): Result<Unit> {
        return try {
            withAuthentication {
                // For deletion, we only need the ID, so we can create a minimal entity
                val bankCardEntity = BankCardEntity(
                    id = bankCard.id,
                    alias = bankCard.alias,
                    lastFourDigits = "", // Not needed for deletion
                    paymentDay = bankCard.paymentDay,
                    isActive = bankCard.isActive,
                    createdAt = bankCard.createdAt,
                    updatedAt = bankCard.updatedAt
                )
                bankCardDao.deleteBankCard(bankCardEntity)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getBankCardById(cardId: Long): BankCard? = withAuthentication {
        val entity = bankCardDao.getBankCardById(cardId) ?: return@withAuthentication null
        decryptBankCardEntity(entity)
    }
    
    override fun getBankCards(): Flow<List<BankCard>> = flow {
        withAuthentication {
            bankCardDao.getAllBankCards().collect { entities ->
                emit(entities.map { decryptBankCardEntity(it) })
            }
        }
    }
    
    override fun getActiveBankCards(): Flow<List<BankCard>> = flow {
        withAuthentication {
            bankCardDao.getActiveBankCards().collect { entities ->
                emit(entities.map { decryptBankCardEntity(it) })
            }
        }
    }
    
    override suspend fun getBankCardByAlias(alias: String): BankCard? = withAuthentication {
        val entity = bankCardDao.getBankCardByAlias(alias) ?: return@withAuthentication null
        decryptBankCardEntity(entity)
    }
    
    override suspend fun updateBankCardStatus(cardId: Long, isActive: Boolean): Result<Unit> {
        return try {
            withAuthentication {
                bankCardDao.updateBankCardStatus(cardId, isActive, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateBankCardAlias(cardId: Long, alias: String): Result<Unit> {
        return try {
            withAuthentication {
                // Check if alias already exists (excluding current card)
                if (isAliasTaken(alias, cardId)) {
                    return@withAuthentication Result.failure(Exception("Card alias already exists"))
                }
                
                bankCardDao.updateBankCardAlias(cardId, alias, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateBankCardPaymentDay(cardId: Long, paymentDay: Int): Result<Unit> {
        return try {
            withAuthentication {
                // Validate payment day
                if (paymentDay < 1 || paymentDay > 31) {
                    return@withAuthentication Result.failure(Exception("Payment day must be between 1 and 31"))
                }
                
                bankCardDao.updateBankCardPaymentDay(cardId, paymentDay, System.currentTimeMillis())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isAliasTaken(alias: String, excludeId: Long): Boolean = withAuthentication {
        bankCardDao.isAliasTaken(alias, excludeId) > 0
    }
    
    override suspend fun deleteAllBankCards(): Result<Unit> {
        return try {
            withAuthentication {
                bankCardDao.deleteAllBankCards()
                Result.success(Unit)
            }
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
            alias = entity.alias,
            lastFourDigits = encryptionManager.decryptLastFourDigits(entity.lastFourDigits),
            paymentDay = entity.paymentDay,
            isActive = entity.isActive,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}