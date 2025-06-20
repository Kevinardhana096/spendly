package com.example.spendly.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendly.R;
import com.example.spendly.model.BudgetCategory;

import java.util.ArrayList;
import java.util.List;

public class BudgetCategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_COLLAPSED = 0;
    private static final int VIEW_TYPE_EXPANDED = 1;

    private final Context context;
    private final List<BudgetCategory> categories;
    private final BudgetCategoryListener listener;

    public interface BudgetCategoryListener {
        void onCategoryClicked(int position);
        void onEditCategoryClicked(int position);
        void onAddExpenseClicked(int position);
    }

    public BudgetCategoryAdapter(Context context, BudgetCategoryListener listener) {
        this.context = context;
        this.categories = new ArrayList<>();
        this.listener = listener;
    }

    public void setCategories(List<BudgetCategory> categories) {
        this.categories.clear();
        if (categories != null) {
            this.categories.addAll(categories);
        }
        notifyDataSetChanged();
    }

    public void addCategory(BudgetCategory category) {
        this.categories.add(category);
        notifyItemInserted(categories.size() - 1);
    }

    public void updateCategory(int position, BudgetCategory category) {
        if (position >= 0 && position < categories.size()) {
            categories.set(position, category);
            notifyItemChanged(position);
        }
    }

    public List<BudgetCategory> getCategories() {
        return new ArrayList<>(categories);
    }

    @Override
    public int getItemViewType(int position) {
        return categories.get(position).isExpanded() ? VIEW_TYPE_EXPANDED : VIEW_TYPE_COLLAPSED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EXPANDED) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_budget_category_expanded, parent, false);
            return new ExpandedViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_budget_category_collapsed, parent, false);
            return new CollapsedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BudgetCategory category = categories.get(position);

        if (holder instanceof CollapsedViewHolder) {
            bindCollapsedViewHolder((CollapsedViewHolder) holder, category, position);
        } else if (holder instanceof ExpandedViewHolder) {
            bindExpandedViewHolder((ExpandedViewHolder) holder, category, position);
        }
    }

    private void bindCollapsedViewHolder(CollapsedViewHolder holder, BudgetCategory category, final int position) {
        holder.tvCategoryName.setText(category.getCategoryName());

        // Set icon based on category name
        setIconForCategory(holder.ivCategoryIcon, category.getCategoryName());

        // Set click listener for the entire view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClicked(position);
            }
        });
    }

    private void bindExpandedViewHolder(ExpandedViewHolder holder, BudgetCategory category, final int position) {
        holder.tvCategoryName.setText(category.getCategoryName());

        // Set icon based on category name
        setIconForCategory(holder.ivCategoryIcon, category.getCategoryName());

        // Set budget details
        holder.tvSpentAmount.setText("Rp" + category.getFormattedSpent());
        holder.tvBudgetAmount.setText("Rp" + category.getFormattedAmount());

        // Calculate and set percentage
        int percentage = category.getProgressPercentage();
        holder.tvPercentage.setText("(" + percentage + "%)");
        holder.progressBar.setProgress(percentage);

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClicked(position);
            }
        });

        holder.btnEditCategory.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditCategoryClicked(position);
            }
        });

        holder.btnAddExpense.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddExpenseClicked(position);
            }
        });
    }

    private void setIconForCategory(ImageView imageView, String categoryName) {
        // Set appropriate icon based on category name
        if (categoryName.contains("Food") || categoryName.contains("food")) {
            imageView.setImageResource(R.drawable.ic_food);
            imageView.setColorFilter(context.getResources().getColor(R.color.orange_primary));
        } else if (categoryName.contains("Transport") || categoryName.contains("transport")) {
            imageView.setImageResource(R.drawable.ic_transportation);
            imageView.setColorFilter(context.getResources().getColor(R.color.blue_primary));
        } else if (categoryName.contains("Shop") || categoryName.contains("shop")) {
            imageView.setImageResource(R.drawable.ic_shopping);
            imageView.setColorFilter(context.getResources().getColor(R.color.pink_primary));
        } else if (categoryName.contains("Health") || categoryName.contains("health")) {
            imageView.setImageResource(R.drawable.ic_health);
            imageView.setColorFilter(context.getResources().getColor(R.color.green_primary));
        } else {
            imageView.setImageResource(R.drawable.ic_other);
            imageView.setColorFilter(context.getResources().getColor(R.color.purple_primary));
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CollapsedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName;
        ImageView ivExpandIcon;

        CollapsedViewHolder(View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            ivExpandIcon = itemView.findViewById(R.id.iv_expand_icon);
        }
    }

    static class ExpandedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName;
        ImageView ivExpandIcon;
        ProgressBar progressBar;
        TextView tvSpentAmount;
        TextView tvBudgetAmount;
        TextView tvPercentage;
        TextView btnEditCategory;
        TextView btnAddExpense;

        ExpandedViewHolder(View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            ivExpandIcon = itemView.findViewById(R.id.iv_expand_icon);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvSpentAmount = itemView.findViewById(R.id.tv_spent_amount);
            tvBudgetAmount = itemView.findViewById(R.id.tv_budget_amount);
            tvPercentage = itemView.findViewById(R.id.tv_percentage);
            btnEditCategory = itemView.findViewById(R.id.btn_edit_category);
            btnAddExpense = itemView.findViewById(R.id.btn_add_expense);
        }
    }
}
