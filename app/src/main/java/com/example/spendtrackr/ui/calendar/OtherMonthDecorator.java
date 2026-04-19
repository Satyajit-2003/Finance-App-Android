package com.example.spendtrackr.ui.calendar;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

/**
 * Grays out dates that belong to a month other than the currently displayed one.
 * Create a new instance (or call {@link #setDisplayedMonth}) whenever the user
 * swipes to a different month and call {@code invalidateDecorators()}.
 */
public class OtherMonthDecorator implements DayViewDecorator {

    private int displayedYear;
    private int displayedMonth; // 1-based (CalendarDay convention)

    public OtherMonthDecorator(int year, int month) {
        this.displayedYear = year;
        this.displayedMonth = month;
    }

    public void setDisplayedMonth(int year, int month) {
        this.displayedYear = year;
        this.displayedMonth = month;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.getYear() != displayedYear || day.getMonth() != displayedMonth;
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new ForegroundColorSpan(Color.parseColor("#888888")));

    }
}
