package com.example.spendly.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "savings")
public class SavingsEntity {
    @PrimaryKey
    @NonNull
    private String id;
    
    private String userId;
    private String name;
    private String category;
    private double targetAmount;
    private double currentAmount;
    private long completionDate;
    private String photoUri;
    private long createdAt;
    private long updatedAt;

    // Constructors
    public SavingsEntity() {}

    public SavingsEntity(@NonNull String id, String userId, String name, String category,
                        double targetAmount, double currentAmount, long completionDate,
                        String photoUri, long createdAt, long updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.completionDate = completionDate;
        this.photoUri = photoUri;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }

    public long getCompletionDate() { return completionDate; }
    public void setCompletionDate(long completionDate) { this.completionDate = completionDate; }

    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
