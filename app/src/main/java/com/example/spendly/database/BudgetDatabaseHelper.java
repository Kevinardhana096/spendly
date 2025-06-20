package com.example.spendly.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BudgetDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "budget.db";
    private static final int DATABASE_VERSION = 1;

    // Total Budget Table
    private static final String TABLE_TOTAL_BUDGET = "total_budget";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_MONTHLY_INCOME = "monthly_income";
    private static final String COLUMN_MONTHLY_BUDGET = "monthly_budget";
    private static final String COLUMN_REMAINING_BUDGET = "remaining_budget";
    private static final String COLUMN_INCOME_FORMATTED = "income_formatted";
    private static final String COLUMN_BUDGET_FORMATTED = "budget_formatted";
    private static final String COLUMN_REMAINING_FORMATTED = "remaining_formatted";
    private static final String COLUMN_SETUP_DATE = "setup_date";
    private static final String COLUMN_LAST_UPDATED = "last_updated";

    // Budget Categories Table
    private static final String TABLE_BUDGET_CATEGORIES = "budget_categories";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_FORMATTED_AMOUNT = "formatted_amount";
    private static final String COLUMN_SPENT = "spent";
    private static final String COLUMN_FORMATTED_SPENT = "formatted_spent";
    private static final String COLUMN_DATE_ADDED = "date_added";

    // Maintain a single database instance
    private SQLiteDatabase mWritableDb;
    private SQLiteDatabase mReadableDb;

    // Constructor
    public BudgetDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Total Budget Table
        String CREATE_TOTAL_BUDGET_TABLE = "CREATE TABLE " + TABLE_TOTAL_BUDGET + "("
                + COLUMN_USER_ID + " TEXT PRIMARY KEY,"
                + COLUMN_MONTHLY_INCOME + " REAL,"
                + COLUMN_MONTHLY_BUDGET + " REAL,"
                + COLUMN_REMAINING_BUDGET + " REAL,"
                + COLUMN_INCOME_FORMATTED + " TEXT,"
                + COLUMN_BUDGET_FORMATTED + " TEXT,"
                + COLUMN_REMAINING_FORMATTED + " TEXT,"
                + COLUMN_SETUP_DATE + " TEXT,"
                + COLUMN_LAST_UPDATED + " TEXT"
                + ")";
        db.execSQL(CREATE_TOTAL_BUDGET_TABLE);

        // Create Budget Categories Table
        String CREATE_BUDGET_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_BUDGET_CATEGORIES + "("
                + COLUMN_USER_ID + " TEXT,"
                + COLUMN_CATEGORY + " TEXT,"
                + COLUMN_AMOUNT + " REAL,"
                + COLUMN_FORMATTED_AMOUNT + " TEXT,"
                + COLUMN_SPENT + " REAL,"
                + COLUMN_FORMATTED_SPENT + " TEXT,"
                + COLUMN_DATE_ADDED + " TEXT,"
                + "PRIMARY KEY (" + COLUMN_USER_ID + ", " + COLUMN_CATEGORY + ")"
                + ")";
        db.execSQL(CREATE_BUDGET_CATEGORIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if they exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOTAL_BUDGET);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGET_CATEGORIES);
        // Create tables again
        onCreate(db);
    }

    // Helper methods to get database connections
    private synchronized SQLiteDatabase getReadableDb() {
        if (mReadableDb == null || !mReadableDb.isOpen()) {
            mReadableDb = getReadableDatabase();
        }
        return mReadableDb;
    }

    private synchronized SQLiteDatabase getWritableDb() {
        if (mWritableDb == null || !mWritableDb.isOpen()) {
            mWritableDb = getWritableDatabase();
        }
        return mWritableDb;
    }

    @Override
    public synchronized void close() {
        if (mReadableDb != null && mReadableDb.isOpen()) {
            mReadableDb.close();
            mReadableDb = null;
        }
        if (mWritableDb != null && mWritableDb.isOpen()) {
            mWritableDb.close();
            mWritableDb = null;
        }
        super.close();
    }

    // CRUD Operations for Total Budget Table
    public void saveTotalBudget(String userId, Map<String, Object> budgetData) {
        SQLiteDatabase db = getWritableDb();
        ContentValues values = new ContentValues();

        values.put(COLUMN_USER_ID, userId);

        // Handle numeric values with proper type checking
        if (budgetData.get("monthly_income") instanceof Double) {
            values.put(COLUMN_MONTHLY_INCOME, (Double) budgetData.get("monthly_income"));
        } else if (budgetData.get("monthly_income") instanceof Long) {
            values.put(COLUMN_MONTHLY_INCOME, ((Long) budgetData.get("monthly_income")).doubleValue());
        }

        if (budgetData.get("monthly_budget") instanceof Double) {
            values.put(COLUMN_MONTHLY_BUDGET, (Double) budgetData.get("monthly_budget"));
        } else if (budgetData.get("monthly_budget") instanceof Long) {
            values.put(COLUMN_MONTHLY_BUDGET, ((Long) budgetData.get("monthly_budget")).doubleValue());
        }

        if (budgetData.get("remaining_budget") instanceof Double) {
            values.put(COLUMN_REMAINING_BUDGET, (Double) budgetData.get("remaining_budget"));
        } else if (budgetData.get("remaining_budget") instanceof Long) {
            values.put(COLUMN_REMAINING_BUDGET, ((Long) budgetData.get("remaining_budget")).doubleValue());
        }

        // Handle string values
        values.put(COLUMN_INCOME_FORMATTED, String.valueOf(budgetData.get("income_formatted")));
        values.put(COLUMN_BUDGET_FORMATTED, String.valueOf(budgetData.get("budget_formatted")));
        values.put(COLUMN_REMAINING_FORMATTED, String.valueOf(budgetData.get("remaining_formatted")));
        values.put(COLUMN_SETUP_DATE, String.valueOf(budgetData.get("setup_date")));

        // Handle last_updated which could be a Timestamp
        Object lastUpdated = budgetData.get("last_updated");
        if (lastUpdated != null) {
            // Convert any type to string representation
            values.put(COLUMN_LAST_UPDATED, lastUpdated.toString());
        } else {
            values.put(COLUMN_LAST_UPDATED, new Date().toString());
        }

        // Insert or replace if entry already exists
        db.insertWithOnConflict(TABLE_TOTAL_BUDGET, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Map<String, Object> getTotalBudget(String userId) {
        SQLiteDatabase db = getReadableDb();
        Map<String, Object> budgetData = new HashMap<>();

        String query = "SELECT * FROM " + TABLE_TOTAL_BUDGET +
                " WHERE " + COLUMN_USER_ID + " = ?";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{userId});

            if (cursor != null && cursor.moveToFirst()) {
                budgetData.put("monthly_income", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_MONTHLY_INCOME)));
                budgetData.put("monthly_budget", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_MONTHLY_BUDGET)));
                budgetData.put("remaining_budget", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_REMAINING_BUDGET)));
                budgetData.put("income_formatted", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INCOME_FORMATTED)));
                budgetData.put("budget_formatted", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BUDGET_FORMATTED)));
                budgetData.put("remaining_formatted", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMAINING_FORMATTED)));
                budgetData.put("setup_date", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SETUP_DATE)));
                budgetData.put("last_updated", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_UPDATED)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return budgetData;
    }

    public void updateRemainingBudget(String userId, double newRemainingBudget, String formattedRemaining) {
        SQLiteDatabase db = getWritableDb();
        ContentValues values = new ContentValues();

        values.put(COLUMN_REMAINING_BUDGET, newRemainingBudget);
        values.put(COLUMN_REMAINING_FORMATTED, formattedRemaining);

        db.update(TABLE_TOTAL_BUDGET, values, COLUMN_USER_ID + " = ?",
                new String[]{userId});
    }

    // CRUD Operations for Budget Categories Table
    public void saveBudgetCategory(String userId, String category, Map<String, Object> categoryData) {
        SQLiteDatabase db = getWritableDb();
        ContentValues values = new ContentValues();

        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_CATEGORY, category);

        // Handle numeric values with proper type checking
        if (categoryData.get("amount") instanceof Double) {
            values.put(COLUMN_AMOUNT, (Double) categoryData.get("amount"));
        } else if (categoryData.get("amount") instanceof Long) {
            values.put(COLUMN_AMOUNT, ((Long) categoryData.get("amount")).doubleValue());
        }

        // Handle string values safely
        values.put(COLUMN_FORMATTED_AMOUNT, String.valueOf(categoryData.get("formatted_amount")));

        // Handle spent amount
        if (categoryData.get("spent") instanceof Double) {
            values.put(COLUMN_SPENT, (Double) categoryData.get("spent"));
        } else if (categoryData.get("spent") instanceof Long) {
            values.put(COLUMN_SPENT, ((Long) categoryData.get("spent")).doubleValue());
        } else {
            values.put(COLUMN_SPENT, 0.0); // Default to 0 if not present
        }

        values.put(COLUMN_FORMATTED_SPENT, String.valueOf(categoryData.get("formatted_spent")));

        // Handle date_added which could be a Timestamp
        Object dateAdded = categoryData.get("date_added");
        if (dateAdded != null) {
            values.put(COLUMN_DATE_ADDED, dateAdded.toString());
        } else {
            values.put(COLUMN_DATE_ADDED, new Date().toString());
        }

        // Insert or replace if entry already exists
        db.insertWithOnConflict(TABLE_BUDGET_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void saveBudgetCategories(String userId, Map<String, Object> categoriesData) {
        SQLiteDatabase db = getWritableDb();

        // Start a transaction for bulk insert
        db.beginTransaction();

        try {
            for (Map.Entry<String, Object> entry : categoriesData.entrySet()) {
                String category = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> categoryData = (Map<String, Object>) entry.getValue();

                ContentValues values = new ContentValues();
                values.put(COLUMN_USER_ID, userId);
                values.put(COLUMN_CATEGORY, category);

                // Handle numeric values with proper type checking
                if (categoryData.get("amount") instanceof Double) {
                    values.put(COLUMN_AMOUNT, (Double) categoryData.get("amount"));
                } else if (categoryData.get("amount") instanceof Long) {
                    values.put(COLUMN_AMOUNT, ((Long) categoryData.get("amount")).doubleValue());
                }

                // Handle string values safely
                values.put(COLUMN_FORMATTED_AMOUNT, String.valueOf(categoryData.get("formatted_amount")));

                // Handle spent amount
                if (categoryData.get("spent") instanceof Double) {
                    values.put(COLUMN_SPENT, (Double) categoryData.get("spent"));
                } else if (categoryData.get("spent") instanceof Long) {
                    values.put(COLUMN_SPENT, ((Long) categoryData.get("spent")).doubleValue());
                } else {
                    values.put(COLUMN_SPENT, 0.0); // Default to 0 if not present
                }

                values.put(COLUMN_FORMATTED_SPENT, String.valueOf(categoryData.get("formatted_spent")));

                // Handle date_added which could be a Timestamp
                Object dateAdded = categoryData.get("date_added");
                if (dateAdded != null) {
                    // Convert any type to string representation
                    values.put(COLUMN_DATE_ADDED, dateAdded.toString());
                } else {
                    values.put(COLUMN_DATE_ADDED, new Date().toString());
                }

                db.insertWithOnConflict(TABLE_BUDGET_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Map<String, Object> getBudgetCategories(String userId) {
        SQLiteDatabase db = getReadableDb();
        Map<String, Object> categoriesData = new HashMap<>();

        String query = "SELECT * FROM " + TABLE_BUDGET_CATEGORIES +
                " WHERE " + COLUMN_USER_ID + " = ?";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{userId});

            while (cursor != null && cursor.moveToNext()) {
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));

                Map<String, Object> categoryData = new HashMap<>();
                categoryData.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                categoryData.put("formatted_amount", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORMATTED_AMOUNT)));
                categoryData.put("spent", cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SPENT)));
                categoryData.put("formatted_spent", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORMATTED_SPENT)));
                categoryData.put("date_added", cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_ADDED)));

                categoriesData.put(category, categoryData);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return categoriesData;
    }

    public boolean hasBudgetData(String userId) {
        SQLiteDatabase db = getReadableDb();
        boolean hasData = false;

        String query = "SELECT COUNT(*) FROM " + TABLE_TOTAL_BUDGET +
                " WHERE " + COLUMN_USER_ID + " = ?";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{userId});
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                hasData = count > 0;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return hasData;
    }

    public boolean hasBudgetCategories(String userId) {
        SQLiteDatabase db = getReadableDb();
        boolean hasCategories = false;

        String query = "SELECT COUNT(*) FROM " + TABLE_BUDGET_CATEGORIES +
                " WHERE " + COLUMN_USER_ID + " = ?";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{userId});
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                hasCategories = count > 0;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return hasCategories;
    }

    public void deleteBudgetData(String userId) {
        SQLiteDatabase db = getWritableDb();

        // Delete from total budget table
        db.delete(TABLE_TOTAL_BUDGET, COLUMN_USER_ID + " = ?", new String[]{userId});

        // Delete from budget categories table
        db.delete(TABLE_BUDGET_CATEGORIES, COLUMN_USER_ID + " = ?", new String[]{userId});
    }
}
