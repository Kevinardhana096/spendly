package com.example.spendly.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spendly.R;
import com.example.spendly.model.SavingsItem;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SavingsAdapter extends RecyclerView.Adapter<SavingsAdapter.SavingsViewHolder> {

    private List<SavingsItem> savingsList;
    private Context context;
    private OnSavingsItemClickListener listener;
    private OnSavingItemRemoveListener removeListener;

    public interface OnSavingsItemClickListener {
        void onSavingsItemClick(SavingsItem item);
    }

    public interface OnSavingItemRemoveListener {
        void onSavingItemRemove(SavingsItem item, int position);
    }

    public SavingsAdapter(Context context, OnSavingsItemClickListener listener) {
        this.context = context;
        this.savingsList = new ArrayList<>();
        this.listener = listener;
    }

    public SavingsAdapter(Context context, OnSavingsItemClickListener listener, OnSavingItemRemoveListener removeListener) {
        this.context = context;
        this.savingsList = new ArrayList<>();
        this.listener = listener;
        this.removeListener = removeListener;
    }

    public void setOnSavingItemRemoveListener(OnSavingItemRemoveListener removeListener) {
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public SavingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_savings, parent, false);
        return new SavingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavingsViewHolder holder, int position) {
        SavingsItem item = savingsList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return savingsList.size();
    }

    public void setSavingsList(List<SavingsItem> savingsList) {
        this.savingsList = savingsList;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < savingsList.size()) {
            savingsList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, savingsList.size());
        }
    }

    class SavingsViewHolder extends RecyclerView.ViewHolder {
        private ImageView savingImage;
        private TextView savingTitle;
        private TextView savingTargetInfo;
        private TextView currentAmount;
        private ImageButton btnRemove;

        public SavingsViewHolder(@NonNull View itemView) {
            super(itemView);
            savingImage = itemView.findViewById(R.id.savingImage);
            savingTitle = itemView.findViewById(R.id.savingTitle);
            savingTargetInfo = itemView.findViewById(R.id.savingTargetInfo);
            currentAmount = itemView.findViewById(R.id.currentAmount);
            btnRemove = itemView.findViewById(R.id.btnRemove);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSavingsItemClick(savingsList.get(position));
                }
            });

            btnRemove.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && removeListener != null) {
                    removeListener.onSavingItemRemove(savingsList.get(position), position);
                }
            });
        }

        public void bind(SavingsItem item) {
            savingTitle.setText(item.getName());

            // Format the date
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(item.getCompletionDate()));
            savingTargetInfo.setText("Target Goal " + formattedDate);

            // Format the amount
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            formatter.setMaximumFractionDigits(0);
            String formattedAmount = formatter.format(item.getTargetAmount());
            currentAmount.setText(formattedAmount);

            // Load image if available
            if (item.getPhotoUri() != null && !item.getPhotoUri().isEmpty()) {
                try {
                    Uri photoUri = Uri.parse(item.getPhotoUri());
                    Glide.with(context)
                            .load(photoUri)
                            .placeholder(R.drawable.placeholder_green)
                            .error(R.drawable.placeholder_green)
                            .centerCrop()
                            .into(savingImage);
                } catch (Exception e) {
                    savingImage.setImageResource(R.drawable.placeholder_green);
                }
            } else {
                savingImage.setImageResource(R.drawable.placeholder_green);
            }
        }
    }
}

