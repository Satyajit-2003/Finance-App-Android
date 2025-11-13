package com.example.spendtrackr.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendtrackr.R;

import java.util.List;
import java.util.Locale;

public class CategorySummaryAdapter extends RecyclerView.Adapter<CategorySummaryAdapter.ViewHolder> {

    public static class CategorySummaryItem {
        public String categoryName;
        public double amount;
        public int count;

        public CategorySummaryItem(String categoryName, double amount, int count) {
            this.categoryName = categoryName;
            this.amount = amount;
            this.count = count;
        }
    }

    private final List<CategorySummaryItem> items;

    public CategorySummaryAdapter(List<CategorySummaryItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CategorySummaryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == 0) {
            // Static Header Row
            holder.categoryNameText.setText(R.string.stats_header_category);
            holder.amountText.setText(R.string.stats_header_amount);
            holder.countText.setText(R.string.stats_header_count);
            holder.categoryNameText.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.amountText.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.countText.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            CategorySummaryItem item = items.get(position);
            holder.categoryNameText.setText(item.categoryName);
            holder.amountText.setText(String.format(Locale.ENGLISH,"₹%.2f", item.amount));
            holder.countText.setText(String.format(Locale.ENGLISH, "%dx", item.count));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameText, amountText, countText;

        ViewHolder(View itemView) {
            super(itemView);
            categoryNameText = itemView.findViewById(R.id.categoryNameText);
            amountText = itemView.findViewById(R.id.amountText);
            countText = itemView.findViewById(R.id.countText);
        }
    }
}
