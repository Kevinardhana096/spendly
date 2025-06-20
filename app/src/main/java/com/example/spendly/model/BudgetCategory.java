package com.example.spendly.model;

public class BudgetCategory {
    private String categoryName;
    private String categoryIcon;
    private double amount;
    private double spent;
    private String formattedAmount;
    private String formattedSpent;
    private String dateAdded;
    private boolean isExpanded;

    public BudgetCategory(String categoryName, String categoryIcon, double amount, double spent,
                         String formattedAmount, String formattedSpent, String dateAdded) {
        this.categoryName = categoryName;
        this.categoryIcon = categoryIcon;
        this.amount = amount;
        this.spent = spent;
        this.formattedAmount = formattedAmount;
        this.formattedSpent = formattedSpent;
        this.dateAdded = dateAdded;
        this.isExpanded = false;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = spent;
    }

    public String getFormattedAmount() {
        return formattedAmount;
    }

    public void setFormattedAmount(String formattedAmount) {
        this.formattedAmount = formattedAmount;
    }

    public String getFormattedSpent() {
        return formattedSpent;
    }

    public void setFormattedSpent(String formattedSpent) {
        this.formattedSpent = formattedSpent;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public int getProgressPercentage() {
        if (amount <= 0) return 0;
        return (int) ((spent / amount) * 100);
    }
}
