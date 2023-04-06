package com.zebra.screensaver;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.List;

// The service can be launched using the graphical user interface, intent actions or adb.
//
// If the option "Start on boot" is enabled, the service will be automatically launched when the boot is complete.
//
// Power events occur when the device is connected to a power source (AC/USB/Wireless).
// If the option "Start when charging / Stop when charging" is enabled, the power events will be monitored.
// The ScreenSaverService will be launched when the device is connected to a power source
//
//
// The service respond to two intent actions (both uses the category: android.intent.category.DEFAULT)
// - "com.zebra.screensaver.startservice" sent on the component "com.zebra.screensaver/com.zebra.screensaver.StartServiceBroadcastReceiver":
//   Start the service.
//   If the device get rebooted the service will start automatically once the reboot is completed.
// - "com.zebra.screensaver.stopservice" sent on the component "com.zebra.screensaver/com.zebra.screensaver.StopServiceBroadcastReceiver":
//   Stop the service.
//   If the device is rebooted, the service will not be started.
//
// The service can be started and stopped manually using the following adb commands:
//  - Start service:
//      adb shell am broadcast -a com.zebra.screensaver.startservice -n com.zebra.screensaver/com.zebra.screensaver.StartServiceBroadcastReceiver
//  - Stop service:
//      adb shell am broadcast -a com.zebra.screensaver.stopservice -n com.zebra.screensaver/com.zebra.screensaver.StopServiceBroadcastReceiver
//  - Setup service
//          The service can be configured using the following intent:
//          adb shell am broadcast -a com.zebra.screensaver.setupservice -n com.zebra.screensaver/com.zebra.screensaver.SetupServiceBroadcastReceiver --es startonboot "true" --es startoncharging "true"
//          The command must contain at least one of the extras:
//          - Configure autostart on boot:
//          --es startonboot "true"
//          - Configure autostart on power connection (AC/USB/Wireless)
//          --es startoncharging "true"
//          The extras value can be set to "true" or "1" to enable the option and "false" or "0" to disable the option.
public class MainActivity extends AppCompatActivity {

    public static MainActivity mMainActivity;

    private static final int MANIFEST_PERMISSION = 1;
    private static final String[] MANIFEST_PERMISSIONS_LIST = new String[]{
            Manifest.permission.ACCESS_WIFI_STATE
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((Button)findViewById(R.id.btLicense)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ltrudu/NoSleepService/blob/master/README.md"));
                Intent myIntent = new Intent(MainActivity.this, ZebraLicenceActivity.class);
                startActivity(myIntent);
            }
        });

        launchPowerEventsWatcherServiceIfNecessary();
        checkManifestPermissions();
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainactivitymenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, SetupActivity.class));
                return true;
            case R.id.menu_zebra_licence:
                startActivity(new Intent(this, ZebraLicenceActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        mMainActivity = this;
        super.onResume();
        launchPowerEventsWatcherServiceIfNecessary();
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }

        return false;
    }

    /*
    private void checkAccessibility()
    {
        if(isAccessibilityServiceEnabled(this, AccessibilityEventsService.class) == false)
        {
            startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }
    */


    public static boolean isAccessibilitySettingsOn(Context context, Class<? extends AccessibilityService> serviceClass) {
        int accessibilityEnabled = 0;

        final String service = serviceClass.getName();

        boolean accessibilityFound = false;
        try
        {
            accessibilityEnabled = Settings.Secure.getInt(context.getApplicationContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            ScreenSaverService.logD("accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e)
        {
            ScreenSaverService.logD("Error finding setting, default accessibility to not found: " + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();

                    if (accessabilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return accessibilityFound;
    }

    public void checkManifestPermissions()
    {
        boolean shouldNotRequestPermissions = true;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            for(String permission : MANIFEST_PERMISSIONS_LIST)
            {
                shouldNotRequestPermissions &= (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
            }
        }

        if (shouldNotRequestPermissions) {
            //checkAccessibility();
        }
        else
        {
            ActivityCompat.requestPermissions(this,MANIFEST_PERMISSIONS_LIST, MANIFEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MANIFEST_PERMISSION:
                boolean allPermissionGranted = true;
                for(int grantResult : grantResults)
                {
                    allPermissionGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
                }
                if (allPermissionGranted) {
                    //checkAccessibility();
                } else {
                    ShowAlertDialog(MainActivity.this, "Error", "Please grant the necessary permission to launch the application.");
                }
                return;
        }
    }

    private void ShowAlertDialog(Context context, String title, String message)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set title
        alertDialogBuilder.setTitle(title);

        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        checkManifestPermissions();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void launchPowerEventsWatcherServiceIfNecessary()
    {
        // We need to launch the PowerEventsWatcher Service if necessary
        SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startServiceOnCharging = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, false);
        if(startServiceOnCharging)
        {
            // Launch the service if it was not running
            if(!PowerEventsWatcherService.isRunning(this))
                PowerEventsWatcherService.startService(this);

            // Let's check if we are already connected on power to launch ScreenSaverService if necessary
            BatteryManager myBatteryManager = (BatteryManager) MainActivity.this.getSystemService(Context.BATTERY_SERVICE);
            if(myBatteryManager.isCharging() && !ScreenSaverService.isRunning(MainActivity.this))
                ScreenSaverService.startService(MainActivity.this);
        }
    }

    @Override
    protected void onPause() {
        mMainActivity = null;
        super.onPause();
    }


}
