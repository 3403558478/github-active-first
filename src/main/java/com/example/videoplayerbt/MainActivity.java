package com.example.videoplayerbt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements VolumeKeyReceiver.VolumeKeyListener {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private TextView statusText;
    private TextView debugText;
    private TextView currentActionText;
    private StringBuilder debugLog;
    private BluetoothAdapter bluetoothAdapter;
    private AudioManager audioManager;
    private VolumeKeyReceiver volumeKeyReceiver;

    @Override
    public void onLogEvent(String message) {
        addDebugLog(message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图
        statusText = findViewById(R.id.statusText);
        debugText = findViewById(R.id.debugText);
        currentActionText = findViewById(R.id.currentActionText);
        debugLog = new StringBuilder();

        // 初始化当前状态显示
        updateCurrentAction("等待操作");

        // 初始化管理器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // 初始化并注册音量键接收器
        volumeKeyReceiver = new VolumeKeyReceiver(this);
        registerReceiver(volumeKeyReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));

        // 检查权限
        checkAndRequestPermissions();

        // 添加初始调试信息
        addDebugLog("应用启动");
        addDebugLog("检查权限和服务状态...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (volumeKeyReceiver != null) {
            unregisterReceiver(volumeKeyReceiver);
        }
    }

    private void updateCurrentAction(String action) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        String displayText = String.format("时间: %s\n当前动作: %s", currentTime, action);

        runOnUiThread(() -> {
            currentActionText.setText(displayText);
            // 更新调试信息
            addDebugLog("状态更新: " + action);
        });
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            addDebugLog("请求必要权限");
        } else {
            initializeBluetooth();
        }
    }

    private void initializeBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            addDebugLog("设备不支持蓝牙");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            addDebugLog("启用蓝牙");
        } else {
            addDebugLog("蓝牙已启用");
        }

        // 检查无障碍服务
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, R.string.error_accessibility_service, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            addDebugLog("请启用无障碍服务");
        } else {
            addDebugLog("无障碍服务已启用");
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            addDebugLog("检查无障碍服务状态失败: " + e.getMessage());
            return false;
        }

        if (accessibilityEnabled == 1) {
            String service = getPackageName() + "/" + GestureAccessibilityService.class.getCanonicalName();
            String enabledServices = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );

            if (enabledServices != null && enabledServices.contains(service)) {
                addDebugLog("无障碍服务正常");
                return true;
            }
        }

        addDebugLog("无障碍服务未启用");
        return false;
    }

    private void addDebugLog(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String log = timestamp + ": " + message + "\n";
        debugLog.insert(0, log);
        runOnUiThread(() -> {
            debugText.setText(debugLog.toString());
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                addDebugLog("所有权限已授予");
                initializeBluetooth();
            } else {
                addDebugLog("部分权限未授予");
                Toast.makeText(this, "需要所有权限才能使用蓝牙功能", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // VolumeKeyListener 接口实现
    @Override
    public void onShortPress() {
        addDebugLog("检测到短按");
        updateCurrentAction("向下滑动");
        GestureAccessibilityService service = GestureAccessibilityService.getInstance();
        if (service != null) {
            service.performSwipeDown();
            statusText.setText("执行向下滑动动作");
            addDebugLog("执行向下滑动");
        } else {
            addDebugLog("无障碍服务未启用");
            Toast.makeText(this, R.string.error_accessibility_service, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDoubleClick() {
        addDebugLog("检测到双击");
        updateCurrentAction("双击操作");
        GestureAccessibilityService service = GestureAccessibilityService.getInstance();
        if (service != null) {
            service.performDoubleClick();
            statusText.setText("执行双击动作");
            addDebugLog("执行双击操作");
        } else {
            addDebugLog("无障碍服务未启用");
            Toast.makeText(this, R.string.error_accessibility_service, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLongPress() {
        addDebugLog("检测到长按开始");
        updateCurrentAction("向上滑动");
        GestureAccessibilityService service = GestureAccessibilityService.getInstance();
        if (service != null) {
            service.performSwipeUp();
            statusText.setText("执行向上滑动动作");
            addDebugLog("执行向上滑动");
        } else {
            addDebugLog("无障碍服务未启用");
            Toast.makeText(this, R.string.error_accessibility_service, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLongPressEnd() {
        addDebugLog("检测到长按结束");
        updateCurrentAction("等待操作");
        statusText.setText(R.string.status_waiting);
    }

    // 处理音量键事件
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            addDebugLog("按键按下: " + (keyCode == KeyEvent.KEYCODE_VOLUME_UP ? "音量+" : "音量-") +
                    " RepeatCount: " + event.getRepeatCount());
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            addDebugLog("按键释放: " + (keyCode == KeyEvent.KEYCODE_VOLUME_UP ? "音量+" : "音量-") +
                    " 持续时间: " + (event.getEventTime() - event.getDownTime()) + "ms");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}