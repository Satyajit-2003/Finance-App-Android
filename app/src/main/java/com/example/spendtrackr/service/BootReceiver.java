package com.example.spendtrackr.service;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, SmsMonitorService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}
