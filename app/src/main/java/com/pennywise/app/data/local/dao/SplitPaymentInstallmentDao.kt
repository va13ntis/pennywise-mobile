package com.pennywise.app.data.local.dao

import androidx.room.*
import com.pennywise.app.data.local.entity.SplitPaymentInstallmentEntity
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for split payment installments
 */
@Dao
interface SplitPaymentInstallmentDao {
    
    @Insert
    suspend fun insertInstallment(installment: SplitPaymentInstallmentEntity): Long
    
    @Insert
    suspend fun insertInstallments(installments: List<SplitPaymentInstallmentEntity>): List<Long>
    
    @Update
    suspend fun updateInstallment(installment: SplitPaymentInstallmentEntity)
    
    @Delete
    suspend fun deleteInstallment(installment: SplitPaymentInstallmentEntity)
    
    @Query("SELECT * FROM split_payment_installments WHERE id = :id")
    suspend fun getInstallmentById(id: Long): SplitPaymentInstallmentEntity?
    
    @Query("SELECT * FROM split_payment_installments WHERE parentTransactionId = :parentTransactionId ORDER BY installmentNumber ASC")
    fun getInstallmentsByParentTransaction(parentTransactionId: Long): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE userId = :userId ORDER BY dueDate ASC")
    fun getInstallmentsByUser(userId: Long): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE userId = :userId AND isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidInstallmentsByUser(userId: Long): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE userId = :userId AND isPaid = 1 ORDER BY paidDate DESC")
    fun getPaidInstallmentsByUser(userId: Long): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE userId = :userId AND dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC")
    fun getInstallmentsByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE userId = :userId AND dueDate BETWEEN :startDate AND :endDate AND isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidInstallmentsByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE userId = :userId AND dueDate BETWEEN :startDate AND :endDate AND isPaid = 1 ORDER BY paidDate DESC")
    fun getPaidInstallmentsByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE userId = :userId AND category = :category ORDER BY dueDate ASC")
    fun getInstallmentsByCategory(userId: Long, category: String): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE userId = :userId AND type = :type ORDER BY dueDate ASC")
    fun getInstallmentsByType(userId: Long, type: TransactionType): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT SUM(amount) FROM split_payment_installments WHERE userId = :userId AND type = :type AND dueDate BETWEEN :startDate AND :endDate AND isPaid = 1")
    fun getTotalPaidInstallmentsByTypeAndDateRange(userId: Long, type: TransactionType, startDate: Date, endDate: Date): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM split_payment_installments WHERE userId = :userId AND type = :type AND dueDate BETWEEN :startDate AND :endDate AND isPaid = 0")
    fun getTotalUnpaidInstallmentsByTypeAndDateRange(userId: Long, type: TransactionType, startDate: Date, endDate: Date): Flow<Double?>
    
    @Query("UPDATE split_payment_installments SET isPaid = 1, paidDate = :paidDate, updatedAt = :updatedAt WHERE id = :installmentId")
    suspend fun markInstallmentAsPaid(installmentId: Long, paidDate: Date, updatedAt: Date)
    
    @Query("UPDATE split_payment_installments SET isPaid = 0, paidDate = NULL, updatedAt = :updatedAt WHERE id = :installmentId")
    suspend fun markInstallmentAsUnpaid(installmentId: Long, updatedAt: Date)
    
    @Query("DELETE FROM split_payment_installments WHERE parentTransactionId = :parentTransactionId")
    suspend fun deleteInstallmentsByParentTransaction(parentTransactionId: Long)
    
    @Query("SELECT COUNT(*) FROM split_payment_installments WHERE userId = :userId AND isPaid = 0 AND dueDate < :currentDate")
    suspend fun getOverdueInstallmentsCount(userId: Long, currentDate: Date): Int
}
