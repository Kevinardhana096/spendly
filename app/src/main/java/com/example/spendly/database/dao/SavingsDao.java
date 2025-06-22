package com.example.spendly.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.spendly.database.entity.SavingsEntity;

import java.util.List;

@Dao
public interface SavingsDao {

    @Query("SELECT * FROM savings WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<SavingsEntity>> getAllSavings(String userId);

    @Query("SELECT * FROM savings WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    LiveData<List<SavingsEntity>> getRecentSavings(String userId, int limit);

    @Query("SELECT * FROM savings WHERE id = :savingsId")
    LiveData<SavingsEntity> getSavingsById(String savingsId);

    @Query("SELECT SUM(currentAmount) FROM savings WHERE userId = :userId")
    LiveData<Double> getTotalSavingsAmount(String userId);

    @Query("SELECT SUM(targetAmount) FROM savings WHERE userId = :userId")
    LiveData<Double> getTotalTargetAmount(String userId);

    @Query("SELECT * FROM savings WHERE userId = :userId AND category = :category ORDER BY createdAt DESC")
    LiveData<List<SavingsEntity>> getSavingsByCategory(String userId, String category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSavings(SavingsEntity savings);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSavingsList(List<SavingsEntity> savingsList);

    @Update
    void updateSavings(SavingsEntity savings);

    @Delete
    void deleteSavings(SavingsEntity savings);

    @Query("DELETE FROM savings WHERE userId = :userId")
    void deleteAllSavings(String userId);

    @Query("DELETE FROM savings WHERE updatedAt < :timestamp")
    void deleteOldSavings(long timestamp);

    // Sync methods for non-LiveData operations
    @Query("SELECT * FROM savings WHERE userId = :userId ORDER BY createdAt DESC")
    List<SavingsEntity> getAllSavingsSync(String userId);

    @Query("SELECT * FROM savings WHERE userId = :userId AND updatedAt > :lastSyncTime")
    List<SavingsEntity> getModifiedSavings(String userId, long lastSyncTime);
}
