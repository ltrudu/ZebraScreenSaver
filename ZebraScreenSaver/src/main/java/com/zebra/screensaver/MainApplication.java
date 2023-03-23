package com.zebra.screensaver;

import android.app.Application;
import android.widget.Toast;

import com.zebra.criticalpermissionshelper.CriticalPermissionsHelper;
import com.zebra.criticalpermissionshelper.EPermissionType;
import com.zebra.criticalpermissionshelper.IResultCallbacks;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Re
        RequestOverlayPermission();
    }

    private void RequestOverlayPermission() {
        // We assume the permission has not been granted so we force it.
        // This one is particularly difficult to check on new versions of Android
        // Setting it all the time won't affect performances as it will be executed
        // only when starting the app the first time.
        CriticalPermissionsHelper.grantPermission(MainApplication.this, EPermissionType.SYSTEM_ALERT_WINDOW, new IResultCallbacks() {
            @Override
            public void onSuccess(String message, String resultXML) {
            }

            @Override
            public void onError(String message, String resultXML) {
                Toast.makeText(MainApplication.this, message, Toast.LENGTH_LONG).show();
                System.exit(0);
            }

            @Override
            public void onDebugStatus(String message) {

            }
        });
    }
}
