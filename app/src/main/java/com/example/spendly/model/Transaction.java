package com.example.spendly.model;

import java.util.Date;

/**
 * Model class representing a transaction in Spendly app
 */
public class Transaction {
    private String id;
    private double amount;
    private String category;
    private String account;
    private Date date;
    private String type; // "expense" or "income"
    private String userId;
    private String description;
    private String formattedAmount;

    // Default constructor for Firebase
    public Transaction() {
    }

    public Transaction(String userId, double amount, String category, String account, Date date, String type) {
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.account = account;
        this.date = date;
        this.type = type;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormattedAmount() {
        return formattedAmount;
    }

    public void setFormattedAmount(String formattedAmount) {
        this.formattedAmount = formattedAmount;
    }
}
