package com.example.spendly.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spendly.PieChartView;
import com.example.spendly.R;

public class ExpenseChartActivity extends AppCompatActivity {

    private ImageView btnBack, btnFilter;
    private PieChartView pieChart;
    private TextView tvTotalExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_chart);

        initViews();
        setupClickListeners();
        loadChartData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnFilter = findViewById(R.id.btn_filter);
        pieChart = findViewById(R.id.pie_chart);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnFilter.setOnClickListener(v -> {
            // Open filter options
            showFilterDialog();
        });
    }

    private void loadChartData() {
        // Load expense data from database/API
        // For demo purposes, using static data
        float shoppingPercentage = 60f; // 600,000 out of 1,000,000
        float foodPercentage = 40f; // 400,000 out of 1,000,000
        int totalExpense = 1000000;

        pieChart.setData(shoppingPercentage, foodPercentage, totalExpense);
    }

    private void showFilterDialog() {
        // Implement filter functionality
        // Show date range picker, category filter, etc.
    }
}