package com.example.spendly.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendly.R;
import com.example.spendly.activity.AddTransactionActivity;
import com.example.spendly.model.Transaction;
import com.example.spendly.repository.RealtimeTransactionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class HistoryFragment extends Fragment {
    
    private static final String TAG = "HistoryFragment";
    private static final int INITIAL_LOAD_LIMIT = 50;
    private static final long CACHE_DURATION = 30000; // 30 seconds
    
    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter transactionAdapter;
    private RealtimeTransactionRepository transactionRepository;
    private List<Transaction> transactions = new ArrayList<>();
    private Map<String, List<Transaction>> groupedByDateTransactions = new TreeMap<>();
    private View emptyStateView;
    private View contentView;
    private Button addTransactionButton;
    
    // Cache variables
    private boolean isDataLoaded = false;
    private long lastLoadTime = 0;

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionRepository = RealtimeTransactionRepository.getInstance(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // Initialize views
        transactionsRecyclerView = view.findViewById(R.id.transactions_recycler_view);
        ImageView filterButton = view.findViewById(R.id.btn_filter);
        emptyStateView = view.findViewById(R.id.empty_state_view);
        contentView = view.findViewById(R.id.content_view);
        addTransactionButton = emptyStateView.findViewById(R.id.add_transaction_button);

        // Set up RecyclerView
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionAdapter = new TransactionAdapter(transactions);
        transactionsRecyclerView.setAdapter(transactionAdapter);

        // Set up filter button
        filterButton.setOnClickListener(v -> {
            // Future implementation for filtering
            Toast.makeText(getContext(), "Filter functionality coming soon", Toast.LENGTH_SHORT).show();
        });        // Set up "Add Transaction" button in empty state view
        addTransactionButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTransactionActivity.class);
            startActivity(intent);
        });

        // Setup real-time data observer
        setupRealtimeDataObserver();

        return view;
    }

    private void setupRealtimeDataObserver() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Observe transactions with real-time updates
        transactionRepository.getTransactions(userId).observe(getViewLifecycleOwner(), 
            new Observer<List<Transaction>>() {
                @Override
                public void onChanged(List<Transaction> loadedTransactions) {
                    if (loadedTransactions != null) {
                        transactions.clear();
                        transactions.addAll(loadedTransactions);
                        
                        // Process data in background thread
                        new Thread(() -> {
                            groupTransactionsByDate();
                            
                            // Update UI on main thread
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    updateEmptyState();
                                    transactionAdapter.refreshData();
                                });
                            }
                        }).start();
                    }
                }
            });

        // Observe loading state
        transactionRepository.getLoadingState().observe(getViewLifecycleOwner(), 
            isLoading -> {
                // Show/hide loading indicators
                // You can add progress bar here
            });

        // Observe error messages
        transactionRepository.getErrorMessage().observe(getViewLifecycleOwner(), 
            error -> {                if (error != null && !error.isEmpty()) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Real-time observer handles data updates automatically
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up repository resources
        if (transactionRepository != null) {
            transactionRepository.cleanup();        }
    }    private void loadTransactions() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading transactions for user: " + userId);

        // Use real-time repository with LiveData observer
        transactionRepository.getTransactions(userId).observe(this, new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> loadedTransactions) {
                if (loadedTransactions != null) {
                    Log.d(TAG, "Received " + loadedTransactions.size() + " transactions");
                    
                    transactions.clear();
                    transactions.addAll(loadedTransactions);

                    // Process data in background thread for heavy operations
                    new Thread(() -> {
                        groupTransactionsByDate();
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (transactionAdapter != null) {
                                    transactionAdapter.refreshData();
                                }
                                updateEmptyState();
                                isDataLoaded = true;
                                lastLoadTime = System.currentTimeMillis();
                            });
                        }
                    }).start();
                } else {
                    Log.w(TAG, "No transactions received");
                    updateEmptyState();
                }
            }
        });

        // Observe error state
        transactionRepository.getErrorMessage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateEmptyState() {
        // Show empty state if no transactions, otherwise show content
        if (transactions.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            contentView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            contentView.setVisibility(View.VISIBLE);
        }
    }    private void groupTransactionsByDate() {
        groupedByDateTransactions.clear();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, dd MMMM yyyy", Locale.getDefault());

        for (Transaction transaction : transactions) {
            // Handle null dates gracefully
            Date transactionDate = transaction.getDate();
            String dateKey;
            
            if (transactionDate != null) {
                dateKey = dateFormatter.format(transactionDate);
            } else {
                // Use current date for transactions with null dates
                dateKey = dateFormatter.format(new Date());
                Log.w(TAG, "Transaction with null date found, using current date: " + transaction.getId());
            }

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
        }        public void refreshData() {
            // Use background thread for heavy processing
            new Thread(() -> {
                List<Object> newCombinedList = new ArrayList<>();
                
                // Add items with headers efficiently
                for (Map.Entry<String, List<Transaction>> entry : groupedByDateTransactions.entrySet()) {
                    newCombinedList.add(entry.getKey());
                    newCombinedList.addAll(entry.getValue());
                }
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        combinedList.clear();
                        combinedList.addAll(newCombinedList);
                        notifyDataSetChanged();
                    });
                }
            }).start();
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
