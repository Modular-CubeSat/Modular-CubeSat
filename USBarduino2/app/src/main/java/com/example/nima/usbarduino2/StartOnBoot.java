package com.example.nima.usbarduino2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartOnBoot extends BroadcastReceiver {
    //@Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, MainActivity.class);
            serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(serviceIntent);
        }
    }

}
