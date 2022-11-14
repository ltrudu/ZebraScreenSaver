package com.zebra.screensaver;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.clangen.gfx.plasma.Plasma;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class ScreenSaverService extends Service {
    private static final int SERVICE_ID = 122315;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private KeyguardManager mKeyguardManager;
    private PowerManager mPowerManager;

    private PowerManager.WakeLock mWakeLock = null;

    private static View mView = null;
    private static WindowManager mWindowManager = null;
    private static Handler mMainThreadHandler = null;

    private static Plasma mPlasma;
    private static SurfaceView mSurfaceView = null;

    private static final int TIMER_DURATION_DEFAULT_VALUE = 20000;
    private static int mTimerDuration = 20000;
    private static final int mTimerInterval = 1000;

    private static CountDownTimer mCountdownTimer = null;

    private static TextView tvTime = null;
    private static TextView tvBatteryRemaining = null;
    private static TextView tvBatteryHealth = null;
    private static TextView tvWifiLevel = null;

    private static float mInfoAlpha = 0.0f;
    private static float mAlphaSpeed = 0.1f;

    private static UpdateScreenRunnable mUpdateScreenRunnable = null;
    private static Thread mUpdateScreenThread = null;

    private static String BATTERY_STATE_CHANGED_INTENT = "android.intent.action.BATTERY_CHANGED";
    private static BatteryIntentReceiver mIntentReceiver;
    private static IntentFilter mIntentFilter;

    private static String mBatteryRemaining = null;
    private static String mBatteryHealth = null;

    private static String mWifiLevel = null;

    private static WifiManager mWifimanager = null;

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logD("onStartCommand");
        super.onStartCommand(intent, flags, startId);
        startService();
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        logD("onDestroy");
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
        }
        cleanupWindow(this);
        stopService();
    }

    @SuppressLint({"Wakelock", "MissingPermission"})
    private void startService()
    {
        logD("startService");
        try
        {
            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    0,
                    mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Create the Foreground Service
            String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(mNotificationManager) : "";

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
            mNotification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.no_sleep_service_notification_title))
                    .setContentText(getString(R.string.no_sleep_service_notification_text))
                    .setTicker(getString(R.string.no_sleep_service_notification_tickle))
                    .setPriority(PRIORITY_MIN)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .build();

            TaskStackBuilder localTaskStackBuilder = TaskStackBuilder.create(this);
            localTaskStackBuilder.addParentStack(MainActivity.class);
            localTaskStackBuilder.addNextIntent(mainActivityIntent);
            notificationBuilder.setContentIntent(localTaskStackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT));

            try{
                startForeground(SERVICE_ID, mNotification);

            }
            catch(Exception e)
            {
                Log.d("toto", e.getMessage());
            }
            // Start foreground service

            // Release current wakelock if any
            if ((this.mWakeLock != null) && (this.mWakeLock.isHeld())) {
                this.mWakeLock.release();
            }

            // Acquire wakelock for service
            this.mWakeLock = this.mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP , "zebra:ScreenSaverService");
            this.mWakeLock.setReferenceCounted(false);
            this.mWakeLock.acquire();

            // Disable keyguard
            this.mKeyguardManager.newKeyguardLock("zebra:ScreenSaverService").disableKeyguard();

            if(mMainThreadHandler == null)
                mMainThreadHandler = new Handler(Looper.getMainLooper());

            //createOverlayWindowToForceScreenOn(this);
            startCountdownTimer(this);
            logD("startService:Service started without error.");

            //mNotificationManager.notify(SERVICE_ID, mNotification );
        }
        catch(Exception e)
        {
            logD("startService:Error while starting service.");
            e.printStackTrace();
        }


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
    private String createNotificationChannel(NotificationManager notificationManager){
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            // Use start foreground service to prevent the runtime error:
            // "not allowed to start service intent app is in background"
            // to happen when running on OS >= Oreo
            context.startForegroundService(myIntent);
        }
        else
        {
            context.startService(myIntent);
        }
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

    private static SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        public void surfaceDestroyed(SurfaceHolder holder) {
            mPlasma.stop(holder);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // surfaceChanged() always called at least once after created(), start it there.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface().isValid()) {
                mPlasma.start(holder);
            }
        }
    };

    private static boolean createScreenSaverOverlayWindow(Context context) {
        try
        {
            // We save the current state of mView
            // If a view is already existing we wants to remove it correctly.
            View saveView = mView;

            // Retrieve the window service
            if(mWindowManager == null)
                mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            // Create a new View for our layout
            // mView = new View(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = inflater.inflate(R.layout.screen_saver, null);

            mView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);

            mPlasma = Plasma.getInstance(context);

            mSurfaceView = (SurfaceView) mView.findViewById(R.id.SurfaceView);
            mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);

            tvTime = mView.findViewById(R.id.tv_time);
            tvBatteryHealth = mView.findViewById(R.id.tv_bhealth);
            tvBatteryRemaining = mView.findViewById(R.id.tv_blevel);
            tvWifiLevel = mView.findViewById(R.id.tv_wifilevel);

            updateScreenValues();

            // We create a new layout with the following parameters
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

            // The type toast will be accepted by the system without specific permissions
            int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            layoutParams.type = windowType;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;

            // screen saver
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            //layoutParams.flags |= WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
            layoutParams.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

            mWindowManager.addView(mView, layoutParams);
            mView.setVisibility(View.VISIBLE);

            if(saveView != null)
            {
                saveView.setVisibility(View.GONE);
                mWindowManager.removeView(saveView);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void updateScreenValues()
    {
        RunInUIThread(new Runnable() {
            @Override
            public void run() {
                // Update Time
                Calendar c = Calendar.getInstance();
                //System.out.println("Current time => "+c.getTime());

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = df.format(c.getTime());

                // Now we display formattedDate value in TextView
                tvTime.setText("Current Date and Time :\n"+formattedDate);

                // Update Battery Values
                tvBatteryRemaining.setText("Battery Level:\n" + mBatteryRemaining + "%");
                tvBatteryHealth.setText("Battery Health:\n" + mBatteryHealth + "%");

                // Update Wifi level
                updateWifiInfo();
                tvWifiLevel.setText("Wifi Level:\n" + mWifiLevel + "%");

                mInfoAlpha += mAlphaSpeed;
                if(mInfoAlpha >= 1.0f)
                {
                    mInfoAlpha = 1.0f;
                    mAlphaSpeed *= -1.0f;
                }
                else if(mInfoAlpha <= 0.0f)
                {
                    mInfoAlpha = 0.0f;
                    mAlphaSpeed *= -1.0f;
                }
                tvTime.setAlpha(mInfoAlpha);
                tvBatteryRemaining.setAlpha(mInfoAlpha);
                tvBatteryHealth.setAlpha(mInfoAlpha);
                tvWifiLevel.setAlpha(mInfoAlpha);
            }
        });
    }

    private static void cleanupWindow(Context context) {
        if(mView != null)
        {
            mView.setVisibility(View.GONE);
            mWindowManager.removeView(mView);
            mView = null;
        }
        if(mWindowManager != null)
        {
            mWindowManager = null;
        }
    }

    private static void RunInUIThread(Runnable runnable)
    {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

    public static boolean isScreenSaverActive()
    {
        return mView != null;
    }

    public static void startScreenSaver(final Context context) {
        // Stop timer (we don't need it, now we'll wait for the next user touch event)
        stopCountdownTimer(context);
        // Register battery receiver
        registerBatteryReceiver(context);
        // Create the screen saver window
        createScreenSaverOverlayWindow(context);
        // Initialize wifi manager
        mWifimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // Start the screen saver update thread
        startUpdateThread();
        // This should be set only when screen saver is active
        mView.setOnTouchListener(new View.OnTouchListener() {
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
        mWifimanager = null;
        unregisterBatteryReceiver(context);
        stopUpdateThread();
        cleanupWindow(context);
    }

    private static void startCountdownTimer(final Context context)
    {
        logD("Start Countdown Timer");
        SharedPreferences sharedpreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mTimerDuration = sharedpreferences.getInt(Constants.SHARED_PREFERENCES_TIMEOUT, TIMER_DURATION_DEFAULT_VALUE);

        if(mCountdownTimer == null) {
            mCountdownTimer = new CountDownTimer(mTimerDuration, mTimerInterval) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    logD("Screen Saver Timer Finished");
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



    /**
     * Screen saver methods & classes
     */

    static class UpdateScreenRunnable implements Runnable{
        // @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    updateScreenValues();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }catch(Exception e){
                }
            }
        }
    }

    public static class BatteryIntentReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (BATTERY_STATE_CHANGED_INTENT.equals(intent.getAction())) {
                Bundle bundle = intent.getExtras();
                updateBatteryInfo(bundle);
            }
        }
    }

    private static void registerBatteryReceiver(Context context)
    {
        if(mIntentReceiver == null) {
            mIntentReceiver = new BatteryIntentReceiver();
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            context.registerReceiver(mIntentReceiver, mIntentFilter);
        }
    }

    private static void unregisterBatteryReceiver(Context context)
    {
        if(mIntentReceiver != null) {
            context.unregisterReceiver(mIntentReceiver);
            mIntentFilter = null;
            mIntentReceiver = null;
        }
    }


    private static void startUpdateThread()
    {
        if(mUpdateScreenThread != null)
        {
            mUpdateScreenThread.start();
        }
        else
        {
            mUpdateScreenRunnable = new UpdateScreenRunnable();
            mUpdateScreenThread = new Thread(mUpdateScreenRunnable);
            mUpdateScreenThread.start();
        }
    }

    private static void stopUpdateThread()
    {
        if(mUpdateScreenThread != null && mUpdateScreenThread.isAlive())
        {
            mUpdateScreenThread.interrupt();
            mUpdateScreenThread = null;
            mUpdateScreenRunnable = null;
        }
    }

    private static void updateBatteryInfo(Bundle bundle)
    {
        if(bundle.containsKey("level"))
        {
            mBatteryRemaining = bundle.get("level").toString();
        }
        if(bundle.containsKey("health_percentage"))
        {
            mBatteryHealth = bundle.get("health_percentage").toString();
        }
    }

    private static void updateWifiInfo()
    {
        if(mWifimanager != null) {
            // Level of current connection
            int rssi = mWifimanager.getConnectionInfo().getRssi();
            mWifiLevel = String.valueOf(WifiManager.calculateSignalLevel(rssi, 100) + 1);
        }
    }

}
