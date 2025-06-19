package com.example.spendly.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spendly.R;

public class AddTransactionActivity extends AppCompatActivity {

    private ImageView btnBack;
    private CardView btnExpenses, btnDate;
    private EditText etAmount;
    private CardView accountCash, accountBni, accountShopee;
    private CardView categoryFood;
    private Button btnAddMore, btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnExpenses = findViewById(R.id.btn_expenses);
        btnDate = findViewById(R.id.btn_date);
        etAmount = findViewById(R.id.et_amount);
        accountCash = findViewById(R.id.account_cash);
        accountBni = findViewById(R.id.account_bni);
        accountShopee = findViewById(R.id.account_shopee);
        categoryFood = findViewById(R.id.category_food);
        btnAddMore = findViewById(R.id.btn_add_more);
        btnDone = findViewById(R.id.btn_done);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnExpenses.setOnClickListener(v -> {
            // Toggle to expenses view
        });

        btnDate.setOnClickListener(v -> {
            // Show date picker
        });

        accountCash.setOnClickListener(v -> selectAccount("Cash"));
        accountBni.setOnClickListener(v -> selectAccount("BNI"));
        accountShopee.setOnClickListener(v -> selectAccount("ShopeePay"));

        categoryFood.setOnClickListener(v -> {
            // Show category selector
        });

        btnAddMore.setOnClickListener(v -> {
            // Add more transactions
            saveTransaction(true);
        });

        btnDone.setOnClickListener(v -> {
            // Save and finish
            saveTransaction(false);
        });
    }

    private void selectAccount(String accountType) {
        // Update UI to show selected account
        Toast.makeText(this, accountType + " selected", Toast.LENGTH_SHORT).show();
    }

    private void saveTransaction(boolean addMore) {
        String amount = etAmount.getText().toString().trim();

        if (amount.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save transaction logic here
        Toast.makeText(this, "Transaction saved", Toast.LENGTH_SHORT).show();

        if (!addMore) {
            finish();
        } else {
            // Clear form for next transaction
            etAmount.setText("");
        }
    }
}