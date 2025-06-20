package com.example.spendly.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendly.R;
import com.example.spendly.model.Transaction;
import com.example.spendly.repository.TransactionRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class HistoryFragment extends Fragment {

    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter transactionAdapter;
    private TransactionRepository transactionRepository;
    private List<Transaction> transactions = new ArrayList<>();
    private Map<String, List<Transaction>> groupedByDateTransactions = new TreeMap<>();

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionRepository = TransactionRepository.getInstance(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // Initialize views
        transactionsRecyclerView = view.findViewById(R.id.transactions_recycler_view);
        ImageView filterButton = view.findViewById(R.id.btn_filter);

        // Set up RecyclerView
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionAdapter(transactions);
        transactionsRecyclerView.setAdapter(transactionAdapter);

        // Set up filter button
        filterButton.setOnClickListener(v -> {
            // Future implementation for filtering
            Toast.makeText(getContext(), "Filter functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        // Load transactions
        loadTransactions();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload transactions when fragment becomes visible
        loadTransactions();
    }

    private void loadTransactions() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        transactionRepository.getUserTransactions(new TransactionRepository.TransactionCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                if (data.containsKey("transactions")) {
                    @SuppressWarnings("unchecked")
                    List<Transaction> loadedTransactions = (List<Transaction>) data.get("transactions");

                    if (loadedTransactions != null) {
                        transactions.clear();
                        transactions.addAll(loadedTransactions);

                        // Group transactions by date for section headers
                        groupTransactionsByDate();

                        // Update UI on main thread
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                transactionAdapter.refreshData();
                            });
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error loading transactions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void groupTransactionsByDate() {
        groupedByDateTransactions.clear();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, dd MMMM yyyy", Locale.getDefault());

        for (Transaction transaction : transactions) {
            String dateKey = dateFormatter.format(transaction.getDate());

            if (!groupedByDateTransactions.containsKey(dateKey)) {
                groupedByDateTransactions.put(dateKey, new ArrayList<>());
            }

            groupedByDateTransactions.get(dateKey).add(transaction);
        }
    }

    /**
     * Adapter for displaying transactions with date headers
     */
    private class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        private final List<Transaction> transactionList;
        private final List<Object> combinedList = new ArrayList<>(); // Contains both Transactions and String date headers

        public TransactionAdapter(List<Transaction> transactionList) {
            this.transactionList = transactionList;
            processCombinedList();
        }

        private void processCombinedList() {
            combinedList.clear();

            // Add items with headers
            for (Map.Entry<String, List<Transaction>> entry : groupedByDateTransactions.entrySet()) {
                // Add date header
                combinedList.add(entry.getKey());

                // Add all transactions for this date
                combinedList.addAll(entry.getValue());
            }
        }

        public void refreshData() {
            processCombinedList();
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return combinedList.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_date_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_transaction, parent, false);
                return new TransactionViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = combinedList.get(position);

            if (holder.getItemViewType() == TYPE_HEADER) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                headerViewHolder.dateText.setText((String) item);
            } else {
                TransactionViewHolder transactionViewHolder = (TransactionViewHolder) holder;
                Transaction transaction = (Transaction) item;
                transactionViewHolder.bindTransaction(transaction);
            }
        }

        @Override
        public int getItemCount() {
            return combinedList.size();
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView dateText;

            HeaderViewHolder(View itemView) {
                super(itemView);
                dateText = itemView.findViewById(R.id.tv_date_header);
            }
        }

        class TransactionViewHolder extends RecyclerView.ViewHolder {
            ImageView categoryIcon;
            View iconBackground;
            TextView categoryText;
            TextView accountText;
            TextView amountText;

            TransactionViewHolder(View itemView) {
                super(itemView);
                categoryIcon = itemView.findViewById(R.id.transaction_icon);
                iconBackground = itemView.findViewById(R.id.icon_background);
                categoryText = itemView.findViewById(R.id.transaction_category);
                accountText = itemView.findViewById(R.id.transaction_account);
                amountText = itemView.findViewById(R.id.transaction_amount);
            }

            void bindTransaction(Transaction transaction) {
                categoryText.setText(transaction.getCategory());

                // Set account text or hide if empty
                if (transaction.getAccount() != null && !transaction.getAccount().isEmpty()) {
                    accountText.setText(transaction.getAccount());
                    accountText.setVisibility(View.VISIBLE);
                } else {
                    accountText.setVisibility(View.GONE);
                }

                // Set amount with color based on transaction type
                String amountStr;
                if ("expense".equals(transaction.getType())) {
                    amountStr = "- " + transaction.getFormattedAmount();
                    amountText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_primary));
                } else {
                    amountStr = "+ " + transaction.getFormattedAmount();
                    amountText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
                }
                amountText.setText(amountStr);

                // Set category icon and background color
                setCategoryIconAndBackground(transaction.getCategory());
            }

            private void setCategoryIconAndBackground(String category) {
                int iconRes = R.drawable.ic_other;
                int colorRes = R.color.purple_primary;
                int backgroundRes = R.drawable.transaction_icon_background_purple;

                category = category.toLowerCase();

                if (category.contains("food") || category.contains("beverage")) {
                    iconRes = R.drawable.ic_food;
                    colorRes = R.color.orange_primary;
                    backgroundRes = R.drawable.transaction_icon_background_orange;
                } else if (category.contains("transport")) {
                    iconRes = R.drawable.ic_transportation;
                    colorRes = R.color.blue_primary;
                    backgroundRes = R.drawable.transaction_icon_background_blue;
                } else if (category.contains("shop")) {
                    iconRes = R.drawable.ic_shopping;
                    colorRes = R.color.pink_primary;
                    backgroundRes = R.drawable.transaction_icon_background_pink;
                } else if (category.contains("bill") || category.contains("util")) {
                    iconRes = R.drawable.ic_bills;
                    colorRes = R.color.purple_primary;
                    backgroundRes = R.drawable.transaction_icon_background_purple;
                } else if (category.contains("health")) {
                    iconRes = R.drawable.ic_health;
                    colorRes = R.color.green_primary;
                    backgroundRes = R.drawable.transaction_icon_background_green;
                }

                categoryIcon.setImageResource(iconRes);
                iconBackground.setBackgroundResource(backgroundRes);
            }
        }
    }
}
