package com.example.spendtrackr.ui;

import android.util.Log;
import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.spendtrackr.R;
import com.example.spendtrackr.api.TransactionItem;
import com.example.spendtrackr.utils.CategoryManager;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder>
        implements EditTransactionDialog.OnTransactionUpdatedListener {

    private final List<TransactionItem> transactions;
    private final String sheetName;
    private final FragmentManager fragmentManager;
    private final OnTransactionEditedListener listener;
    private final String TAG = "TransactionAdopter";

    public interface OnTransactionEditedListener {
        void onTransactionUpdated(List<TransactionItem> transactions, int i);

        void onTransactionDeleted(List<TransactionItem> transactions, int i);
    }

    public TransactionAdapter(List<TransactionItem> transactionList, String sheetName,
                              FragmentManager fragmentManager, OnTransactionEditedListener listener) {
        this.transactions = transactionList;
        this.sheetName = sheetName;
        this.fragmentManager = fragmentManager;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionAdapter.ViewHolder holder, int position) {
        TransactionItem item = transactions.get(position);
        int colorRes = CategoryManager.getColorResForCategory(item.type);
        int color = holder.itemView.getContext().getResources().getColor(colorRes, null);
        holder.cardView.setCardBackgroundColor(color);

        holder.category.setText(item.type);
        holder.amount.setText(String.format(Locale.ENGLISH, "₹%s", item.amount));
        holder.friendSplit.setText(String.format(Locale.ENGLISH, "Friend: ₹%s", item.friendSplit));
        holder.amountBorne.setText(String.format(Locale.ENGLISH, "Me: ₹%s", item.amountBorne));
        holder.notes.setText(item.notes);
        holder.account.setText((item.account));

        holder.editButton.setOnClickListener(v -> {
            EditTransactionDialog dialog = EditTransactionDialog.newInstance(item, sheetName);
            dialog.setOnTransactionUpdatedListener(this);
            dialog.show(fragmentManager, "EditTransaction");
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    @Override
    public void onTransactionUpdated(TransactionItem updatedItem) {
        int idx = -1;
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).rowIndex == updatedItem.rowIndex) {
                transactions.set(i, updatedItem);
                idx = i;
                break;
            }
        }

        if (idx != -1) {
            listener.onTransactionUpdated(transactions, idx);
        } else {
            Log.e(TAG, String.format("Given rowIndex %d, not found for updation.", updatedItem.rowIndex));
        }
    }

    @Override
    public void onTransactionDeleted(int rowIndex) {
        int idx = -1;
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).rowIndex == rowIndex) {
                transactions.remove(i);
                notifyItemRemoved(i);

                // Decrement rowIndex for items after the deleted one
                for (int j = i; j < transactions.size(); j++) {
                    transactions.get(j).rowIndex -= 1;
                }
                idx = i;
                break;
            }
        }
        if (idx != -1) {
            listener.onTransactionDeleted(transactions, idx);
        } else {
            Log.e(TAG, String.format("Given rowIndex %d, not found for deletion", rowIndex));
        }
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView category, amount, friendSplit, amountBorne, notes, account;
        CardView cardView;
        ImageButton editButton;

        ViewHolder(View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.transactionCategory);
            amount = itemView.findViewById(R.id.transactionAmount);
            friendSplit = itemView.findViewById(R.id.transactionFriendSplit);
            amountBorne = itemView.findViewById(R.id.transactionAmountBorne);
            notes = itemView.findViewById(R.id.transactionNotes);
            account = itemView.findViewById(R.id.transactionAccount);
            cardView = itemView.findViewById(R.id.cardView);
            editButton = itemView.findViewById(R.id.editButton);
        }
    }
}
