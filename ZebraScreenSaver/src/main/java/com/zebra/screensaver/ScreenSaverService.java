package com.zebra.screensaver;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.app.TaskStackBuilder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;
import static com.zebra.screensaver.Constants.DISMISS_NOTIFICATION_ACTION;

public class ScreenSaverService extends Service {
    private static final int SERVICE_ID = 122315;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;

    private PowerManager.WakeLock mWakeLock = null;

    private static int mTimerDuration = Constants.SHARED_PREFERENCES_DEFAULT_TIMEOUT_VALUE;
    private static final int mTimerInterval = 1000;

    private static CountDownTimer mCountdownTimer = null;

    public ScreenSaverService() {
    }

    public IBinder onBind(Intent paramIntent)
    {
        return null;
    }

    public void onCreate()
    {
        logD("onCreate");
        this.mPowerManager = ((PowerManager)getSystemService(Context.POWER_SERVICE));
        this.mKeyguardManager = ((KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE));
        this.mNotificationManager = ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE));
        SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mTimerDuration = sharedpreferences.getInt(Constants.SHARED_PREFERENCES_TIMEOUT, Constants.SHARED_PREFERENCES_DEFAULT_TIMEOUT_VALUE);
        mTimerDuration  = Math.max(mTimerDuration, Constants.SHARED_PREFERENCES_MIN_TIMEOUT_VALUE);
        mTimerDuration = Math.min(mTimerDuration, Constants.SHARED_PREFERENCES_MAX_TIMEOUT_VALUE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logD("onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if(intent != null && intent.getAction() == DISMISS_NOTIFICATION_ACTION)
        {
            createServiceNotificationChannel();
            StartForegroundService();
        }
        else {
            startService();
        }
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        logD("onDestroy");
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
        }
        ScreenSaverOverlay.cleanupWindow(this);
        stopService();
    }

    @SuppressLint({"Wakelock", "MissingPermission"})
    private void startService()
    {
        logD("startService");
        try
        {
            createServiceNotificationChannel();

            // Start foreground service
            StartForegroundService();

            StartServiceCustomCode();

            //mNotificationManager.notify(SERVICE_ID, mNotification );
        }
        catch(Exception e)
        {
            logD("startService:Error while starting service.");
            e.printStackTrace();
        }


    }

    private void StartServiceCustomCode() {
        // Release current wakelock if any
        if ((this.mWakeLock != null) && (this.mWakeLock.isHeld())) {
            this.mWakeLock.release();
        }

        // Acquire wakelock for service
        this.mWakeLock = this.mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP , "zebra:ScreenSaverService");
        this.mWakeLock.setReferenceCounted(false);
        this.mWakeLock.acquire();

        // Disable keyguard
        this.mKeyguardManager.newKeyguardLock("zebra:ScreenSaverService").disableKeyguard();

        //createOverlayWindowToForceScreenOn(this);
        startCountdownTimer(this);
        logD("startService:Service started without error.");
    }

    private void StartForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            ServiceCompat.startForeground(
                    this,
                    SERVICE_ID,
                    mNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE // Example type
            );
        } else {
            startForeground(SERVICE_ID, mNotification);
        }
    }

    private void createServiceNotificationChannel() {
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                mainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create a dismiss intent
        // Dismiss Intent
        Intent dismissIntent = new Intent(this, ScreenSaverService.class);
        dismissIntent.setAction(DISMISS_NOTIFICATION_ACTION);

        PendingIntent dismissPendingIntent = PendingIntent.getService(
                this,
                0,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create the Foreground Service
        String channelId = createServiceNotificationChannel(mNotificationManager);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        mNotification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.no_sleep_service_notification_title))
                .setContentText(getString(R.string.no_sleep_service_notification_text))
                .setTicker(getString(R.string.no_sleep_service_notification_tickle))
                .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .setOngoing(true)
                .build();

        TaskStackBuilder localTaskStackBuilder = TaskStackBuilder.create(this);
        localTaskStackBuilder.addParentStack(MainActivity.class);
        localTaskStackBuilder.addNextIntent(mainActivityIntent);
        notificationBuilder.setContentIntent(localTaskStackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }

    private void stopService()
    {
        try
        {
            logD("stopService.");
            //if (this.mWakeLock != null) {
            //    this.mWakeLock.release();
            //}
            //cleanupWindow(this);
            stopForeground(true);
            logD("stopService:Service stopped without error.");
        }
        catch(Exception e)
        {
            logD("Error while stopping service.");
            e.printStackTrace();

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createServiceNotificationChannel(NotificationManager notificationManager){
        NotificationChannel channel = new NotificationChannel(getString(R.string.nosleepservice_channel_id), getString(R.string.nosleepservice_channel_name), NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return getString(R.string.nosleepservice_channel_id);
    }

    public static void logD(String message)
    {
        Log.d(Constants.TAG, message);
    }

    public static void startService(Context context)
    {
        Intent myIntent = new Intent(context, ScreenSaverService.class);
        context.startForegroundService(myIntent);
    }

    public static void stopService(Context context)
    {
        Intent myIntent = new Intent(context, ScreenSaverService.class);
        context.stopService(myIntent);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ScreenSaverService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



    public static boolean isScreenSaverActive()
    {
        return ScreenSaverOverlay.isViewActive();
    }

    public static void startScreenSaver(final Context context) {
        // Stop timer (we don't need it, now we'll wait for the next user touch event)
        stopCountdownTimer(context);
        // Create the screen saver window
        ScreenSaverOverlay.createScreenSaverOverlayWindow(context);
        // Start the screen saver update thread
        ScreenSaverOverlay.start();
        // This should be set only when screen saver is active
        ScreenSaverOverlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                stopScreenSaver(context);
                startCountdownTimer(context);
                return true;
            }
        });
    }

    private static void stopScreenSaver(Context context)
    {
        ScreenSaverOverlay.stop();
        ScreenSaverOverlay.cleanupWindow(context);
    }

    private static void startCountdownTimer(final Context context)
    {
        logD("Start Countdown Timer");
        SharedPreferences sharedpreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mTimerDuration = sharedpreferences.getInt(Constants.SHARED_PREFERENCES_TIMEOUT, Constants.SHARED_PREFERENCES_DEFAULT_TIMEOUT_VALUE);
        mTimerDuration  = Math.max(mTimerDuration, Constants.SHARED_PREFERENCES_MIN_TIMEOUT_VALUE);
        mTimerDuration = Math.min(mTimerDuration, Constants.SHARED_PREFERENCES_MAX_TIMEOUT_VALUE);
        if(mCountdownTimer == null) {
            mCountdownTimer = new CountDownTimer(mTimerDuration, mTimerInterval) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    logD("Screen Saver Timer Finished");
                    if(isScreenSaverActive() == false)
                        startScreenSaver(context);
                }
            };
        }
        else
        {
            mCountdownTimer.cancel();
        }
        mCountdownTimer.start();
    }

    protected static void resetCountdownTimer(final Context context)
    {
        logD("Reset Countdown Timer");
        if(mCountdownTimer != null)
        {
            mCountdownTimer.cancel();
            mCountdownTimer.start();
        }
        else
        {
            if(isScreenSaverActive() == false && isRunning(context))
                startCountdownTimer(context);
        }
    }

    private static void stopCountdownTimer(final Context context)
    {
        logD("Stop Countdown Timer");
        if(mCountdownTimer != null)
        {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
    }

    protected static void setTimeOut(final Context context, int timeOut)
    {
        mTimerDuration = timeOut;
        resetCountdownTimer(context);
    }
}
