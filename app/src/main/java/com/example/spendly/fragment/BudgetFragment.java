package com.example.spendly.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.spendly.R;
import com.example.spendly.activity.SetBudgetActivity;
import com.example.spendly.activity.SetTotalBudgetActivity;
import com.example.spendly.repository.BudgetRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BudgetFragment extends Fragment {

    private View budgetView;
    private View emptyStateContainer;
    private TextView tvMonthYear;
    private TextView tvRemainingBudget;
    private TextView tvUsedBudget;
    private TextView tvTotalBudget;
    private TextView tvBudgetStatus;
    private TextView btnEditBudget;
    private TextView btnAddCategory;
    private ProgressBar progressBudget;
    private ProgressBar loadingProgressBar;

    // Category items
    private Map<String, CategoryViewHolder> categoryViewHolders = new HashMap<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BudgetRepository budgetRepository;
    private boolean hasBudget = false;
    private double totalBudget = 0.0;
    private double usedBudget = 0.0;
    private double remainingBudget = 0.0;
    private boolean isOfflineMode = false;

    public BudgetFragment() {
        // Required empty public constructor
    }

    public static BudgetFragment newInstance() {
        return new BudgetFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (getActivity() != null) {
            budgetRepository = BudgetRepository.getInstance(getActivity());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the root container
        View rootView = inflater.inflate(R.layout.fragment_budget, container, false);

        // Initialize views
        initViews(rootView);
        setClickListeners();

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Hide both views initially until we check budget state
        emptyStateContainer.setVisibility(View.GONE);
        budgetView.setVisibility(View.GONE);
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check budget state every time fragment becomes visible
        checkBudgetState();
    }

    private void initViews(View view) {
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        budgetView = view.findViewById(R.id.budget_content);
        tvMonthYear = view.findViewById(R.id.tv_month_year);
        tvRemainingBudget = view.findViewById(R.id.tv_remaining_budget);
        tvUsedBudget = view.findViewById(R.id.tv_used_amount);
        tvTotalBudget = view.findViewById(R.id.tv_total_budget);
        progressBudget = view.findViewById(R.id.progress_budget);
        tvBudgetStatus = view.findViewById(R.id.tv_budget_status);
        btnEditBudget = view.findViewById(R.id.btn_edit_budget);
        btnAddCategory = view.findViewById(R.id.btn_add_category);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);

        // Set current month and year
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(new Date()));
    }

    private void setClickListeners() {
        btnEditBudget.setOnClickListener(v -> {
            // Direct to SetTotalBudgetActivity to edit the budget
            Intent intent = new Intent(getActivity(), SetTotalBudgetActivity.class);
            startActivity(intent);
        });

        btnAddCategory.setOnClickListener(v -> {
            // Direct to SetBudgetActivity to add a category
            Intent intent = new Intent(getActivity(), SetBudgetActivity.class);
            if (hasBudget) {
                intent.putExtra("monthly_budget", totalBudget);
                intent.putExtra("remaining_budget", remainingBudget);
            }
            startActivity(intent);
        });
    }

    private void checkBudgetState() {
        if (getActivity() == null || budgetRepository == null) return;

        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }

        // Use the repository to check if budget exists, works offline
        budgetRepository.checkBudgetExists(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                boolean budgetExists = data.containsKey("exists") && (boolean) data.get("exists");
                isOfflineMode = data.containsKey("offline_only") && (boolean) data.get("offline_only");

                if (budgetExists) {
                    // Budget exists, get total budget data
                    getTotalBudgetData();
                } else {
                    // No budget found
                    if (loadingProgressBar != null) {
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                    showEmptyState();
                }
            }

            @Override
            public void onError(Exception e) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                showEmptyState();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error checking budget: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getTotalBudgetData() {
        budgetRepository.getTotalBudget(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                loadTotalBudgetData(data);

                // Check if categories exist
                checkBudgetCategories();
            }

            @Override
            public void onError(Exception e) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Error loading budget data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }

                showEmptyState();
            }
        });
    }

    private void checkBudgetCategories() {
        budgetRepository.checkCategoriesExist(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                boolean categoriesExist = data.containsKey("exists") && (boolean) data.get("exists");

                if (categoriesExist) {
                    // Load categories data
                    getBudgetCategories();
                } else {
                    if (loadingProgressBar != null) {
                        loadingProgressBar.setVisibility(View.GONE);
                    }

                    // Has total budget but no categories
                    navigateToSetBudget();
                }
            }

            @Override
            public void onError(Exception e) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                // Error checking categories, try setting categories
                navigateToSetBudget();
            }
        });
    }

    private void getBudgetCategories() {
        budgetRepository.getBudgetCategories(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                // Both total budget and categories exist
                hasBudget = true;
                loadBudgetCategories(data);
                showBudgetView();

                // If we're in offline mode, show notification
                if (isOfflineMode && getContext() != null) {
                    Toast.makeText(getContext(),
                            "Offline mode: Using locally saved budget data",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                // Error getting categories, try setting categories
                navigateToSetBudget();
            }
        });
    }

    private void loadTotalBudgetData(Map<String, Object> data) {
        // Extract budget data
        if (data.containsKey("monthly_budget")) {
            if (data.get("monthly_budget") instanceof Double) {
                totalBudget = (Double) data.get("monthly_budget");
            } else if (data.get("monthly_budget") instanceof Long) {
                totalBudget = ((Long) data.get("monthly_budget")).doubleValue();
            }
        }

        if (data.containsKey("remaining_budget")) {
            if (data.get("remaining_budget") instanceof Double) {
                remainingBudget = (Double) data.get("remaining_budget");
            } else if (data.get("remaining_budget") instanceof Long) {
                remainingBudget = ((Long) data.get("remaining_budget")).doubleValue();
            }
        }

        // Calculate used budget
        usedBudget = totalBudget - remainingBudget;

        // Update UI with budget amounts
        String formattedRemainingBudget = data.containsKey("remaining_formatted") ?
            data.get("remaining_formatted").toString() : formatNumber((int)remainingBudget);

        String formattedTotalBudget = data.containsKey("budget_formatted") ?
            data.get("budget_formatted").toString() : formatNumber((int)totalBudget);

        String formattedUsedBudget = formatNumber((int)usedBudget);

        tvRemainingBudget.setText("Rp" + formattedRemainingBudget);
        tvTotalBudget.setText("From Rp" + formattedTotalBudget);
        tvUsedBudget.setText("Used Rp" + formattedUsedBudget);

        // Update progress bar
        int progressPercentage = totalBudget > 0 ? (int)((usedBudget / totalBudget) * 100) : 0;
        progressBudget.setProgress(progressPercentage);

        // Update budget status message based on spending
        updateBudgetStatusMessage(progressPercentage);
    }

    private void updateBudgetStatusMessage(int progressPercentage) {
        if (progressPercentage < 50) {
            tvBudgetStatus.setText("You're on track! You can still spend Rp" + formatNumber((int)remainingBudget) + " for the rest of this month.");
        } else if (progressPercentage < 80) {
            tvBudgetStatus.setText("You've used " + progressPercentage + "% of your budget. Try to limit your spending for the rest of the month.");
        } else {
            tvBudgetStatus.setText("Warning! You've used " + progressPercentage + "% of your monthly budget. Be careful with your spending.");
        }
    }

    private void loadBudgetCategories(Map<String, Object> categoriesData) {
        // TODO: Implement dynamic creation of category cards based on data
        // For now we'll use the existing static UI
    }

    private void showBudgetView() {
        // Hide empty state and show budget content
        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            emptyStateContainer.setVisibility(View.GONE);
            budgetView.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState() {
        // Show empty state fragment and hide budget content
        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.empty_state_container, new BudgetingEmptyStateFragment())
                    .commit();

            emptyStateContainer.setVisibility(View.VISIBLE);
            budgetView.setVisibility(View.GONE);
        }
    }

    private void navigateToSetBudget() {
        // Navigate to set budget categories
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), SetBudgetActivity.class);
            intent.putExtra("monthly_budget", totalBudget);
            intent.putExtra("remaining_budget", remainingBudget);
            startActivity(intent);
        }
    }

    private String formatNumber(int number) {
        return String.format(Locale.getDefault(), "%,d", number).replace(",", ".");
    }

    // Attempt to sync data when coming back online
    public void syncData() {
        if (budgetRepository != null && isOfflineMode) {
            budgetRepository.syncWithFirebase(new BudgetRepository.BudgetCallback() {
                @Override
                public void onSuccess(Map<String, Object> data) {
                    isOfflineMode = false;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Budget data synchronized with cloud", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to sync: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // ViewHolder class for category views
    private static class CategoryViewHolder {
        View containerView;
        TextView titleText;
        TextView usageText;
        ProgressBar progressBar;
        TextView setAmountText;
        TextView spentAmountText;
        TextView totalAmountText;

        CategoryViewHolder(View view) {
            // Initialize views from the container
            containerView = view;
        }
    }
}
