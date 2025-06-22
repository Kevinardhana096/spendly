package com.example.spendly.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.spendly.database.dao.SavingsDao;
import com.example.spendly.database.dao.TransactionDao;
import com.example.spendly.database.dao.UserDao;
import com.example.spendly.database.entity.SavingsEntity;
import com.example.spendly.database.entity.TransactionEntity;
import com.example.spendly.database.entity.UserEntity;

@Database(
    entities = {UserEntity.class, TransactionEntity.class, SavingsEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class SpendlyDatabase extends RoomDatabase {

    private static volatile SpendlyDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract TransactionDao transactionDao();
    public abstract SavingsDao savingsDao();

    public static SpendlyDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SpendlyDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SpendlyDatabase.class,
                            "spendly_database"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
