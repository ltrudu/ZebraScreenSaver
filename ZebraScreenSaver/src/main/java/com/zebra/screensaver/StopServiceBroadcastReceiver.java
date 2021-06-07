package com.zebra.screensaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StopServiceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "StopServiceBroadcastReceiver::onReceive");
        // Stop service
        ScreenSaverService.stopService(context);
        MainActivity.updateGUISwitchesIfNecessary();
    }
}
