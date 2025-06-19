package com.example.spendly.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.spendly.R;
import com.example.spendly.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvGreeting;
    private TextView tvBalance;

    // Firebase components
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize views
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvBalance = view.findViewById(R.id.tv_balance);

        // Load user data
        loadUserData();
    }

    /**
     * Load user data from Firestore and update UI
     */
    private void loadUserData() {
        if (currentUser == null) {
            // This should not happen, but just in case
            return;
        }

        // Show loading state
        showLoadingState(true);

        // Get user document from Firestore
        mFirestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoadingState(false);

                    if (documentSnapshot.exists()) {
                        // Convert document to User object
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            updateUI(user);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoadingState(false);
                    Toast.makeText(getContext(), "Failed to load user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Update UI with user data
     */
    private void updateUI(User user) {
        // Update greeting with user's email (or name if available)
        String greeting = "Hi! " + extractName(user.getEmail());
        tvGreeting.setText(greeting);

        // Format and display balance
        String formattedBalance = formatCurrency(user.getCurrentBalance());
        tvBalance.setText(formattedBalance);
    }

    /**
     * Extract name from email address (before @ symbol)
     */
    private String extractName(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "User";
    }

    /**
     * Format currency in Indonesian Rupiah
     */
    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        return formatter.format(amount);
    }

    /**
     * Show or hide loading state
     */
    private void showLoadingState(boolean isLoading) {
        // Implement loading state UI if needed
    }
}