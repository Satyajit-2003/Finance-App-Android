package com.example.spendtrackr;

import android.app.Application;
import com.google.android.material.color.DynamicColors;
import com.jakewharton.threetenabp.AndroidThreeTen;

public class SpendTrackr extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
