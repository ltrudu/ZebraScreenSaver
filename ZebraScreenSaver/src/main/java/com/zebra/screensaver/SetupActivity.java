package com.zebra.screensaver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SetupActivity extends AppCompatActivity {
    private Switch mStartStopServiceSwitch = null;
    private Switch mAutoStartServiceOnBootSwitch = null;
    private Switch mAutoStartServiceOnPowerEventSwitch = null;

    private TextView mCurrentTimeOut = null;
    private SeekBar mTimeoutSeekBar = null;

    protected static SetupActivity mSetupActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSetupActivity = this;

        setContentView(R.layout.activity_setup);

        mAutoStartServiceOnBootSwitch = (Switch) findViewById(R.id.swStartOnBootSwitch);
        mAutoStartServiceOnPowerEventSwitch = (Switch) findViewById(R.id.swStartOnCraddle);
        mStartStopServiceSwitch = (Switch) findViewById(R.id.swStartStopServiceSwitch);

        mStartStopServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStarted));
                    if(!ScreenSaverService.isRunning(SetupActivity.this)) {
                        ScreenSaverService.startService(SetupActivity.this);
                        ScreenSaverService.startScreenSaver(SetupActivity.this);
                    }
                }
                else
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStopped));
                    if(ScreenSaverService.isRunning(SetupActivity.this))
                        ScreenSaverService.stopService(SetupActivity.this);
                }
            }
        });

        mAutoStartServiceOnBootSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mAutoStartServiceOnBootSwitch.setText(getString(R.string.startOnBoot));
                }
                else
                {
                    mAutoStartServiceOnBootSwitch.setText(getString(R.string.doNothingOnBoot));
                }
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, isChecked);
                editor.commit();
            }
        });

        mAutoStartServiceOnPowerEventSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mAutoStartServiceOnPowerEventSwitch.setText(getString(R.string.startOnCharging));
                    // Launch the watcher service
                    if(!PowerEventsWatcherService.isRunning(SetupActivity.this))
                        PowerEventsWatcherService.startService(SetupActivity.this);
                    // Let's check if we are already connected on power to launch ScreenSaverService if necessary
                    BatteryManager myBatteryManager = (BatteryManager) SetupActivity.this.getSystemService(Context.BATTERY_SERVICE);
                    if(myBatteryManager.isCharging() && !ScreenSaverService.isRunning(SetupActivity.this))
                        ScreenSaverService.startService(SetupActivity.this);
                }
                else
                {
                    mAutoStartServiceOnPowerEventSwitch.setText(getString(R.string.doNothingOnCharging));
                    // Stop the watcher service
                    if(PowerEventsWatcherService.isRunning(SetupActivity.this))
                        PowerEventsWatcherService.stopService(SetupActivity.this);
                }
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, isChecked);
                editor.commit();
            }
        });


        mCurrentTimeOut = (TextView) findViewById(R.id.txtCurrentTimeOut);
        mTimeoutSeekBar = (SeekBar) findViewById(R.id.sbTimeout);
        mTimeoutSeekBar.setMax(Constants.SHARED_PREFERENCES_MAX_TIMEOUT_VALUE);
        mTimeoutSeekBar.setMin(Constants.SHARED_PREFERENCES_MIN_TIMEOUT_VALUE);

        mTimeoutSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int progressNormalized = normalizeTimeOutValue(progress);
                seekBar.setProgress(progressNormalized);
                SetupActivity.this.mCurrentTimeOut.setText(String.valueOf(progressNormalized / Constants.A_SECOND_IN_MILLI_SECONDS) + "s" );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progressNormalized = normalizeTimeOutValue(seekBar.getProgress());
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putInt(Constants.SHARED_PREFERENCES_TIMEOUT, progressNormalized);
                editor.commit();
                if(ScreenSaverService.isRunning(SetupActivity.this)) {
                    ScreenSaverService.setTimeOut(SetupActivity.this, progressNormalized);
                }
                mCurrentTimeOut.setText(String.valueOf(progressNormalized / Constants.A_SECOND_IN_MILLI_SECONDS) + "s");
            }
        });
        updateGuiInternal();
    }

    private void setAutoStartServiceOnBootSwitch(final boolean checked)
    {
        mAutoStartServiceOnBootSwitch.setChecked(checked);
        mAutoStartServiceOnBootSwitch.setText(checked ? R.string.startOnBoot : R.string.doNothingOnBoot);
    }
    private void setAutoStartServiceOnPowerEventSwitch(final boolean checked)
    {
        mAutoStartServiceOnPowerEventSwitch.setChecked(checked);
        mAutoStartServiceOnPowerEventSwitch.setText(checked ? R.string.startOnCharging : R.string.doNothingOnCharging);
    }

    private void setServiceStartedSwitchValues(final boolean checked, final String text)
    {
        mStartStopServiceSwitch.setChecked(checked);
        mStartStopServiceSwitch.setText(text);
    }


    private void syncGUIwithServiceStatus() {
        boolean bServiceIsRunning = ScreenSaverService.isRunning(SetupActivity.this);
        if(bServiceIsRunning)
        {
            setServiceStartedSwitchValues(true, getString(R.string.serviceStarted));
        }
        else
        {
            setServiceStartedSwitchValues(false, getString(R.string.serviceStopped));
        }
    }

    public static void updateGUI()
    {
        // Update GUI if necessary
        if(SetupActivity.mSetupActivity != null) // The application default activity has been opened
        {
            SetupActivity.mSetupActivity.updateGuiInternal();
        }
    }

    private void updateGuiInternal()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                syncGUIwithServiceStatus();
                SharedPreferences sharedpreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean startServiceOnBoot = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
                setAutoStartServiceOnBootSwitch(startServiceOnBoot);
                boolean startServicePowerEvents = sharedpreferences.getBoolean(Constants.SHARED_PREFERENCES_START_SERVICE_ON_CHARGING, false);
                setAutoStartServiceOnPowerEventSwitch(startServicePowerEvents);
                int timeOut = sharedpreferences.getInt(Constants.SHARED_PREFERENCES_TIMEOUT, Constants.SHARED_PREFERENCES_DEFAULT_TIMEOUT_VALUE);
                mTimeoutSeekBar.setProgress(normalizeTimeOutValue(timeOut));
                mCurrentTimeOut.setText(String.valueOf(timeOut / Constants.A_SECOND_IN_MILLI_SECONDS) + "s");
            }
        });

    }

    private int normalizeTimeOutValue(int value)
    {
        if(value == 0)
            return 0;
        return ((int)(value/10.0f))*10;
    }

}
