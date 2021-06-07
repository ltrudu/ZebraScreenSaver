package com.zebra.screensaver;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

public class AccessibilityEventsService extends AccessibilityService {

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes |= AccessibilityEvent.TYPE_TOUCH_INTERACTION_START;
        this.setServiceInfo(info);
        createOverlayWindowForUserEventsCatching();
        super.onServiceConnected();
    }

    private void createOverlayWindowForUserEventsCatching() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        FrameLayout layout = new FrameLayout(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS|
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;

        windowManager.addView(layout, params);
        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ScreenSaverService.resetCountdownTimer(AccessibilityEventsService.this);
                return false;
            }
        });
    }

    @Override
       public void onAccessibilityEvent(AccessibilityEvent event) {
           ScreenSaverService.logD("ACCESSIBILITY SERVICE : " + event.toString());
           if(event.getEventType() == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START)
           {
               ScreenSaverService.resetCountdownTimer(this);
           }
       }
    @Override
    public void onInterrupt() {

    }
}
