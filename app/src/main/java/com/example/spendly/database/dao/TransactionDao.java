package com.example.spendly.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.spendly.database.entity.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<TransactionEntity>> getAllTransactions(String userId);

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    LiveData<List<TransactionEntity>> getRecentTransactions(String userId, int limit);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<TransactionEntity>> getTransactionsByDateRange(String userId, long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY date DESC")
    LiveData<List<TransactionEntity>> getTransactionsByType(String userId, String type);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'income'")
    LiveData<Double> getTotalIncome(String userId);

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = 'expense'")
    LiveData<Double> getTotalExpenses(String userId);

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    LiveData<TransactionEntity> getTransactionById(String transactionId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTransaction(TransactionEntity transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTransactions(List<TransactionEntity> transactions);

    @Update
    void updateTransaction(TransactionEntity transaction);

    @Delete
    void deleteTransaction(TransactionEntity transaction);

    @Query("DELETE FROM transactions WHERE userId = :userId")
    void deleteAllTransactions(String userId);

    @Query("DELETE FROM transactions WHERE updatedAt < :timestamp")
    void deleteOldTransactions(long timestamp);

    // Sync methods for non-LiveData operations
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    List<TransactionEntity> getAllTransactionsSync(String userId);

    @Query("SELECT * FROM transactions WHERE userId = :userId AND updatedAt > :lastSyncTime")
    List<TransactionEntity> getModifiedTransactions(String userId, long lastSyncTime);
}
