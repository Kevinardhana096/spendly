package com.example.spendly.model;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

public class Transaction {
    private String id;
    private String userId;
    private double amount;
    private String category;
    private String account;
    private Date date;
    private String type; // "expense" or "income"
    private String formattedAmount;

    public Transaction() {
        // Empty constructor needed for Firestore
    }

    public Transaction(String userId, double amount, String category, String account, Date date, String type) {
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.account = account;
        this.date = date;
        this.type = type;

        // Format the amount
        formatAmount();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
        formatAmount();
    }

    public String getFormattedAmount() {
        return formattedAmount;
    }

    public void setFormattedAmount(String formattedAmount) {
        this.formattedAmount = formattedAmount;
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

    private void formatAmount() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        this.formattedAmount = formatter.format(amount)
                .replace("Rp", "") // Remove the currency symbol
                .trim(); // Remove any extra spaces
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", account='" + account + '\'' +
                ", date=" + date +
                ", type='" + type + '\'' +
                '}';
    }
}
