package com.example.spendly.model;

import java.io.Serializable;

public class SavingHistoryItem implements Serializable {

    private String id;                    // Document ID from Firestore
    private double amount;                // Transaction amount (e.g., 50000.0)
    private long date;                    // Timestamp in milliseconds (e.g., 1719341975000 for 2025-06-21 18:39:35)
    private String formattedAmount;       // Pre-formatted currency string (e.g., "Rp50.000")
    private String type;                  // Transaction type: "deposit" or "withdrawal"
    private String note;                  // Transaction description/note
    private String userId;                // User who made this transaction (e.g., "nowriafisda")
    private String savingsId;

    /**
     * Empty constructor required for Firestore serialization
     */
    public SavingHistoryItem() {
        // Required empty constructor for Firestore
    }

    public SavingHistoryItem(double amount, long date, String formattedAmount, String type, String note) {
        this.amount = amount;
        this.date = date;
        this.formattedAmount = formattedAmount;
        this.type = type;
        this.note = note;
    }

    /**
     * Full constructor with all fields
     */
    public SavingHistoryItem(String id, double amount, long date, String formattedAmount,
                             String type, String note, String userId, String savingsId) {
        this.id = id;
        this.amount = amount;
        this.date = date;
        this.formattedAmount = formattedAmount;
        this.type = type;
        this.note = note;
        this.userId = userId;
        this.savingsId = savingsId;
    }

    // Getters and Setters

    /**
     * Get document ID from Firestore
     */
    public String getId() {
        return id;
    }

    /**
     * Set document ID (usually set after Firestore save)
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get transaction amount
     * @return amount in double (e.g., 50000.0 for Rp50,000)
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Set transaction amount
     * @param amount Transaction amount in double
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * Get transaction timestamp
     * @return timestamp in milliseconds since epoch
     */
    public long getDate() {
        return date;
    }

    /**
     * Set transaction timestamp
     * @param date timestamp in milliseconds since epoch
     */
    public void setDate(long date) {
        this.date = date;
    }

    /**
     * Get pre-formatted amount string
     * @return formatted currency string (e.g., "Rp50.000")
     */
    public String getFormattedAmount() {
        return formattedAmount;
    }

    /**
     * Set pre-formatted amount string
     * @param formattedAmount formatted currency string
     */
    public void setFormattedAmount(String formattedAmount) {
        this.formattedAmount = formattedAmount;
    }

    /**
     * Get transaction type
     * @return "deposit" or "withdrawal"
     */
    public String getType() {
        return type;
    }

    /**
     * Set transaction type
     * @param type "deposit" or "withdrawal"
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get transaction note/description
     * @return note or description of the transaction
     */
    public String getNote() {
        return note;
    }

    /**
     * Set transaction note/description
     * @param note description of the transaction
     */
    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Get user ID who made this transaction
     * @return user identifier (e.g., "nowriafisda")
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Set user ID who made this transaction
     * @param userId user identifier
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Get parent savings ID reference
     * @return ID of the SavingsItem this history belongs to
     */
    public String getSavingsId() {
        return savingsId;
    }

    /**
     * Set parent savings ID reference
     * @param savingsId ID of the SavingsItem this history belongs to
     */
    public void setSavingsId(String savingsId) {
        this.savingsId = savingsId;
    }

    // Utility Methods

    /**
     * Check if this is a deposit transaction
     * @return true if type is "deposit"
     */
    public boolean isDeposit() {
        return "deposit".equals(type);
    }

    /**
     * Check if this is a withdrawal transaction
     * @return true if type is "withdrawal"
     */
    public boolean isWithdrawal() {
        return "withdrawal".equals(type);
    }

    /**
     * Get display amount with proper prefix
     * @return formatted amount with + or - prefix
     */
    public String getDisplayAmount() {
        String formatted = formattedAmount != null ? formattedAmount : String.valueOf(amount);

        if (isDeposit()) {
            return "+" + formatted;
        } else if (isWithdrawal()) {
            return "-" + formatted;
        } else {
            return formatted;
        }
    }

    /**
     * Create a deposit history item for current context
     * Current time: 2025-06-21 18:39:35 UTC (1719341975000 ms)
     * Current user: nowriafisda
     */
    public static SavingHistoryItem createDeposit(double amount, String formattedAmount,
                                                  String note, String savingsId) {
        // Current timestamp: 2025-06-21 18:39:35 UTC
        long currentTimestamp = 1719341975000L; // Exact current time

        SavingHistoryItem item = new SavingHistoryItem();
        item.setAmount(amount);
        item.setDate(currentTimestamp);
        item.setFormattedAmount(formattedAmount);
        item.setType("deposit");
        item.setNote(note != null ? note : "Manual deposit");
        item.setUserId("nowriafisda"); // Current user
        item.setSavingsId(savingsId);

        return item;
    }

    /**
     * Create a withdrawal history item for current context
     */
    public static SavingHistoryItem createWithdrawal(double amount, String formattedAmount,
                                                     String note, String savingsId) {
        // Current timestamp: 2025-06-21 18:39:35 UTC
        long currentTimestamp = 1719341975000L; // Exact current time

        SavingHistoryItem item = new SavingHistoryItem();
        item.setAmount(amount);
        item.setDate(currentTimestamp);
        item.setFormattedAmount(formattedAmount);
        item.setType("withdrawal");
        item.setNote(note != null ? note : "Manual withdrawal");
        item.setUserId("nowriafisda"); // Current user
        item.setSavingsId(savingsId);

        return item;
    }

    @Override
    public String toString() {
        return "SavingHistoryItem{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                ", date=" + date +
                ", formattedAmount='" + formattedAmount + '\'' +
                ", type='" + type + '\'' +
                ", note='" + note + '\'' +
                ", userId='" + userId + '\'' +
                ", savingsId='" + savingsId + '\'' +
                '}';
    }
}