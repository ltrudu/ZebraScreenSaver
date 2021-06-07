package com.zebra.screensaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "StartServiceBroadcastReceiver::onReceive");
        // Start service
        ScreenSaverService.startService(context);
        MainActivity.updateGUISwitchesIfNecessary();
    }
}
