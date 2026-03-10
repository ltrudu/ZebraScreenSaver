package com.zebra.screensaver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class Constants {
    public static final String TAG  ="ScreenSaverService";

    public static final String DISMISS_NOTIFICATION_ACTION = "com.zebra.foregroundservice.DISMISS_NOTIFICATION_ACTION";

    // Shared preference keys
    public static final String SHARED_PREFERENCES_NAME = "ScreenSaverService";
    public static final String SHARED_PREFERENCES_START_SERVICE_ON_BOOT = "startonboot";
    public static final String SHARED_PREFERENCES_START_SERVICE_ON_CHARGING = "startoncharging";
    public static final String SHARED_PREFERENCES_TIMEOUT = "timeout";

    public static final int A_SECOND_IN_MILLI_SECONDS = 1000;
    public static final int SHARED_PREFERENCES_DEFAULT_TIMEOUT_VALUE = 60*A_SECOND_IN_MILLI_SECONDS;
    public static final int SHARED_PREFERENCES_MIN_TIMEOUT_VALUE = 5*A_SECOND_IN_MILLI_SECONDS;
    public static final int SHARED_PREFERENCES_MAX_TIMEOUT_VALUE = 500*A_SECOND_IN_MILLI_SECONDS;

    public static final String EXTRA_CONFIGURATION_START_ON_BOOT = "startonboot";
    public static final String EXTRA_CONFIGURATION_START_ON_CHARGING = "startoncharging";


}
