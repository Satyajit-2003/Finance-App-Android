package com.example.spendtrackr.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.spendtrackr.R;
import com.example.spendtrackr.api.ApiClient;
import com.example.spendtrackr.api.ApiService;
import com.example.spendtrackr.api.StatsResponse;
import com.example.spendtrackr.utils.NotificationHelper;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChartFragment extends Fragment {

    public ChartFragment() {
        // Required empty public constructor
    }

    private StatsResponse.Data cachedStats = null;
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate your fragment layout (we’ll create it next)
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PieChart amountPieChart = view.findViewById(R.id.amountPieChart);
        PieChart countPieChart = view.findViewById(R.id.countPieChart);
        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        TextView summaryTextView = view.findViewById(R.id.summaryTextView);
        TextView totalSpendsTextView = view.findViewById(R.id.totalSpendsText);
        TextView totalTransactionsTextView = view.findViewById(R.id.totalTransactionsText);
        TextView notCategorizedWarningTextView = view.findViewById(R.id.notCategorizedWarningText);
        RecyclerView summaryRecyclerView = view.findViewById(R.id.summaryRecyclerView);

        summaryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        swipeRefreshLayout.setOnRefreshListener(() -> fetchAndDisplayStats(amountPieChart, countPieChart, swipeRefreshLayout, summaryTextView,
                totalSpendsTextView, totalTransactionsTextView, notCategorizedWarningTextView, summaryRecyclerView));

        // Fetch only once on view created
        fetchAndDisplayStats(amountPieChart, countPieChart, swipeRefreshLayout, summaryTextView,
                totalSpendsTextView, totalTransactionsTextView, notCategorizedWarningTextView, summaryRecyclerView);
    }

    private void setupPieChartWithOthers(PieChart pieChart, Map<String, Float> data, float total, String centerText) {
        List<PieEntry> entries = new ArrayList<>();
        float othersTotal = 0f;

        for (Map.Entry<String, Float> entry : data.entrySet()) {
            float percentage = (entry.getValue() / total) * 100f;
            if (percentage < 5f) {
                othersTotal += entry.getValue();
            } else {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        if (othersTotal > 0) {
            entries.add(new PieEntry(othersTotal, "Others"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(2f);

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.setCenterText(centerText);
        pieChart.setCenterTextSize(16f);
        pieChart.getDescription().setEnabled(false);
        pieChart.setEntryLabelTextSize(10f);
        pieChart.animateY(1000);
        pieChart.invalidate();

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextColor(Color.WHITE);
        legend.setTextSize(12f);
        legend.setWordWrapEnabled(true);
    }


    private void fetchAndDisplayStats(PieChart amountPieChart, PieChart countPieChart, SwipeRefreshLayout swipeRefreshLayout, TextView summaryTextView,
                                      TextView totalSpendsTextView, TextView totalTransactionsTextView, TextView notCategorizedWarningTextView, RecyclerView summaryRecyclerView) {
        long now = System.currentTimeMillis();
        if (cachedStats != null && (now - lastFetchTime) < CACHE_DURATION_MS) {
            // If cache is valid, display cached data and return
            swipeRefreshLayout.setRefreshing(false);
            Log.i("fetchAndDisplayStats", "Using Cache");
            displayStats(amountPieChart, countPieChart, summaryTextView, totalSpendsTextView, totalTransactionsTextView,
                    notCategorizedWarningTextView,summaryRecyclerView, cachedStats);
            return;
        }
        Log.i("fetchAndDisplayStats", "Making Fresh API calls");

        swipeRefreshLayout.setRefreshing(true);

        ApiService apiService = ApiClient.getApiService(requireContext());
        apiService.getStats().enqueue(new Callback<StatsResponse>() {
            @Override
            public void onResponse(@NonNull Call<StatsResponse> call, @NonNull Response<StatsResponse> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    cachedStats = response.body().data;
                    lastFetchTime = System.currentTimeMillis();
                    displayStats(amountPieChart, countPieChart, summaryTextView, totalSpendsTextView, totalTransactionsTextView,
                            notCategorizedWarningTextView, summaryRecyclerView, cachedStats);
                } else {
                    NotificationHelper.showErrorNotification(requireContext(), "API Error getStats", "Code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<StatsResponse> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                NotificationHelper.showErrorNotification(requireContext(), "API Failure getStats", t.getMessage());
            }
        });
    }

    private void displayStats(PieChart amountPieChart, PieChart countPieChart, TextView summaryTextView, TextView totalSpendsTextView,
                              TextView totalTransactionsTextView, TextView notCategorizedWarningTextView, RecyclerView summaryRecyclerView, StatsResponse.Data stats) {
        Map<String, Float> amountData = new HashMap<>();
        Map<String, Float> countData = new HashMap<>();
        List<CategorySummaryAdapter.CategorySummaryItem> summaryItems = new ArrayList<>();
        int uncategorizedCount = 0;

        for (Map.Entry<String, StatsResponse.CategoryInfo> entry : stats.categories.entrySet()) {
            String category = entry.getKey();
            if ("Select".equalsIgnoreCase(category)) {
                uncategorizedCount = entry.getValue().count;
                continue;
            }

            amountData.put(category, (float) entry.getValue().amount);
            countData.put(category, (float) entry.getValue().count);

            summaryItems.add(new CategorySummaryAdapter.CategorySummaryItem(category, entry.getValue().amount, entry.getValue().count));
        }

        summaryTextView.setText(getString(R.string.summary_header, stats.monthYear));

        setupPieChartWithOthers(amountPieChart, amountData, (float) stats.totalSpend, "Amount by Category");
        setupPieChartWithOthers(countPieChart, countData, stats.transactionCount,"Count by Category");

        totalSpendsTextView.setText(getString(R.string.total_spends_text, stats.totalSpend));
        totalTransactionsTextView.setText(getString(R.string.total_transactions_text, stats.transactionCount));

        if (uncategorizedCount > 0) {
            Log.d("uncategorized count", String.valueOf(uncategorizedCount));
            notCategorizedWarningTextView.setText(getString(R.string.not_categorizedWarning_text, uncategorizedCount));
        }

        summaryRecyclerView.setAdapter(new CategorySummaryAdapter(summaryItems));
    }



}
