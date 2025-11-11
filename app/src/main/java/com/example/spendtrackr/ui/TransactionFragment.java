package com.example.spendtrackr.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.spendtrackr.R;
import com.example.spendtrackr.api.*;
import com.example.spendtrackr.utils.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionFragment extends Fragment implements TransactionAdapter.OnTransactionEditedListener, AddTransactionDialog.OnTransactionAddedListener {

    private static class TransactionCacheEntry {
        List<TransactionItem> transactions;
        long fetchTime;

        TransactionCacheEntry(List<TransactionItem> transactions, long fetchTime) {
            this.transactions = transactions;
            this.fetchTime = fetchTime;
        }
    }

    private static final String TAG = "TransactionFragment";

    private final Map<String, TransactionCacheEntry> transactionCacheMap = new HashMap<>();
    private static final long CACHE_DURATION_MS = 60 * 1000; // 1 min

    private TextView dateText, totalAmountText, totalByMeText, totalByFriendText, noTransactionsWarningText;
    private FloatingActionButton floatingAddButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TransactionAdapter adapter;
    private String currentDate;
    private final List<TransactionItem> masterTransactions = new ArrayList<>();

    public TransactionFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.transactionsRecyclerView);
        dateText = view.findViewById(R.id.dateTextView);
        totalAmountText = view.findViewById(R.id.totalAmountTextView);
        totalByMeText = view.findViewById(R.id.totalByMeTextView);
        totalByFriendText = view.findViewById(R.id.totalByFriendTextView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        noTransactionsWarningText = view.findViewById(R.id.noTransactionsWarningText);
        floatingAddButton = view.findViewById(R.id.fabAddTransaction);

        // initialize currentDate before adapter
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        adapter = new TransactionAdapter(masterTransactions, getSheetName(currentDate), getChildFragmentManager(), this);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        dateText.setText(currentDate);
        dateText.setOnClickListener(v -> showDatePicker());
        swipeRefreshLayout.setOnRefreshListener(() -> fetchTransactions(currentDate));

        floatingAddButton.setOnClickListener(v -> {
            AddTransactionDialog dialog = AddTransactionDialog.newInstance(currentDate);
            dialog.setOnTransactionAddedListener(this);
            dialog.show(getChildFragmentManager(), "AddTransactionDialog");
        });

        fetchTransactions(currentDate);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        try {
            Date selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate);
            if (selectedDate != null) {
                calendar.setTime(selectedDate);
            }
        } catch (Exception ignored) {

        }
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            calendar.set(year, month, day);
            currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            dateText.setText(currentDate);

            adapter = new TransactionAdapter(masterTransactions, getSheetName(currentDate), getChildFragmentManager(), this);
            RecyclerView recyclerView = requireView().findViewById(R.id.transactionsRecyclerView);
            recyclerView.setAdapter(adapter);

            fetchTransactions(currentDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }


    private void fetchTransactions(String date) {
        currentDate = date;
        TransactionCacheEntry cacheEntry = transactionCacheMap.get(date);
        long now = System.currentTimeMillis();

        if (cacheEntry != null && (now - cacheEntry.fetchTime) < CACHE_DURATION_MS) {
            swipeRefreshLayout.setRefreshing(false);
            Log.i(TAG, "Using cache for " + date);
            masterTransactions.clear();
            masterTransactions.addAll(cacheEntry.transactions);
            adapter.notifyDataSetChanged();
            updateSummary(cacheEntry.transactions);
            return;
        }

        Log.i(TAG, "Making Fresh API Calls for " + date);

        swipeRefreshLayout.setRefreshing(true);
        ApiService apiService = ApiClient.getApiService(requireContext());
        apiService.getTransactionsByDate(date).enqueue(new Callback<BaseResponse<TransactionResponse>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<TransactionResponse>> call, @NonNull Response<BaseResponse<TransactionResponse>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    TransactionResponse data = response.body().data;
                    masterTransactions.clear();
                    masterTransactions.addAll(data.transactions);
                    adapter.notifyDataSetChanged();
                    updateSummary(data.transactions);
                    transactionCacheMap.put(date, new TransactionCacheEntry(new ArrayList<>(data.transactions), now));
                } else {
                    String apiMessage = response.body() != null ? response.body().message : response.message();
                    NotificationHelper.showErrorNotification(requireContext(), "getTransactionsByDate Code: " + response.code(), apiMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<TransactionResponse>> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                NotificationHelper.showErrorNotification(requireContext(), "API Failure getTransactionsByDate", t.getMessage());
            }
        });
    }

    private void updateSummary(List<TransactionItem> items) {
        double totalAmount = 0, totalFriend = 0, totalByMe = 0;
        for (TransactionItem item : items) {
            try {
                double amount = Double.parseDouble(item.amount);
                double friendSplit = Double.parseDouble(item.friendSplit);
                totalAmount += amount;
                totalFriend += friendSplit;
                totalByMe += (amount - friendSplit);
            } catch (NumberFormatException ignored) {}
        }

        if (items.isEmpty()) {
            noTransactionsWarningText.setText(getString(R.string.no_transaction_or_sheet_Warning, currentDate));
        } else {
            noTransactionsWarningText.setText("");
        }

        totalAmountText.setText(getString(R.string.transaction_total_amount, totalAmount));
        totalByMeText.setText(getString(R.string.transaction_total_by_me, totalByMe));
        totalByFriendText.setText(getString(R.string.transaction_total_by_friend, totalFriend));
    }

    // Callback from edit dialog when transaction is updated
    @Override
    public void onTransactionChanged() {
        updateSummary(masterTransactions);
    }

    // Callback from add dialog when transaction is added
    @Override
    public void onTransactionAdded(TransactionItem newItem) {
        masterTransactions.add(masterTransactions.size(), newItem);
        adapter.notifyItemInserted(masterTransactions.size() - 1);
        updateSummary(masterTransactions);
    }


    private String getSheetName(String dateStr) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(dateStr);
            return new SimpleDateFormat("MMMM-yyyy", Locale.ENGLISH).format(date);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
