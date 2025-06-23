package com.example.spendly.model;

/**
 * User model class for storing user data in Firebase Firestore
 */
public class User {
    private String userId;
    private String email;
    private String phoneNumber;
    private String gender;
    private String dateOfBirth;
    private double currentBalance;
    private String profileImage;  // URI-based profile image (legacy support)
    private String profilePhotoBase64;  // Base64-encoded profile image

    // Required empty constructor for Firestore
    public User() {
    }    public User(String userId, String email, String phoneNumber, String gender, String dateOfBirth, double currentBalance) {
        this.userId = userId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.currentBalance = currentBalance;
        this.profileImage = null;  // Initialize as null
        this.profilePhotoBase64 = null;  // Initialize as null
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }    public void setCurrentBalance(double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getProfilePhotoBase64() {
        return profilePhotoBase64;
    }

    public void setProfilePhotoBase64(String profilePhotoBase64) {
        this.profilePhotoBase64 = profilePhotoBase64;
    }
}
