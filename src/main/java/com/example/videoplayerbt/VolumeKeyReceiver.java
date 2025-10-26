package com.example.videoplayerbt;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;

public class VolumeKeyReceiver extends BroadcastReceiver {
    private final VolumeKeyListener listener;
    private final Handler handler;
    private long lastDownTime = 0;    // 最后一次按下的时间
    private long lastUpTime = 0;      // 最后一次释放的时间
    private int clickCount = 0;       // 点击计数
    private boolean isInLongPress = false;  // 是否处于长按状态

    // 时间阈值
    private static final long DOUBLE_CLICK_INTERVAL = 300;  // 双击间隔时间阈值
    private static final long LONG_PRESS_THRESHOLD = 500;   // 长按触发阈值
    private static final long CLICK_RESET_TIME = 1000;      // 点击重置时间

    public interface VolumeKeyListener {
        void onShortPress();
        void onDoubleClick();
        void onLongPress();
        void onLongPressEnd();
        void onLogEvent(String message);
    }

    public VolumeKeyReceiver(VolumeKeyListener listener) {
        this.listener = listener;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
            return;
        }

        KeyEvent event = intent.getParcelableExtra("android.media.EXTRA_KEY_EVENT");
        if (event == null) return;

        long currentTime = System.currentTimeMillis();

        // 按键按下事件
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            handleKeyDown(currentTime);
        }
        // 按键释放事件
        else if (event.getAction() == KeyEvent.ACTION_UP) {
            handleKeyUp(currentTime);
        }
    }

    @SuppressLint("DefaultLocale")
    private void handleKeyDown(long currentTime) {
        // 记录按下时间
        lastDownTime = currentTime;

        // 检查是否需要重置点击计数
        if (currentTime - lastUpTime > CLICK_RESET_TIME) {
            clickCount = 0;
        }

        // 增加点击计数
        clickCount++;

        logEvent(String.format("按键按下 - 点击计数: %d", clickCount));

        // 设置长按检测
        handler.postDelayed(() -> {
            if (currentTime == lastDownTime && !isInLongPress) {
                isInLongPress = true;
                clickCount = 0;
                logEvent("触发长按");
                listener.onLongPress();
            }
        }, LONG_PRESS_THRESHOLD);
    }

    @SuppressLint("DefaultLocale")
    private void handleKeyUp(long currentTime) {
        lastUpTime = currentTime;
        long pressDuration = currentTime - lastDownTime;

        logEvent(String.format("按键释放 - 持续时间: %dms", pressDuration));

        // 处理长按释放
        if (isInLongPress) {
            isInLongPress = false;
            logEvent("长按结束");
            listener.onLongPressEnd();
            clickCount = 0;
            return;
        }

        // 移除可能的长按检测
        handler.removeCallbacksAndMessages(null);

        // 处理短按和双击
        if (pressDuration < LONG_PRESS_THRESHOLD) {
            if (clickCount == 2 && (currentTime - lastDownTime) <= DOUBLE_CLICK_INTERVAL) {
                // 双击
                logEvent("触发双击");
                listener.onDoubleClick();
                clickCount = 0;
            } else if (clickCount == 1) {
                // 可能的单击，等待一段时间确认不是双击的一部分
                handler.postDelayed(() -> {
                    if (clickCount == 1) {
                        logEvent("触发单击");
                        listener.onShortPress();
                        clickCount = 0;
                    }
                }, DOUBLE_CLICK_INTERVAL);
            }
        }
    }

    private void logEvent(String message) {
        if (listener != null) {
            listener.onLogEvent(String.format("[VolumeKeyReceiver] %s", message));
        }
    }

    // 清理方法，在不再需要接收器时调用
    public void cleanup() {
        handler.removeCallbacksAndMessages(null);
    }
}