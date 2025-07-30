package com.example.spendtrackr.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.spendtrackr.R;
import com.example.spendtrackr.api.ApiClient;
import com.example.spendtrackr.api.ApiService;
import com.example.spendtrackr.api.BaseResponse;
import com.example.spendtrackr.api.StatsResponse;
import com.example.spendtrackr.utils.CategoryManager;
import com.example.spendtrackr.utils.NotificationHelper;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChartFragment extends Fragment {

    // Inner Private class to store Cache Entries
    private static class StatsCacheEntry {
        StatsResponse statsResponse;
        long fetchTime;

        StatsCacheEntry(StatsResponse statsResponse, long fetchTime) {
            this.statsResponse = statsResponse;
            this.fetchTime = fetchTime;
        }
    }


    public ChartFragment() {
        // Required empty public constructor
    }

    private final Map<String, StatsCacheEntry> statsCacheMap = new HashMap<>();
    private static final long CACHE_DURATION_MS = 60 * 1000; // 1 minute
    private String currentMonthYear = null;
    private final String TAG = "ChartFragment";



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        MaterialCardView monthYearSelectorCard = view.findViewById(R.id.monthYearSelectorCard);

        summaryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));


        SimpleDateFormat sdf = new SimpleDateFormat("MMMM-yyyy", Locale.getDefault());
        currentMonthYear = sdf.format(new Date());

        swipeRefreshLayout.setOnRefreshListener(() -> fetchAndDisplayStats(currentMonthYear, amountPieChart, countPieChart, swipeRefreshLayout, summaryTextView,
                totalSpendsTextView, totalTransactionsTextView, notCategorizedWarningTextView, summaryRecyclerView));

        monthYearSelectorCard.setOnClickListener(v -> showMonthYearPicker(amountPieChart, countPieChart, swipeRefreshLayout, summaryTextView,
                totalSpendsTextView, totalTransactionsTextView, notCategorizedWarningTextView, summaryRecyclerView));

        // Fetch only once on view created
        fetchAndDisplayStats(currentMonthYear, amountPieChart, countPieChart, swipeRefreshLayout, summaryTextView,
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
        legend.setEnabled(false);
    }


    private void fetchAndDisplayStats(String currentMonthYear, PieChart amountPieChart, PieChart countPieChart, SwipeRefreshLayout swipeRefreshLayout, TextView summaryTextView,
                                      TextView totalSpendsTextView, TextView totalTransactionsTextView, TextView notCategorizedWarningTextView, RecyclerView summaryRecyclerView) {
        StatsCacheEntry cacheEntry = statsCacheMap.get(currentMonthYear);
        long now = System.currentTimeMillis();

        if (cacheEntry != null && (now - cacheEntry.fetchTime) < CACHE_DURATION_MS) {
            swipeRefreshLayout.setRefreshing(false);
            Log.i(TAG, "Using Cache for " + currentMonthYear);
            displayStats(amountPieChart, countPieChart, summaryTextView, totalSpendsTextView,
                    totalTransactionsTextView, notCategorizedWarningTextView, summaryRecyclerView, cacheEntry.statsResponse);
            return;
        }

        Log.i(TAG, "Making Fresh API calls");

        swipeRefreshLayout.setRefreshing(true);

        ApiService apiService = ApiClient.getApiService(requireContext());
        apiService.getStats(currentMonthYear).enqueue(new Callback<BaseResponse<StatsResponse>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<StatsResponse>> call, @NonNull Response<BaseResponse<StatsResponse>> response) {
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    StatsResponse freshStatsResponse = response.body().data;
                    // Caching
                    statsCacheMap.put(currentMonthYear, new StatsCacheEntry(freshStatsResponse, now));

                    displayStats(amountPieChart, countPieChart, summaryTextView, totalSpendsTextView, totalTransactionsTextView,
                            notCategorizedWarningTextView, summaryRecyclerView, freshStatsResponse);
                } else {
                    String apiMessage = response.body() != null ? response.body().message : response.message();
                    NotificationHelper.showErrorNotification(requireContext(), "getStats Code: " + response.code(), apiMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<StatsResponse>> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                NotificationHelper.showErrorNotification(requireContext(), "API Failure getStats", t.getMessage());
            }
        });

    }

    private void displayStats(PieChart amountPieChart, PieChart countPieChart, TextView summaryTextView, TextView totalSpendsTextView,
                              TextView totalTransactionsTextView, TextView notCategorizedWarningTextView, RecyclerView summaryRecyclerView, StatsResponse stats) {
        Map<String, Float> amountData = new HashMap<>();
        Map<String, Float> countData = new HashMap<>();
        List<CategorySummaryAdapter.CategorySummaryItem> summaryItems = new ArrayList<>();
        int uncategorizedCount = 0;
        long totalAmountPieChart = (long) 0;
        int totalTransactionsPieChart = 0;


        // Add for header:
        summaryItems.add(new CategorySummaryAdapter.CategorySummaryItem("Header", 0.0, 0));

        for (Map.Entry<String, StatsResponse.CategoryInfo> entry : stats.categories.entrySet()) {
            String category = entry.getKey();
            if ("Select".equalsIgnoreCase(category)) {
                uncategorizedCount = entry.getValue().count;
                continue;
            }

            if (CategoryManager.shouldIncludeInPieChart(category)){
                amountData.put(category, (float) entry.getValue().amount);
                countData.put(category, (float) entry.getValue().count);
                totalAmountPieChart += (long) entry.getValue().amount;
                totalTransactionsPieChart += entry.getValue().count;
            }

            summaryItems.add(new CategorySummaryAdapter.CategorySummaryItem(category, entry.getValue().amount, entry.getValue().count));
        }

        summaryTextView.setText(getString(R.string.summary_header, stats.monthYear));
        currentMonthYear = stats.monthYear;

        setupPieChartWithOthers(amountPieChart, amountData, (float) totalAmountPieChart, "Amount by Category");
        setupPieChartWithOthers(countPieChart, countData, totalTransactionsPieChart,"Count by Category");

        totalSpendsTextView.setText(getString(R.string.total_spends_text, (float) totalAmountPieChart));
        totalTransactionsTextView.setText(getString(R.string.total_transactions_text, totalTransactionsPieChart));

        if (uncategorizedCount > 0) {
            notCategorizedWarningTextView.setText(getString(R.string.not_categorizedWarning_text, uncategorizedCount));
        } else {
            notCategorizedWarningTextView.setText("");
        }

        if (summaryItems.size() > 1){
            summaryRecyclerView.setAdapter(new CategorySummaryAdapter(summaryItems));
        } else {
            List<CategorySummaryAdapter.CategorySummaryItem> itemList = new ArrayList<>();
            summaryRecyclerView.setAdapter(new CategorySummaryAdapter(itemList));
            notCategorizedWarningTextView.setText( notCategorizedWarningTextView.getText() +
                    getString(R.string.no_transaction_or_sheet_Warning, stats.monthYear));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showMonthYearPicker(
            PieChart amountPieChart, PieChart countPieChart, SwipeRefreshLayout swipeRefreshLayout,
            TextView summaryTextView, TextView totalSpendsTextView, TextView totalTransactionsTextView,
            TextView notCategorizedWarningTextView, RecyclerView summaryRecyclerView) {

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.month_year_picker_dialog, null);

        MaterialAutoCompleteTextView monthDropdown = dialogView.findViewById(R.id.spinner_month);
        MaterialAutoCompleteTextView yearDropdown = dialogView.findViewById(R.id.spinner_year);

        // Populate months
        String[] months = new DateFormatSymbols().getMonths();
        List<String> monthList = new ArrayList<>(Arrays.asList(months).subList(0, 12));
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, monthList);
        monthDropdown.setAdapter(monthAdapter);

        // Populate years (current year to last 10 years)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> yearList = new ArrayList<>();
        for (int i = currentYear; i >= currentYear - 10; i--) {
            yearList.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, yearList);
        yearDropdown.setAdapter(yearAdapter);

        // Disable keyboard for month dropdown
        monthDropdown.setInputType(0);
        monthDropdown.setKeyListener(null);
        monthDropdown.setFocusable(false);
        monthDropdown.setOnTouchListener((v, event) -> {
            monthDropdown.showDropDown();
            return false;
        });

        // Disable keyboard for year dropdown
        yearDropdown.setInputType(0);
        yearDropdown.setKeyListener(null);
        yearDropdown.setFocusable(false);
        yearDropdown.setOnTouchListener((v, event) -> {
            yearDropdown.showDropDown();
            return false;
        });


        // Preselect current month and year
        monthDropdown.setText(monthList.get(Calendar.getInstance().get(Calendar.MONTH)), false);
        yearDropdown.setText(String.valueOf(currentYear), false);

        // Show Dialog
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Month & Year")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    String selectedMonthName = monthDropdown.getText().toString();
                    String selectedYearText = yearDropdown.getText().toString();

                    if (selectedMonthName.isEmpty() || selectedYearText.isEmpty()) {
                        Toast.makeText(requireContext(), "Please select both month and year", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedMonth = monthList.indexOf(selectedMonthName);  // 0-based index
                    int selectedYear = Integer.parseInt(selectedYearText);

                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(Calendar.MONTH, selectedMonth);
                    selectedDate.set(Calendar.YEAR, selectedYear);

                    String selectedMonthYear = new SimpleDateFormat("MMMM-yyyy", Locale.getDefault()).format(selectedDate.getTime());

                    fetchAndDisplayStats(selectedMonthYear, amountPieChart, countPieChart, swipeRefreshLayout, summaryTextView,
                            totalSpendsTextView, totalTransactionsTextView, notCategorizedWarningTextView, summaryRecyclerView);

                })
                .setNegativeButton("Cancel", null)
                .show();
    }


}
