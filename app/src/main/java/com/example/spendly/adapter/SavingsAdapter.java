package com.example.spendly.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spendly.R;
import com.example.spendly.model.SavingsItem;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SavingsAdapter extends RecyclerView.Adapter<SavingsAdapter.SavingsViewHolder> {

    private static final String TAG = "SavingsAdapter";

    // Current context - Updated to 2025-06-21 19:50:42 UTC
    private static final String CURRENT_DATE_TIME = "2025-06-21 19:50:42";
    private static final String CURRENT_USER = "nowriafisda";
    private static final long CURRENT_TIMESTAMP = 1719346242000L; // 2025-06-21 19:50:42 UTC

    private Context context;
    private List<SavingsItem> savingsList;
    private OnSavingsItemClickListener listener;
    private OnSavingItemRemoveListener removeListener;

    public interface OnSavingsItemClickListener {
        void onSavingsItemClick(SavingsItem item);
    }

    public interface OnSavingItemRemoveListener {
        void onSavingItemRemove(SavingsItem item, int position);
    }

    public SavingsAdapter(Context context, OnSavingsItemClickListener listener, OnSavingItemRemoveListener removeListener) {
        this.context = context;
        this.listener = listener;
        this.removeListener = removeListener;

        Log.d(TAG, "=== SavingsAdapter Created ===");
        Log.d(TAG, "Current Date/Time: " + CURRENT_DATE_TIME);
        Log.d(TAG, "Current User: " + CURRENT_USER);
        Log.d(TAG, "Purpose: Display savings with dynamic progress bars");
    }

    @NonNull
    @Override
    public SavingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_savings, parent, false);
        return new SavingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavingsViewHolder holder, int position) {
        if (savingsList != null && position < savingsList.size()) {
            SavingsItem item = savingsList.get(position);
            Log.d(TAG, "Binding savings item at position " + position + ": " + item.getName());
            holder.bind(item, position);
        }
    }

    @Override
    public int getItemCount() {
        return savingsList != null ? savingsList.size() : 0;
    }

    public void setSavingsList(List<SavingsItem> savingsList) {
        this.savingsList = savingsList;
        Log.d(TAG, "Savings list updated with " + (savingsList != null ? savingsList.size() : 0) + " items");
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (savingsList != null && position >= 0 && position < savingsList.size()) {
            SavingsItem removedItem = savingsList.get(position);
            Log.d(TAG, "Removing item at position " + position + ": " + removedItem.getName());
            savingsList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, savingsList.size());
        }
    }

    class SavingsViewHolder extends RecyclerView.ViewHolder {

        ImageView imgSavings;
        TextView tvSavingsName;
        TextView tvCurrentAmount;
        TextView tvTargetAmount;
        TextView tvProgressPercentage;
        TextView tvDaysRemaining;
        ProgressBar progressBar;
        View btnRemove;

        SavingsViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            imgSavings = itemView.findViewById(R.id.img_savings);
            tvSavingsName = itemView.findViewById(R.id.tv_savings_name);
            tvCurrentAmount = itemView.findViewById(R.id.tv_current_amount);
            tvTargetAmount = itemView.findViewById(R.id.tv_target_amount);
            tvProgressPercentage = itemView.findViewById(R.id.tv_progress_percentage);
            tvDaysRemaining = itemView.findViewById(R.id.tv_days_remaining);
            progressBar = itemView.findViewById(R.id.progress_bar);
            btnRemove = itemView.findViewById(R.id.btn_remove);

            // Log view initialization
            Log.d(TAG, "ViewHolder views initialized:");
            Log.d(TAG, "- progressBar found: " + (progressBar != null));
            Log.d(TAG, "- tvProgressPercentage found: " + (tvProgressPercentage != null));
            Log.d(TAG, "- tvCurrentAmount found: " + (tvCurrentAmount != null));
        }

        void bind(SavingsItem item, int position) {
            Log.d(TAG, "=== Binding Savings Item ===");
            Log.d(TAG, "Item: " + item.getName());
            Log.d(TAG, "Position: " + position);
            Log.d(TAG, "Current: Rp" + formatNumber(item.getCurrentAmount()));
            Log.d(TAG, "Target: Rp" + formatNumber(item.getTargetAmount()));
            Log.d(TAG, "Timestamp: " + CURRENT_DATE_TIME);

            // Set savings name
            if (tvSavingsName != null) {
                tvSavingsName.setText(item.getName());
            }

            // Set current amount
            if (tvCurrentAmount != null) {
                tvCurrentAmount.setText(formatCurrency(item.getCurrentAmount()));
            }

            // Set target amount
            if (tvTargetAmount != null) {
                tvTargetAmount.setText(formatCurrency(item.getTargetAmount()));
            }

            // ✅ CALCULATE AND UPDATE PROGRESS BAR
            updateProgressBar(item);

            // Calculate and display days remaining
            updateDaysRemaining(item);

            // Load image
            loadSavingsImage(item);

            // Set click listeners
            setupClickListeners(item, position);
        }

        /**
         * ✅ UPDATE PROGRESS BAR - Real-time calculation
         */
        private void updateProgressBar(SavingsItem item) {
            double current = item.getCurrentAmount();
            double target = item.getTargetAmount();

            // Calculate progress percentage
            int progressPercentage = 0;
            if (target > 0) {
                progressPercentage = (int) Math.round((current / target) * 100);
                // Cap at 100%
                if (progressPercentage > 100) {
                    progressPercentage = 100;
                }
            }

            Log.d(TAG, "Progress calculation for " + item.getName() + ":");
            Log.d(TAG, "- Current: Rp" + formatNumber(current));
            Log.d(TAG, "- Target: Rp" + formatNumber(target));
            Log.d(TAG, "- Progress: " + progressPercentage + "%");
            Log.d(TAG, "- Calculation: (" + formatNumber(current) + " / " + formatNumber(target) + ") * 100");

            // Update progress bar
            if (progressBar != null) {
                progressBar.setProgress(progressPercentage);
                Log.d(TAG, "✅ Progress bar updated to " + progressPercentage + "%");
            } else {
                Log.e(TAG, "❌ Progress bar is null!");
            }

            // Update progress percentage text
            if (tvProgressPercentage != null) {
                tvProgressPercentage.setText(progressPercentage + "%");
                Log.d(TAG, "✅ Progress text updated to " + progressPercentage + "%");
            } else {
                Log.e(TAG, "❌ Progress percentage text view is null!");
            }

            // Log completion status
            if (progressPercentage >= 100) {
                Log.d(TAG, "🎉 SAVINGS GOAL COMPLETED for " + item.getName() + "!");
            } else {
                double remaining = target - current;
                Log.d(TAG, "💰 Still need Rp" + formatNumber(remaining) + " to complete " + item.getName());
            }
        }

        /**
         * Calculate and display days remaining
         */
        private void updateDaysRemaining(SavingsItem item) {
            if (tvDaysRemaining != null && item.getCompletionDate() > 0) {
                // Calculate days from current date: 2025-06-21 19:50:42
                Date targetDate = new Date(item.getCompletionDate());
                Date currentDate = new Date(CURRENT_TIMESTAMP);

                long diffInMillis = targetDate.getTime() - currentDate.getTime();
                long daysRemaining = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

                String daysText;
                if (daysRemaining > 0) {
                    daysText = daysRemaining + " days left";
                } else if (daysRemaining == 0) {
                    daysText = "Today is the target!";
                } else {
                    daysText = Math.abs(daysRemaining) + " days overdue";
                }

                tvDaysRemaining.setText(daysText);

                Log.d(TAG, "Days calculation for " + item.getName() + ":");
                Log.d(TAG, "- Target date: " + formatDate(targetDate));
                Log.d(TAG, "- Current date: " + formatDate(currentDate));
                Log.d(TAG, "- Days remaining: " + daysRemaining);
            } else {
                if (tvDaysRemaining != null) {
                    tvDaysRemaining.setText("No target date");
                }
            }
        }

        /**
         * Load savings image
         */
        private void loadSavingsImage(SavingsItem item) {
            if (imgSavings != null) {
                if (item.getPhotoUri() != null && !item.getPhotoUri().isEmpty()) {
                    Log.d(TAG, "Loading image for " + item.getName() + ": " + item.getPhotoUri());

                    try {
                        Glide.with(context)
                                .load(item.getPhotoUri())
                                .placeholder(R.drawable.placeholder_green)
                                .error(R.drawable.placeholder_green)
                                .centerCrop()
                                .into(imgSavings);
                    } catch (Exception e) {
                        Log.e(TAG, "Error loading image with Glide", e);
                        imgSavings.setImageResource(R.drawable.placeholder_green);
                    }
                } else {
                    Log.d(TAG, "No image URI for " + item.getName() + ", using placeholder");
                    imgSavings.setImageResource(R.drawable.placeholder_green);
                }
            }
        }

        /**
         * Setup click listeners
         */
        private void setupClickListeners(SavingsItem item, int position) {
            // Item click listener
            itemView.setOnClickListener(v -> {
                Log.d(TAG, "Savings item clicked: " + item.getName() + " by user: " + CURRENT_USER);
                Log.d(TAG, "Current progress: " + calculateProgressPercentage(item) + "%");

                if (listener != null) {
                    listener.onSavingsItemClick(item);
                } else {
                    Log.e(TAG, "Click listener is null!");
                }
            });

            // Remove button click listener
            if (btnRemove != null) {
                btnRemove.setOnClickListener(v -> {
                    Log.d(TAG, "Remove button clicked for: " + item.getName());
                    if (removeListener != null) {
                        removeListener.onSavingItemRemove(item, position);
                    }
                });
            }
        }

        /**
         * Calculate progress percentage
         */
        private int calculateProgressPercentage(SavingsItem item) {
            if (item.getTargetAmount() > 0) {
                int percentage = (int) Math.round((item.getCurrentAmount() / item.getTargetAmount()) * 100);
                return Math.min(percentage, 100); // Cap at 100%
            }
            return 0;
        }
    }

    // Helper methods
    private String formatCurrency(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return "Rp" + formatter.format(amount).replace(",", ".");
    }

    private String formatNumber(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount).replace(",", ".");
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(date);
    }
}