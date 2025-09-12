package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.SplitPaymentInstallment
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for split payment installments
 */
interface SplitPaymentInstallmentRepository {
    
    /**
     * Insert a single installment
     */
    suspend fun insertInstallment(installment: SplitPaymentInstallment): Long
    
    /**
     * Insert multiple installments for a split payment
     */
    suspend fun insertInstallments(installments: List<SplitPaymentInstallment>): List<Long>
    
    /**
     * Update an installment
     */
    suspend fun updateInstallment(installment: SplitPaymentInstallment)
    
    /**
     * Delete an installment
     */
    suspend fun deleteInstallment(installment: SplitPaymentInstallment)
    
    /**
     * Get an installment by ID
     */
    suspend fun getInstallmentById(id: Long): SplitPaymentInstallment?
    
    /**
     * Get all installments for a parent transaction
     */
    fun getInstallmentsByParentTransaction(parentTransactionId: Long): Flow<List<SplitPaymentInstallment>>
    
    /**
     * Get all installments for a user
     */
    fun getInstallmentsByUser(userId: Long): Flow<List<SplitPaymentInstallment>>
    
    /**
     * Get unpaid installments for a user
     */
    fun getUnpaidInstallmentsByUser(userId: Long): Flow<List<SplitPaymentInstallment>>
    
    /**
     * Get paid installments for a user
     */
    fun getPaidInstallmentsByUser(userId: Long): Flow<List<SplitPaymentInstallment>>
    
    /**
     * Get installments within a date range
     */
    fun getInstallmentsByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallment>>
    
    /**
     * Get unpaid installments within a date range
     */
    fun getUnpaidInstallmentsByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallment>>
    
    /**
     * Get paid installments within a date range
     */
    fun getPaidInstallmentsByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallment>>
    
    /**
     * Get installments by category
     */
    fun getInstallmentsByCategory(userId: Long, category: String): Flow<List<SplitPaymentInstallment>>
    
    /**
     * Get installments by type
     */
    fun getInstallmentsByType(userId: Long, type: TransactionType): Flow<List<SplitPaymentInstallment>>
    
    /**
     * Get total amount of paid installments by type and date range
     */
    fun getTotalPaidInstallmentsByTypeAndDateRange(userId: Long, type: TransactionType, startDate: Date, endDate: Date): Flow<Double?>
    
    /**
     * Get total amount of unpaid installments by type and date range
     */
    fun getTotalUnpaidInstallmentsByTypeAndDateRange(userId: Long, type: TransactionType, startDate: Date, endDate: Date): Flow<Double?>
    
    /**
     * Mark an installment as paid
     */
    suspend fun markInstallmentAsPaid(installmentId: Long, paidDate: Date)
    
    /**
     * Mark an installment as unpaid
     */
    suspend fun markInstallmentAsUnpaid(installmentId: Long)
    
    /**
     * Delete all installments for a parent transaction
     */
    suspend fun deleteInstallmentsByParentTransaction(parentTransactionId: Long)
    
    /**
     * Get count of overdue installments for a user
     */
    suspend fun getOverdueInstallmentsCount(userId: Long): Int
}
