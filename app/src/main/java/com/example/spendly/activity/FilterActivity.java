package com.example.spendly.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spendly.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FilterActivity extends AppCompatActivity {
    private ImageView btnBack;
    private Spinner spinnerCategory;
    private LinearLayout categoryTransportation, categoryShopping, categoryHealth, categoryFood;
    private LinearLayout fromDateContainer, toDateContainer;
    private TextView tvFromDate, tvToDate;
    private Button btnSaveFilter;
    private Calendar fromCalendar, toCalendar;
    private SimpleDateFormat dateFormat;
    private String selectedCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        initViews();
        setupSpinner();
        setupClickListeners();
        initializeDates();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        spinnerCategory = findViewById(R.id.spinner_category);
        categoryTransportation = findViewById(R.id.category_transportation);
        categoryShopping = findViewById(R.id.category_shopping);
        categoryHealth = findViewById(R.id.category_health);
        categoryFood = findViewById(R.id.category_food);
        fromDateContainer = findViewById(R.id.from_date_container);
        toDateContainer = findViewById(R.id.to_date_container);
        tvFromDate = findViewById(R.id.tv_from_date);
        tvToDate = findViewById(R.id.tv_to_date);
        btnSaveFilter = findViewById(R.id.btn_save_filter);

        fromCalendar = Calendar.getInstance();
        toCalendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    private void setupSpinner() {
        String[] categories = {"All", "Transportation", "Shopping", "Health and Sport", "Food & Beverages"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        categoryTransportation.setOnClickListener(v -> selectCategory("Transportation"));
        categoryShopping.setOnClickListener(v -> selectCategory("Shopping"));
        categoryHealth.setOnClickListener(v -> selectCategory("Health and Sport"));
        categoryFood.setOnClickListener(v -> selectCategory("Food & Beverages"));

        fromDateContainer.setOnClickListener(v -> showDatePicker(true));
        toDateContainer.setOnClickListener(v -> showDatePicker(false));

        btnSaveFilter.setOnClickListener(v -> saveFilter());
    }

    private void initializeDates() {
        // Set default dates (current month)
        fromCalendar.set(Calendar.DAY_OF_MONTH, 1);
        toCalendar.add(Calendar.DAY_OF_MONTH, 10);

        updateDateDisplays();
    }

    private void selectCategory(String category) {
        selectedCategory = category;

        // Update spinner selection
        String[] categories = {"All", "Transportation", "Shopping", "Health and Sport", "Food & Beverages"};
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        Toast.makeText(this, category + " selected", Toast.LENGTH_SHORT).show();
    }

    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = isFromDate ? fromCalendar : toCalendar;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateDisplays();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateDisplays() {
        tvFromDate.setText(dateFormat.format(fromCalendar.getTime()));
        tvToDate.setText(dateFormat.format(toCalendar.getTime()));
    }

    private void saveFilter() {
        // Validate date range
        if (fromCalendar.after(toCalendar)) {
            Toast.makeText(this, "From date cannot be after To date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected category from spinner
        String spinnerCategory = this.spinnerCategory.getSelectedItem().toString();

        // Create result intent with filter data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("category", spinnerCategory);
        resultIntent.putExtra("from_date", dateFormat.format(fromCalendar.getTime()));
        resultIntent.putExtra("to_date", dateFormat.format(toCalendar.getTime()));
        resultIntent.putExtra("from_date_millis", fromCalendar.getTimeInMillis());
        resultIntent.putExtra("to_date_millis", toCalendar.getTimeInMillis());

        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, "Filter saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    public static class FilterData {
        public String category;
        public String fromDate;
        public String toDate;
        public long fromDateMillis;
        public long toDateMillis;

        public FilterData(String category, String fromDate, String toDate, long fromDateMillis, long toDateMillis) {
            this.category = category;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.fromDateMillis = fromDateMillis;
            this.toDateMillis = toDateMillis;
        }
    }
}