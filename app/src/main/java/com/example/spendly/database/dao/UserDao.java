package com.example.spendly.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.spendly.database.entity.UserEntity;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users WHERE userId = :userId")
    LiveData<UserEntity> getUser(String userId);

    @Query("SELECT * FROM users WHERE userId = :userId")
    UserEntity getUserSync(String userId);

    @Query("SELECT currentBalance FROM users WHERE userId = :userId")
    LiveData<Double> getUserBalance(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    @Update
    void updateUser(UserEntity user);

    @Query("UPDATE users SET currentBalance = :balance, lastSyncTime = :syncTime WHERE userId = :userId")
    void updateUserBalance(String userId, double balance, long syncTime);

    @Query("UPDATE users SET lastSyncTime = :syncTime WHERE userId = :userId")
    void updateLastSyncTime(String userId, long syncTime);

    @Query("DELETE FROM users WHERE userId = :userId")
    void deleteUser(String userId);

    @Query("SELECT lastSyncTime FROM users WHERE userId = :userId")
    long getLastSyncTime(String userId);
}
