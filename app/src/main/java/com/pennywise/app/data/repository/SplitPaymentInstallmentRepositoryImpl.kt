package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.SplitPaymentInstallmentDao
import com.pennywise.app.data.local.entity.SplitPaymentInstallmentEntity
import com.pennywise.app.domain.model.SplitPaymentInstallment
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.repository.SplitPaymentInstallmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SplitPaymentInstallmentRepository
 */
@Singleton
class SplitPaymentInstallmentRepositoryImpl @Inject constructor(
    private val splitPaymentInstallmentDao: SplitPaymentInstallmentDao
) : SplitPaymentInstallmentRepository {
    
    override suspend fun insertInstallment(installment: SplitPaymentInstallment): Long {
        return splitPaymentInstallmentDao.insertInstallment(
            SplitPaymentInstallmentEntity.fromDomainModel(installment)
        )
    }
    
    override suspend fun insertInstallments(installments: List<SplitPaymentInstallment>): List<Long> {
        val entities = installments.map { SplitPaymentInstallmentEntity.fromDomainModel(it) }
        return splitPaymentInstallmentDao.insertInstallments(entities)
    }
    
    override suspend fun updateInstallment(installment: SplitPaymentInstallment) {
        splitPaymentInstallmentDao.updateInstallment(
            SplitPaymentInstallmentEntity.fromDomainModel(installment)
        )
    }
    
    override suspend fun deleteInstallment(installment: SplitPaymentInstallment) {
        splitPaymentInstallmentDao.deleteInstallment(
            SplitPaymentInstallmentEntity.fromDomainModel(installment)
        )
    }
    
    override suspend fun getInstallmentById(id: Long): SplitPaymentInstallment? {
        return splitPaymentInstallmentDao.getInstallmentById(id)?.toDomainModel()
    }
    
    override fun getInstallmentsByParentTransaction(parentTransactionId: Long): Flow<List<SplitPaymentInstallment>> {
        return splitPaymentInstallmentDao.getInstallmentsByParentTransaction(parentTransactionId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override fun getInstallmentsByUser(userId: Long): Flow<List<SplitPaymentInstallment>> {
        return splitPaymentInstallmentDao.getInstallmentsByUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override fun getUnpaidInstallmentsByUser(userId: Long): Flow<List<SplitPaymentInstallment>> {
        return splitPaymentInstallmentDao.getUnpaidInstallmentsByUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override fun getPaidInstallmentsByUser(userId: Long): Flow<List<SplitPaymentInstallment>> {
        return splitPaymentInstallmentDao.getPaidInstallmentsByUser(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override fun getInstallmentsByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallment>> {
        return splitPaymentInstallmentDao.getInstallmentsByDateRange(userId, startDate, endDate)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override fun getUnpaidInstallmentsByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallment>> {
        return splitPaymentInstallmentDao.getUnpaidInstallmentsByDateRange(userId, startDate, endDate)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override fun getPaidInstallmentsByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallment>> {
        return splitPaymentInstallmentDao.getPaidInstallmentsByDateRange(userId, startDate, endDate)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override fun getInstallmentsByCategory(userId: Long, category: String): Flow<List<SplitPaymentInstallment>> {
        return splitPaymentInstallmentDao.getInstallmentsByCategory(userId, category)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override fun getInstallmentsByType(userId: Long, type: TransactionType): Flow<List<SplitPaymentInstallment>> {
        return splitPaymentInstallmentDao.getInstallmentsByType(userId, type)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    override fun getTotalPaidInstallmentsByTypeAndDateRange(userId: Long, type: TransactionType, startDate: Date, endDate: Date): Flow<Double?> {
        return splitPaymentInstallmentDao.getTotalPaidInstallmentsByTypeAndDateRange(userId, type, startDate, endDate)
    }
    
    override fun getTotalUnpaidInstallmentsByTypeAndDateRange(userId: Long, type: TransactionType, startDate: Date, endDate: Date): Flow<Double?> {
        return splitPaymentInstallmentDao.getTotalUnpaidInstallmentsByTypeAndDateRange(userId, type, startDate, endDate)
    }
    
    override suspend fun markInstallmentAsPaid(installmentId: Long, paidDate: Date) {
        splitPaymentInstallmentDao.markInstallmentAsPaid(installmentId, paidDate, Date())
    }
    
    override suspend fun markInstallmentAsUnpaid(installmentId: Long) {
        splitPaymentInstallmentDao.markInstallmentAsUnpaid(installmentId, Date())
    }
    
    override suspend fun deleteInstallmentsByParentTransaction(parentTransactionId: Long) {
        splitPaymentInstallmentDao.deleteInstallmentsByParentTransaction(parentTransactionId)
    }
    
    override suspend fun getOverdueInstallmentsCount(userId: Long): Int {
        return splitPaymentInstallmentDao.getOverdueInstallmentsCount(userId, Date())
    }
}
