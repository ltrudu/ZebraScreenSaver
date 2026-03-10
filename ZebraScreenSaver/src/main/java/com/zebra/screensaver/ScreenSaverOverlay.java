package com.zebra.screensaver;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class ScreenSaverOverlay {

    private static View mView = null;
    private static WindowManager mWindowManager = null;
    private static Handler mMainThreadHandler = null;

    private static View vCustomBackgroundView = null;
    private static RelativeLayout rlOverlay = null;
    private static RelativeLayout rlMovingOverlay = null;

    private static float mCustomBackgroundAlpha = 0.5f;
    private static float mCustomBackgroundAlphaSpeed = 0.01f;

    private static float mCustomBackgroundMaxAlpha = 0.70f;
    private static float mCustomBackgroundMinAlpha = 0.15f;

    private static float mMovingOverlayAlpha = 0.5f;
    private static float mMovingOverlayAlphaSpeed = 0.01f;

    private static float mMovingOverlayMaxAlpha = 0.55f;
    private static float mMovingOverlayMinAlpha = 0.25f;

    private static float xMovingOverlayVelocity = 1;
    private static float yMovingOverlayVelocity = 1;

    /**
     * Screen saver methods & classes
     */

    static class UpdateMovingOverlayAlphaRunnable implements Runnable{
        // @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    updateMovingOverlayAlpha();
                    Thread.sleep(mUpdateMovingOverlayPositionSleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }catch(Exception e){
                }
            }
        }
    }

    static class UpdateCustomBackgroundAlphaRunnable implements Runnable{
        // @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    updateCustomBackgroundAlpha();
                    Thread.sleep(mUpdateMovingOverlayAlphaSleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }catch(Exception e){
                }
            }
        }
    }

    static class UpdateMovingOverlayPositionRunnable implements Runnable{
        // @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    updateMovingOverlayPosition();
                    Thread.sleep(mUpdateMovingOverlayPositionSleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }catch(Exception e){
                }
            }
        }
    }

    public static void start()
    {
        if(mUpdateCustomBackgroundAlphaThread != null)
        {
            mUpdateCustomBackgroundAlphaThread.start();
        }
        else
        {
            mUpdateCustomBackgroundAlphaRunnable = new UpdateCustomBackgroundAlphaRunnable();
            mUpdateCustomBackgroundAlphaThread = new Thread(mUpdateCustomBackgroundAlphaRunnable);
            mUpdateCustomBackgroundAlphaThread.start();
        }


        if(mUpdateMovingOverlayAlphaThread != null)
        {
            mUpdateMovingOverlayAlphaThread.start();
        }
        else
        {
            mUpdateMovingOverlayAlphaRunnable = new UpdateMovingOverlayAlphaRunnable();
            mUpdateMovingOverlayAlphaThread = new Thread(mUpdateMovingOverlayAlphaRunnable);
            mUpdateMovingOverlayAlphaThread.start();
        }

        if(mUpdateMovingOverlayPositionThread != null)
        {
            mUpdateMovingOverlayPositionThread.start();
        }
        else
        {
            mUpdateMovingOverlayPositionRunnable = new UpdateMovingOverlayPositionRunnable();
            mUpdateMovingOverlayPositionThread = new Thread(mUpdateMovingOverlayPositionRunnable);
            mUpdateMovingOverlayPositionThread.start();
        }
    }

    public static void stop()
    {
        if(mUpdateCustomBackgroundAlphaThread != null && mUpdateCustomBackgroundAlphaThread.isAlive())
        {
            mUpdateCustomBackgroundAlphaThread.interrupt();
            mUpdateCustomBackgroundAlphaThread = null;
            mUpdateCustomBackgroundAlphaRunnable = null;
        }

        if(mUpdateMovingOverlayAlphaThread != null && mUpdateMovingOverlayAlphaThread.isAlive())
        {
            mUpdateMovingOverlayAlphaThread.interrupt();
            mUpdateMovingOverlayAlphaThread = null;
            mUpdateMovingOverlayAlphaRunnable = null;
        }
        if(mUpdateMovingOverlayPositionThread != null && mUpdateMovingOverlayPositionThread.isAlive())
        {
            mUpdateMovingOverlayPositionThread.interrupt();
            mUpdateMovingOverlayPositionThread = null;
            mUpdateMovingOverlayPositionRunnable = null;
        }
    }

    private static UpdateCustomBackgroundAlphaRunnable mUpdateCustomBackgroundAlphaRunnable = null;
    private static UpdateMovingOverlayAlphaRunnable mUpdateMovingOverlayAlphaRunnable = null;
    private static UpdateMovingOverlayPositionRunnable mUpdateMovingOverlayPositionRunnable = null;

    private static Thread mUpdateCustomBackgroundAlphaThread = null;
    private static int mUpdateCustomBackgroundAlphaSleep = 100;

    private static Thread mUpdateMovingOverlayAlphaThread = null;
    private static int mUpdateMovingOverlayAlphaSleep = 20;
    private static Thread mUpdateMovingOverlayPositionThread = null;
    private static int mUpdateMovingOverlayPositionSleep = 20;

    public static boolean isViewActive()
    {
        return mView != null;
    }

    private static void updateMovingOverlayAlpha()
    {
        RunInUIThread(new Runnable() {
            @Override
            public void run() {
                mMovingOverlayAlpha += mMovingOverlayAlphaSpeed;
                if(mMovingOverlayAlpha >= mMovingOverlayMaxAlpha)
                {
                    mMovingOverlayAlpha = mMovingOverlayMaxAlpha;
                    mMovingOverlayAlphaSpeed *= -1.0f;
                }
                else if(mMovingOverlayAlpha <= mMovingOverlayMinAlpha)
                {
                    mMovingOverlayAlpha = mMovingOverlayMinAlpha;
                    mMovingOverlayAlphaSpeed *= -1.0f;
                }
                rlMovingOverlay.setAlpha(mMovingOverlayAlpha);
            }
        });
    }

    private static void updateCustomBackgroundAlpha()
    {
        RunInUIThread(new Runnable() {
            @Override
            public void run() {
                mCustomBackgroundAlpha += mCustomBackgroundAlphaSpeed;
                if(mCustomBackgroundAlpha >= mCustomBackgroundMaxAlpha)
                {
                    mCustomBackgroundAlpha = mCustomBackgroundMaxAlpha;
                    mCustomBackgroundAlphaSpeed *= -1.0f;
                }
                else if(mCustomBackgroundAlpha <= mCustomBackgroundMinAlpha)
                {
                    mCustomBackgroundAlpha = mCustomBackgroundMinAlpha;
                    mCustomBackgroundAlphaSpeed *= -1.0f;
                }
                vCustomBackgroundView.setAlpha(mCustomBackgroundAlpha);
            }
        });
    }

    private static void updateMovingOverlayPosition()
    {
        RunInUIThread(new Runnable() {
            @Override
            public void run() {

                int x = (int) (rlMovingOverlay.getX() + xMovingOverlayVelocity);
                int y = (int) (rlMovingOverlay.getY() + yMovingOverlayVelocity);

                if (x > rlOverlay.getWidth() - rlMovingOverlay.getWidth() || x < 0) {
                    xMovingOverlayVelocity = -xMovingOverlayVelocity;
                }

                if (y > rlOverlay.getHeight() - rlMovingOverlay.getHeight() || y < 0) {
                    yMovingOverlayVelocity = -yMovingOverlayVelocity;
                }

                rlMovingOverlay.setX(x);
                rlMovingOverlay.setY(y);
            }
        });
    }


    public static void cleanupWindow(Context context) {
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
        if(mMainThreadHandler == null)
            mMainThreadHandler = new Handler(Looper.getMainLooper());

        mMainThreadHandler.post(runnable);
    }

    public static boolean createScreenSaverOverlayWindow(Context context) {
        try
        {
            if(mMainThreadHandler == null)
                mMainThreadHandler = new Handler(Looper.getMainLooper());

            // We save the current state of mView
            // If a view is already existing we wants to remove it correctly.
            View saveView = mView;

            // Retrieve the window service
            if(mWindowManager == null)
                mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            // Create a new View for our layout
            // mView = new View(context);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = inflater.inflate(R.layout.overlay_layout, null);

            mView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);


            rlOverlay = mView.findViewById(R.id.rlOverlay);
            rlMovingOverlay = mView.findViewById(R.id.rlMovingOverlay);
            vCustomBackgroundView = mView.findViewById(R.id.vcustombackground);

            updateCustomBackgroundAlpha();
            updateMovingOverlayAlpha();
            updateMovingOverlayPosition();

            // We create a new layout with the following parameters
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
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

    public static void setOnTouchListener(View.OnTouchListener touchListener)
    {
        if(mView != null)
        {
            mView.setOnTouchListener(touchListener);
        }
    }

}
