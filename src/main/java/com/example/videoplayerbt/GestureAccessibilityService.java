package com.example.videoplayerbt;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

public class GestureAccessibilityService extends AccessibilityService {
    private static GestureAccessibilityService instance;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 不需要处理任何辅助功能事件
    }

    @Override
    public void onInterrupt() {
        // 服务中断时的处理
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    public static GestureAccessibilityService getInstance() {
        return instance;
    }

    // 执行向下滑动
    public void performSwipeDown() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int screenHeight = size.y;
        int screenWidth = size.x;

        Path swipePath = new Path();
        swipePath.moveTo(screenWidth / 2, screenHeight / 3);
        swipePath.lineTo(screenWidth / 2, screenHeight * 2 / 3);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(
                swipePath, 0, 300));
        dispatchGesture(gestureBuilder.build(), null, null);
    }

    // 执行向上滑动
    public void performSwipeUp() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int screenHeight = size.y;
        int screenWidth = size.x;

        Path swipePath = new Path();
        swipePath.moveTo(screenWidth / 2, screenHeight * 2 / 3);
        swipePath.lineTo(screenWidth / 2, screenHeight / 3);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(
                swipePath, 0, 300));
        dispatchGesture(gestureBuilder.build(), null, null);
    }

    // 执行双击
    public void performDoubleClick() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int screenWidth = size.x;
        int screenHeight = size.y;

        Path clickPath = new Path();
        clickPath.moveTo(screenWidth / 2, screenHeight / 2);

        // 第一次点击
        GestureDescription.Builder gestureBuilder1 = new GestureDescription.Builder();
        gestureBuilder1.addStroke(new GestureDescription.StrokeDescription(
                clickPath, 0, 100));

        // 使用显式的GestureResultCallback实现
        dispatchGesture(gestureBuilder1.build(), new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                // 第二次点击
                GestureDescription.Builder gestureBuilder2 = new GestureDescription.Builder();
                gestureBuilder2.addStroke(new GestureDescription.StrokeDescription(
                        clickPath, 0, 100));
                dispatchGesture(gestureBuilder2.build(), null, null);
            }
        }, null);
    }
}