package com.example.spendtrackr.ui.calendar;

import android.graphics.Color;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.Collection;
import java.util.HashSet;

public class UncategorizedDatesDecorator implements DayViewDecorator {

    private static final float DOT_RADIUS = 5f;
    private static final int DOT_COLOR = Color.parseColor("#FFC107"); // amber yellow

    private final HashSet<CalendarDay> dates = new HashSet<>();

    public void setDates(Collection<CalendarDay> newDates) {
        dates.clear();
        dates.addAll(newDates);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new DotSpan(DOT_RADIUS, DOT_COLOR));
    }
}
