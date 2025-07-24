package com.example.spendtrackr;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class SpendTrackr extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
