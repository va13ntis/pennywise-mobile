package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.SplitPaymentInstallmentDao
import com.pennywise.app.data.local.entity.SplitPaymentInstallmentEntity
import com.pennywise.app.domain.model.SplitPaymentInstallment
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.repository.SplitPaymentInstallmentRepository
import com.pennywise.app.domain.validation.AuthenticationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SplitPaymentInstallmentRepository with authentication validation.
 * All database operations require user authentication.
 */
@Singleton
class SplitPaymentInstallmentRepositoryImpl @Inject constructor(
    private val splitPaymentInstallmentDao: SplitPaymentInstallmentDao,
    authValidator: AuthenticationValidator
) : SplitPaymentInstallmentRepository, BaseAuthenticatedRepository(authValidator) {
    
    override suspend fun insertInstallment(installment: SplitPaymentInstallment): Long = withAuthentication {
        splitPaymentInstallmentDao.insertInstallment(
            SplitPaymentInstallmentEntity.fromDomainModel(installment)
        )
    }
    
    override suspend fun insertInstallments(installments: List<SplitPaymentInstallment>): List<Long> = withAuthentication {
        val entities = installments.map { SplitPaymentInstallmentEntity.fromDomainModel(it) }
        splitPaymentInstallmentDao.insertInstallments(entities)
    }
    
    override suspend fun updateInstallment(installment: SplitPaymentInstallment) = withAuthentication {
        splitPaymentInstallmentDao.updateInstallment(
            SplitPaymentInstallmentEntity.fromDomainModel(installment)
        )
    }
    
    override suspend fun deleteInstallment(installment: SplitPaymentInstallment) = withAuthentication {
        splitPaymentInstallmentDao.deleteInstallment(
            SplitPaymentInstallmentEntity.fromDomainModel(installment)
        )
    }
    
    override suspend fun getInstallmentById(id: Long): SplitPaymentInstallment? = withAuthentication {
        splitPaymentInstallmentDao.getInstallmentById(id)?.toDomainModel()
    }
    
    override fun getInstallmentsByParentTransaction(parentTransactionId: Long): Flow<List<SplitPaymentInstallment>> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getInstallmentsByParentTransaction(parentTransactionId).collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getInstallments(): Flow<List<SplitPaymentInstallment>> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getAllInstallments().collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getUnpaidInstallments(): Flow<List<SplitPaymentInstallment>> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getUnpaidInstallments().collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getPaidInstallments(): Flow<List<SplitPaymentInstallment>> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getPaidInstallments().collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getInstallmentsByDateRange(startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallment>> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getInstallmentsByDateRange(startDate, endDate).collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getUnpaidInstallmentsByDateRange(startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallment>> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getUnpaidInstallmentsByDateRange(startDate, endDate).collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getPaidInstallmentsByDateRange(startDate: Date, endDate: Date): Flow<List<SplitPaymentInstallment>> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getPaidInstallmentsByDateRange(startDate, endDate).collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getInstallmentsByCategory(category: String): Flow<List<SplitPaymentInstallment>> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getInstallmentsByCategory(category).collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getInstallmentsByType(type: TransactionType): Flow<List<SplitPaymentInstallment>> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getInstallmentsByType(type).collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getTotalPaidInstallmentsByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Flow<Double?> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getTotalPaidInstallmentsByTypeAndDateRange(type, startDate, endDate).collect { value ->
                emit(value)
            }
        }
    }
    
    override fun getTotalUnpaidInstallmentsByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Flow<Double?> = flow {
        withAuthentication {
            splitPaymentInstallmentDao.getTotalUnpaidInstallmentsByTypeAndDateRange(type, startDate, endDate).collect { value ->
                emit(value)
            }
        }
    }
    
    override suspend fun markInstallmentAsPaid(installmentId: Long, paidDate: Date) = withAuthentication {
        splitPaymentInstallmentDao.markInstallmentAsPaid(installmentId, paidDate, Date())
    }
    
    override suspend fun markInstallmentAsUnpaid(installmentId: Long) = withAuthentication {
        splitPaymentInstallmentDao.markInstallmentAsUnpaid(installmentId, Date())
    }
    
    override suspend fun deleteInstallmentsByParentTransaction(parentTransactionId: Long) = withAuthentication {
        splitPaymentInstallmentDao.deleteInstallmentsByParentTransaction(parentTransactionId)
    }
    
    override suspend fun getOverdueInstallmentsCount(): Int = withAuthentication {
        splitPaymentInstallmentDao.getOverdueInstallmentsCount(Date())
    }
}
