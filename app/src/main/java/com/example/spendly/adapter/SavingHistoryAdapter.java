package com.example.spendly.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendly.R;
import com.example.spendly.model.SavingHistoryItem;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SavingHistoryAdapter extends RecyclerView.Adapter<SavingHistoryAdapter.ViewHolder> {

    private static final String TAG = "SavingHistoryAdapter";

    // Current context info - Updated to 2025-06-21 18:44:37
    private static final String CURRENT_DATE_TIME = "2025-06-21 18:44:37";
    private static final String CURRENT_USER = "nowriafisda";
    private static final long CURRENT_TIMESTAMP = 1719342277000L; // 2025-06-21 18:44:37 UTC

    private List<SavingHistoryItem> historyList;
    private OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(SavingHistoryItem historyItem);
    }

    public SavingHistoryAdapter(List<SavingHistoryItem> historyList, OnHistoryItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;

        Log.d(TAG, "=== SavingHistoryAdapter Created ===");
        Log.d(TAG, "Current context: " + CURRENT_DATE_TIME + " (User: " + CURRENT_USER + ")");
        Log.d(TAG, "Current timestamp: " + CURRENT_TIMESTAMP);
        Log.d(TAG, "Initial history list size: " + (historyList != null ? historyList.size() : 0));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Creating view holder for history item");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saving_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (historyList != null && position < historyList.size()) {
            SavingHistoryItem item = historyList.get(position);
            Log.d(TAG, "Binding history item at position " + position + ": " +
                    (item.getFormattedAmount() != null ? item.getFormattedAmount() : item.getAmount()));
            holder.bind(item, position);
        } else {
            Log.e(TAG, "Invalid position or null historyList at position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        int count = historyList != null ? historyList.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    public void updateHistoryList(List<SavingHistoryItem> newHistoryList) {
        Log.d(TAG, "Updating history list from " +
                (this.historyList != null ? this.historyList.size() : 0) +
                " to " + (newHistoryList != null ? newHistoryList.size() : 0) + " items");

        this.historyList = newHistoryList;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView historyIcon;
        TextView historyAmount;
        TextView historyDate;
        TextView historyNote;

        ViewHolder(View itemView) {
            super(itemView);

            // Initialize views from item_saving_history.xml
            historyIcon = itemView.findViewById(R.id.history_icon);
            historyAmount = itemView.findViewById(R.id.history_amount);
            historyDate = itemView.findViewById(R.id.history_date);
            historyNote = itemView.findViewById(R.id.history_note);

            // Log view initialization success
            Log.d(TAG, "ViewHolder initialized successfully:");
            Log.d(TAG, "- history_icon found: " + (historyIcon != null));
            Log.d(TAG, "- history_amount found: " + (historyAmount != null));
            Log.d(TAG, "- history_date found: " + (historyDate != null));
            Log.d(TAG, "- history_note found: " + (historyNote != null));

            if (historyIcon == null || historyAmount == null || historyDate == null || historyNote == null) {
                Log.e(TAG, "ERROR: Some views are null! Check R.layout.item_saving_history and view IDs");
            }
        }

        void bind(SavingHistoryItem item, int position) {
            Log.d(TAG, "Binding item at position " + position);

            if (item == null) {
                Log.e(TAG, "SavingHistoryItem is null at position " + position);
                return;
            }

            // Set amount with proper formatting and color
            bindAmount(item);

            // Set date with relative time calculation from current context
            bindDate(item);

            // Set note
            bindNote(item);

            // Set icon and colors based on type
            bindIconAndColors(item);

            // Set click listener
            bindClickListener(item, position);

            Log.d(TAG, "Successfully bound item at position " + position +
                    " - Amount: " + item.getFormattedAmount() +
                    ", Type: " + item.getType() +
                    ", Date: " + new Date(item.getDate()));
        }

        private void bindAmount(SavingHistoryItem item) {
            if (historyAmount == null) {
                Log.e(TAG, "historyAmount view is null - cannot bind amount");
                return;
            }

            String amountText;
            if (item.getFormattedAmount() != null && !item.getFormattedAmount().isEmpty()) {
                amountText = item.getFormattedAmount();
            } else {
                // Format amount if not pre-formatted
                amountText = formatCurrency(item.getAmount());
            }

            // Add prefix and set color based on type
            if ("deposit".equals(item.getType())) {
                amountText = "+" + amountText;
                historyAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.progress_green));
                Log.d(TAG, "Set deposit amount: " + amountText + " (green)");
            } else if ("withdrawal".equals(item.getType())) {
                amountText = "-" + amountText;
                historyAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.red_primary));
                Log.d(TAG, "Set withdrawal amount: " + amountText + " (red)");
            } else {
                // Default to positive (deposit)
                amountText = "+" + amountText;
                historyAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.progress_green));
                Log.d(TAG, "Set default amount: " + amountText + " (green - unknown type: " + item.getType() + ")");
            }

            historyAmount.setText(amountText);
        }

        private void bindDate(SavingHistoryItem item) {
            if (historyDate == null) {
                Log.e(TAG, "historyDate view is null - cannot bind date");
                return;
            }

            Date itemDate = new Date(item.getDate());
            String dateText = formatDateTimeWithRelative(itemDate);

            historyDate.setText(dateText);
            Log.d(TAG, "Set date: " + dateText + " (original timestamp: " + item.getDate() + ")");
        }

        private void bindNote(SavingHistoryItem item) {
            if (historyNote == null) {
                Log.e(TAG, "historyNote view is null - cannot bind note");
                return;
            }

            String noteText = item.getNote();
            if (noteText == null || noteText.trim().isEmpty()) {
                // Set default note based on type and current user
                if ("deposit".equals(item.getType())) {
                    noteText = "Manual deposit - " + CURRENT_USER;
                } else if ("withdrawal".equals(item.getType())) {
                    noteText = "Manual withdrawal - " + CURRENT_USER;
                } else {
                    noteText = "Saving transaction - " + CURRENT_USER;
                }
            }

            historyNote.setText(noteText);
            Log.d(TAG, "Set note: " + noteText);
        }

        private void bindIconAndColors(SavingHistoryItem item) {
            if (historyIcon == null) {
                Log.e(TAG, "historyIcon view is null - cannot bind icon");
                return;
            }

            // Set icon based on type
            if ("deposit".equals(item.getType())) {
                historyIcon.setImageResource(R.drawable.ic_add_green);
                Log.d(TAG, "Set deposit icon (ic_add_green)");
            } else if ("withdrawal".equals(item.getType())) {
                historyIcon.setImageResource(R.drawable.ic_remove_red);
                Log.d(TAG, "Set withdrawal icon (ic_remove_red)");
            } else {
                // Default to deposit icon
                historyIcon.setImageResource(R.drawable.ic_add_green);
                Log.d(TAG, "Set default icon (ic_add_green) for unknown type: " + item.getType());
            }
        }

        private void bindClickListener(SavingHistoryItem item, int position) {
            itemView.setOnClickListener(v -> {
                Log.d(TAG, "History item clicked at position " + position);
                Log.d(TAG, "Clicked item details: " + item.getFormattedAmount() +
                        " on " + formatDateTime(new Date(item.getDate())) +
                        " by user: " + CURRENT_USER);

                if (listener != null) {
                    listener.onHistoryItemClick(item);
                } else {
                    Log.w(TAG, "No click listener set");
                }
            });
        }

        private String formatCurrency(double amount) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            return "Rp" + formatter.format(amount).replace(",", ".");
        }

        private String formatDateTime(Date date) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            return sdf.format(date);
        }

        private String formatDateTimeWithRelative(Date itemDate) {
            // Current date context: 2025-06-21 18:44:37 UTC (CURRENT_TIMESTAMP = 1719342277000L)
            Date currentDate = new Date(CURRENT_TIMESTAMP);

            // Calculate time difference
            long diffInMillis = Math.abs(currentDate.getTime() - itemDate.getTime());
            long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
            long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

            String baseFormat = formatDateTime(itemDate);

            // Add relative time if recent
            if (diffInMinutes < 1) {
                return baseFormat + " (Just now)";
            } else if (diffInMinutes < 60) {
                return baseFormat + " (" + diffInMinutes + " min ago)";
            } else if (diffInHours < 24) {
                return baseFormat + " (" + diffInHours + " hours ago)";
            } else if (diffInDays < 7) {
                return baseFormat + " (" + diffInDays + " days ago)";
            } else {
                return baseFormat;
            }
        }
    }

    // Public utility methods
    public void addHistoryItem(SavingHistoryItem newItem) {
        if (historyList != null && newItem != null) {
            Log.d(TAG, "Adding new history item: " + newItem.getFormattedAmount() +
                    " at " + CURRENT_DATE_TIME);

            // Add to beginning of list (most recent first)
            historyList.add(0, newItem);
            notifyItemInserted(0);

            Log.d(TAG, "History list size after addition: " + historyList.size());
        } else {
            Log.e(TAG, "Cannot add history item - list or item is null");
        }
    }

    public void removeHistoryItem(int position) {
        if (historyList != null && position >= 0 && position < historyList.size()) {
            SavingHistoryItem removedItem = historyList.get(position);
            Log.d(TAG, "Removing history item at position " + position + ": " +
                    removedItem.getFormattedAmount());

            historyList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, historyList.size());

            Log.d(TAG, "History list size after removal: " + historyList.size());
        } else {
            Log.e(TAG, "Cannot remove history item - invalid position: " + position);
        }
    }

    public boolean isEmpty() {
        boolean empty = historyList == null || historyList.isEmpty();
        Log.d(TAG, "isEmpty check: " + empty);
        return empty;
    }

    public int getHistoryCount() {
        int count = historyList != null ? historyList.size() : 0;
        Log.d(TAG, "getHistoryCount: " + count);
        return count;
    }
}