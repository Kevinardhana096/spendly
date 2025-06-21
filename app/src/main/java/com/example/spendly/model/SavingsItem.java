package com.example.spendly.model;

import java.io.Serializable;

public class SavingsItem implements Serializable {
    private String id;
    private String name;
    private String category;
    private double targetAmount;
    private double currentAmount;
    private long completionDate;
    private String photoUri;
    private long createdAt;

    // Empty constructor for Firestore
    public SavingsItem() {
    }

    public SavingsItem(String id, String name, String category, double targetAmount,
                       double currentAmount, long completionDate, String photoUri, long createdAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.completionDate = completionDate;
        this.photoUri = photoUri;
        this.createdAt = createdAt;
    }

    // All your existing getters and setters remain the same
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
}