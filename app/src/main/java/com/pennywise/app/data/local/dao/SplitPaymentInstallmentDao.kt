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
    
    @Query("SELECT * FROM split_payment_installments ORDER BY dueDate ASC")
    fun getAllInstallments(): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidInstallments(): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE isPaid = 1 ORDER BY paidDate DESC")
    fun getPaidInstallments(): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC")
    fun getInstallmentsByDateRange(startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE dueDate BETWEEN :startDate AND :endDate AND isPaid = 0 ORDER BY dueDate ASC")
    fun getUnpaidInstallmentsByDateRange(startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE dueDate BETWEEN :startDate AND :endDate AND isPaid = 1 ORDER BY paidDate DESC")
    fun getPaidInstallmentsByDateRange(startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE category = :category ORDER BY dueDate ASC")
    fun getInstallmentsByCategory(category: String): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT * FROM split_payment_installments WHERE type = :type ORDER BY dueDate ASC")
    fun getInstallmentsByType(type: TransactionType): Flow<List<SplitPaymentInstallmentEntity>>
    
    @Query("SELECT SUM(amount) FROM split_payment_installments WHERE type = :type AND dueDate BETWEEN :startDate AND :endDate AND isPaid = 1")
    fun getTotalPaidInstallmentsByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM split_payment_installments WHERE type = :type AND dueDate BETWEEN :startDate AND :endDate AND isPaid = 0")
    fun getTotalUnpaidInstallmentsByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Flow<Double?>
    
    @Query("UPDATE split_payment_installments SET isPaid = 1, paidDate = :paidDate, updatedAt = :updatedAt WHERE id = :installmentId")
    suspend fun markInstallmentAsPaid(installmentId: Long, paidDate: Date, updatedAt: Date)
    
    @Query("UPDATE split_payment_installments SET isPaid = 0, paidDate = NULL, updatedAt = :updatedAt WHERE id = :installmentId")
    suspend fun markInstallmentAsUnpaid(installmentId: Long, updatedAt: Date)
    
    @Query("DELETE FROM split_payment_installments WHERE parentTransactionId = :parentTransactionId")
    suspend fun deleteInstallmentsByParentTransaction(parentTransactionId: Long)
    
    @Query("SELECT COUNT(*) FROM split_payment_installments WHERE isPaid = 0 AND dueDate < :currentDate")
    suspend fun getOverdueInstallmentsCount(currentDate: Date): Int
}