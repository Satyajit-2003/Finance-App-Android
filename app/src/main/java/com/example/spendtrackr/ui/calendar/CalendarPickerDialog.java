package com.example.spendtrackr.ui.calendar;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.spendtrackr.R;
import com.example.spendtrackr.api.ApiClient;
import com.example.spendtrackr.api.BaseResponse;
import com.example.spendtrackr.api.UncategorizedTransactionsResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarPickerDialog extends BottomSheetDialogFragment {

    public interface OnDateSelectedListener {
        void onDateSelected(String date); // returns "yyyy-MM-dd"
    }

    private static final String ARG_CURRENT_DATE = "current_date";

    private OnDateSelectedListener listener;
    private MaterialCalendarView calendarView;
    private MaterialButton refreshButton;
    private UncategorizedDatesDecorator uncategorizedDecorator;
    private TodayDecorator todayDecorator;
    private OtherMonthDecorator otherMonthDecorator;

    // Cache: "MMMM-yyyy" key → list of uncategorized date strings ("yyyy-MM-dd")
    private final Map<String, List<String>> uncategorizedCache = new HashMap<>();

    private int currentDisplayYear;
    private int currentDisplayMonth; // 1-based (CalendarDay convention)

    public static CalendarPickerDialog newInstance(String currentDate) {
        CalendarPickerDialog dialog = new CalendarPickerDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_DATE, currentDate);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_calendar_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarView = view.findViewById(R.id.calendarView);
        refreshButton = view.findViewById(R.id.calendarRefreshButton);

        // Determine initial displayed month from current date argument
        CalendarDay todayDay = CalendarDay.today();
        currentDisplayYear = todayDay.getYear();
        currentDisplayMonth = todayDay.getMonth();

        String currentDate = getArguments() != null ? getArguments().getString(ARG_CURRENT_DATE) : null;
        if (currentDate != null) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(currentDate);
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    currentDisplayYear = cal.get(Calendar.YEAR);
                    currentDisplayMonth = cal.get(Calendar.MONTH) + 1; // 1-based
                    CalendarDay day = CalendarDay.from(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH));
                    calendarView.setSelectedDate(day);
                    calendarView.setCurrentDate(day);
                }
            } catch (Exception ignored) {}
        }

        // Decorators
        todayDecorator = new TodayDecorator();
        otherMonthDecorator = new OtherMonthDecorator(currentDisplayYear, currentDisplayMonth);
        uncategorizedDecorator = new UncategorizedDatesDecorator();
        calendarView.addDecorators(todayDecorator, otherMonthDecorator, uncategorizedDecorator);

        calendarView.setOnMonthChangedListener((widget, date) -> {
            currentDisplayYear = date.getYear();
            currentDisplayMonth = date.getMonth();
            otherMonthDecorator.setDisplayedMonth(date.getYear(), date.getMonth());
            // invalidate other-month graying first (no dot data change needed here)
            calendarView.invalidateDecorators();
            fetchUncategorizedDates(currentDisplayYear, currentDisplayMonth, false);
        });

        // Block future dates
        Calendar today = Calendar.getInstance();
        calendarView.state().edit()
                .setMaximumDate(CalendarDay.from(
                        today.get(Calendar.YEAR),
                        today.get(Calendar.MONTH) + 1,
                        today.get(Calendar.DAY_OF_MONTH)))
                .commit();

        calendarView.setOnDateChangedListener((widget, day, selected) -> {
            if (selected && listener != null) {
                String picked = String.format(Locale.ENGLISH, "%04d-%02d-%02d",
                        day.getYear(), day.getMonth(), day.getDay());
                listener.onDateSelected(picked);
                dismiss();
            }
        });

        refreshButton.setOnClickListener(v -> {
            String cacheKey = formatMonthYear(currentDisplayYear, currentDisplayMonth);
            uncategorizedCache.remove(cacheKey);
            fetchUncategorizedDates(currentDisplayYear, currentDisplayMonth, true);
        });

        // Initial load for the displayed month
        fetchUncategorizedDates(currentDisplayYear, currentDisplayMonth, false);
    }

    private String formatMonthYear(int year, int month) {
        // month is 1-based; Calendar.MONTH is 0-based
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        return new SimpleDateFormat("MMMM-yyyy", Locale.ENGLISH).format(cal.getTime());
    }

    private void fetchUncategorizedDates(int year, int month, boolean isRefresh) {
        String cacheKey = formatMonthYear(year, month);

        if (uncategorizedCache.containsKey(cacheKey)) {
            applyUncategorizedDecorator(uncategorizedCache.get(cacheKey));
            return;
        }

        if (isRefresh) {
            refreshButton.setEnabled(false);
            Toast.makeText(requireContext(), "Refreshing…", Toast.LENGTH_SHORT).show();
        }

        Context appContext = requireContext().getApplicationContext();

        ApiClient.getApiService(appContext)
                .getUncategorizedTransactions(cacheKey)
                .enqueue(new Callback<BaseResponse<UncategorizedTransactionsResponse>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<BaseResponse<UncategorizedTransactionsResponse>> call,
                            @NonNull Response<BaseResponse<UncategorizedTransactionsResponse>> response) {
                        if (!isAdded()) return;
                        List<String> dates = new ArrayList<>();
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().success
                                && response.body().data != null) {
                            List<String> raw = response.body().data.getUncategorizedDates();
                            if (raw != null) dates.addAll(raw);
                        }
                        uncategorizedCache.put(cacheKey, dates);
                        applyUncategorizedDecorator(dates);
                        if (isRefresh) refreshButton.setEnabled(true);
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<BaseResponse<UncategorizedTransactionsResponse>> call,
                            @NonNull Throwable t) {
                        if (!isAdded()) return;
                        if (isRefresh) refreshButton.setEnabled(true);
                    }
                });
    }

    private void applyUncategorizedDecorator(List<String> dateStrings) {
        HashSet<CalendarDay> days = new HashSet<>();
        for (String ds : dateStrings) {
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(ds);
                if (d != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(d);
                    days.add(CalendarDay.from(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)));
                }
            } catch (Exception ignored) {}
        }
        // Update the decorator's set, then force a full re-decoration cycle by
        // removing and re-adding all decorators. This is necessary because
        // invalidateDecorators() alone doesn't always re-evaluate shouldDecorate
        // for decorators that were added before data was available.
        uncategorizedDecorator.setDates(days);
        calendarView.removeDecorators();
        calendarView.addDecorators(todayDecorator, otherMonthDecorator, uncategorizedDecorator);
    }
}

