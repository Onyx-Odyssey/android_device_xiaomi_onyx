/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 *           (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.lineageos.settings.thermallimiter;

import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.utils.FileUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ThermalLimiter {
    private static final String TAG = "ThermalLimiter";

    public static final String KEY_TEMP_WARM = Constants.KEY_TEMP_WARM;
    public static final String KEY_TEMP_HIGH = Constants.KEY_TEMP_HIGH;
    public static final String KEY_TEMP_CRITICAL = Constants.KEY_TEMP_CRITICAL;

    public static final int TEMP_DEFAULT_WARM = 40;
    public static final int TEMP_DEFAULT_HIGH = 43;
    public static final int TEMP_DEFAULT_CRITICAL = 45;
    public static final int TEMP_MIN = 35;
    public static final int TEMP_MAX = 60;

    private static final String BATTERY_TEMP = "/sys/class/power_supply/battery/temp";
    private static final String BATTERY_TEMP_ZONE = "/sys/class/thermal/thermal_zone51/temp";

    // Onyx SM8735 (2+5+1) CPU clusters
    private static final String CPU_LITTLE_MAX_FREQ = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    private static final String[] CPU_LITTLE_CORES = {"cpu0", "cpu1"};
    private static final String CPU_MID_MAX_FREQ = "/sys/devices/system/cpu/cpu2/cpufreq/scaling_max_freq";
    private static final String[] CPU_MID_CORES = {"cpu2", "cpu3", "cpu4", "cpu5", "cpu6"};
    private static final String CPU_PRIME_MAX_FREQ = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq";
    private static final String[] CPU_PRIME_CORES = {"cpu7"};

    // Onyx Adreno 825 GPU
    private static final String GPU_MAX_FREQ = "/sys/class/kgsl/kgsl-3d0/max_gpuclk";

    // Calibrated frequencies for Onyx SM8735
    private static final String CPU_LITTLE_FREQ_NORMAL = "2016000";
    private static final String CPU_LITTLE_FREQ_WARM = "1785600";
    private static final String CPU_LITTLE_FREQ_HIGH = "1516800";
    private static final String CPU_LITTLE_FREQ_CRITICAL = "1190400";

    private static final String CPU_MID_FREQ_NORMAL = "2803200";
    private static final String CPU_MID_FREQ_WARM = "2438400";
    private static final String CPU_MID_FREQ_HIGH = "1920000";
    private static final String CPU_MID_FREQ_CRITICAL = "1516800";

    private static final String CPU_PRIME_FREQ_NORMAL = "2956800";
    private static final String CPU_PRIME_FREQ_WARM = "2284800";
    private static final String CPU_PRIME_FREQ_HIGH = "1920000";
    private static final String CPU_PRIME_FREQ_CRITICAL = "1478400";

    private static final String GPU_FREQ_NORMAL = "1150000000";
    private static final String GPU_FREQ_WARM = "873000000";
    private static final String GPU_FREQ_HIGH = "763000000";
    private static final String GPU_FREQ_CRITICAL = "510000000";

    public static final int STATE_NORMAL = 0;
    public static final int STATE_WARM = 1;
    public static final int STATE_HIGH = 2;
    public static final int STATE_CRITICAL = 3;

    private static Handler mHandler;
    private static Runnable mThermalMonitor;
    private static boolean mIsMonitoring = false;
    private static int mCurrentThermalState = STATE_NORMAL;
    private static Context mContext;
    private static SharedPreferences mPrefs;
    private static BroadcastReceiver mScreenReceiver;
    private static TaskStackListener mTaskStackListener;
    private static String mThrottledPackage = null;
    private static boolean mInteractive = true;

    private static String mLittleCeiling;
    private static String mMidCeiling;
    private static String mPrimeCeiling;

    public static boolean isSupported() {
        if (!FileUtils.isFileReadable(BATTERY_TEMP) && !FileUtils.isFileReadable(BATTERY_TEMP_ZONE)) {
            Log.w(TAG, "Battery temperature node not readable");
            return false;
        }

        boolean cpuSupported = FileUtils.isFileWritable(CPU_LITTLE_MAX_FREQ) &&
                               FileUtils.isFileWritable(CPU_MID_MAX_FREQ) &&
                               FileUtils.isFileWritable(CPU_PRIME_MAX_FREQ);

        if (!cpuSupported) {
            Log.w(TAG, "CPU frequency control not available");
            return false;
        }

        return true;
    }

    public static void startMonitoring(Context context) {
        if (mIsMonitoring || !isSupported()) {
            return;
        }

        mContext = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mCurrentThermalState = STATE_NORMAL;
        mIsMonitoring = true;
        mHandler = new Handler(Looper.getMainLooper());
        cacheClusterCeilings();
        seedInteractiveState();

        mThermalMonitor = new Runnable() {
            @Override
            public void run() {
                if (!mIsMonitoring) return;
                int nextInterval = checkThermals();
                mHandler.postDelayed(this, nextInterval);
            }
        };

        registerScreenReceiver();
        registerTaskStackListener();
        mHandler.post(mThermalMonitor);
        Log.i(TAG, "ThermalLimiter monitoring started");
    }

    public static void stopMonitoring() {
        if (!mIsMonitoring) return;

        mIsMonitoring = false;
        if (mHandler != null && mThermalMonitor != null) {
            mHandler.removeCallbacks(mThermalMonitor);
        }

        unregisterScreenReceiver();
        unregisterTaskStackListener();
        resetFrequencies();
        mThrottledPackage = null;
        Log.i(TAG, "ThermalLimiter monitoring stopped");
    }

    public static boolean isMonitoring() {
        return mIsMonitoring;
    }

    public static int getCurrentThermalState() {
        return mCurrentThermalState;
    }

    private static void cacheClusterCeilings() {
        mLittleCeiling = readCoreFrequency(CPU_LITTLE_CORES[0], CPU_LITTLE_FREQ_NORMAL);
        mMidCeiling = readCoreFrequency(CPU_MID_CORES[0], CPU_MID_FREQ_NORMAL);
        mPrimeCeiling = readCoreFrequency(CPU_PRIME_CORES[0], CPU_PRIME_FREQ_NORMAL);
    }

    private static String readCoreFrequency(String core, String fallback) {
        String path = "/sys/devices/system/cpu/" + core + "/cpufreq/scaling_max_freq";
        String val = FileUtils.readOneLine(path);
        return (val != null && !val.trim().isEmpty()) ? val.trim() : fallback;
    }

    private static int checkThermals() {
        if (!isInteractive()) {
            return 30000;
        }

        final String foreground = getForegroundPackage();
        if (foreground == null || !isPackageThrottled(foreground)) {
            releaseThrottle();
            return 4000;
        }

        mThrottledPackage = foreground;
        final int temperature = getBatteryTemperature();
        adjustFrequencies(temperature);
        return getPollingInterval(temperature);
    }

    private static void releaseThrottle() {
        if (mCurrentThermalState == STATE_NORMAL) {
            mThrottledPackage = null;
            return;
        }
        mThrottledPackage = null;
        resetFrequencies();
        ThermalLimiterTileService.updateTileState("NORMAL", getBatteryTemperature() / 10.0f);
    }

    public static int getBatteryTemperature() {
        String tempStr = FileUtils.readOneLine(BATTERY_TEMP);
        if (tempStr == null) {
            tempStr = FileUtils.readOneLine(BATTERY_TEMP_ZONE);
        }
        if (tempStr == null) {
            return 0;
        }
        try {
            int temp = Integer.parseInt(tempStr.trim());
            if (temp > 10000) {
                temp = temp / 100;
            } else if (temp > 1000) {
                temp = temp / 10;
            }
            if (temp < 0 || temp > 800) {
                return 0;
            }
            return temp;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int getPollingInterval(int temperature) {
        final int[] thresholds = getThresholds();
        if (temperature >= thresholds[2]) return 1000;
        if (temperature >= thresholds[1]) return 1500;
        if (temperature >= thresholds[0]) return 2000;
        return 3000;
    }

    private static void setClusterFrequency(String[] cores, String frequency) {
        for (String core : cores) {
            String cpuPath = "/sys/devices/system/cpu/" + core + "/cpufreq/scaling_max_freq";
            if (FileUtils.isFileWritable(cpuPath)) {
                FileUtils.writeLine(cpuPath, frequency);
            }
        }
    }

    private static void adjustFrequencies(int temperature) {
        final int[] thresholds = getThresholds();

        int newState = STATE_NORMAL;
        String littleFreq = mLittleCeiling;
        String midFreq = mMidCeiling;
        String primeFreq = mPrimeCeiling;
        String gpuFreq = GPU_FREQ_NORMAL;
        String stateName = "NORMAL";

        if (temperature >= thresholds[2]) {
            newState = STATE_CRITICAL;
            littleFreq = CPU_LITTLE_FREQ_CRITICAL;
            midFreq = CPU_MID_FREQ_CRITICAL;
            primeFreq = CPU_PRIME_FREQ_CRITICAL;
            gpuFreq = GPU_FREQ_CRITICAL;
            stateName = "CRITICAL";
        } else if (temperature >= thresholds[1]) {
            newState = STATE_HIGH;
            littleFreq = CPU_LITTLE_FREQ_HIGH;
            midFreq = CPU_MID_FREQ_HIGH;
            primeFreq = CPU_PRIME_FREQ_HIGH;
            gpuFreq = GPU_FREQ_HIGH;
            stateName = "HIGH";
        } else if (temperature >= thresholds[0]) {
            newState = STATE_WARM;
            littleFreq = CPU_LITTLE_FREQ_WARM;
            midFreq = CPU_MID_FREQ_WARM;
            primeFreq = CPU_PRIME_FREQ_WARM;
            gpuFreq = GPU_FREQ_WARM;
            stateName = "WARM";
        }

        if (newState != mCurrentThermalState) {
            mCurrentThermalState = newState;
            setClusterFrequency(CPU_LITTLE_CORES, littleFreq);
            setClusterFrequency(CPU_MID_CORES, midFreq);
            setClusterFrequency(CPU_PRIME_CORES, primeFreq);

            if (FileUtils.isFileWritable(GPU_MAX_FREQ)) {
                FileUtils.writeLine(GPU_MAX_FREQ, gpuFreq);
            }

            float tempCelsius = temperature / 10.0f;
            ThermalLimiterTileService.updateTileState(stateName, tempCelsius);
        }
    }

    private static void resetFrequencies() {
        setClusterFrequency(CPU_LITTLE_CORES,
                mLittleCeiling != null ? mLittleCeiling : CPU_LITTLE_FREQ_NORMAL);
        setClusterFrequency(CPU_MID_CORES,
                mMidCeiling != null ? mMidCeiling : CPU_MID_FREQ_NORMAL);
        setClusterFrequency(CPU_PRIME_CORES,
                mPrimeCeiling != null ? mPrimeCeiling : CPU_PRIME_FREQ_NORMAL);

        if (FileUtils.isFileWritable(GPU_MAX_FREQ)) {
            FileUtils.writeLine(GPU_MAX_FREQ, GPU_FREQ_NORMAL);
        }

        mCurrentThermalState = STATE_NORMAL;
    }

    private static int[] getThresholds() {
        if (mPrefs == null) {
            return new int[]{TEMP_DEFAULT_WARM * 10, TEMP_DEFAULT_HIGH * 10, TEMP_DEFAULT_CRITICAL * 10};
        }
        int warm = mPrefs.getInt(Constants.KEY_TEMP_WARM, TEMP_DEFAULT_WARM) * 10;
        int high = mPrefs.getInt(Constants.KEY_TEMP_HIGH, TEMP_DEFAULT_HIGH) * 10;
        int critical = mPrefs.getInt(Constants.KEY_TEMP_CRITICAL, TEMP_DEFAULT_CRITICAL) * 10;
        return new int[]{warm, high, critical};
    }

    public static Set<String> getSelectedApps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getStringSet(Constants.KEY_THERMAL_LIMITER_APPS, Collections.emptySet());
    }

    public static void setSelectedApps(Context context, Set<String> apps) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putStringSet(Constants.KEY_THERMAL_LIMITER_APPS, new HashSet<>(apps)).apply();
    }

    private static boolean isPackageThrottled(String pkg) {
        if (mContext == null) return false;
        return getSelectedApps(mContext).contains(pkg);
    }

    private static String getForegroundPackage() {
        try {
            var focusedTask = ActivityTaskManager.getService().getFocusedRootTaskInfo();
            if (focusedTask != null && focusedTask.topActivity != null) {
                return focusedTask.topActivity.getPackageName();
            }
            var tasks = ActivityTaskManager.getService().getTasks(1, false, false, Display.DEFAULT_DISPLAY);
            if (tasks != null && !tasks.isEmpty() && tasks.get(0).topActivity != null) {
                return tasks.get(0).topActivity.getPackageName();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isInteractive() {
        return mContext != null && mInteractive;
    }

    private static void seedInteractiveState() {
        final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
        final Display display = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
        mInteractive = display == null || display.getState() == Display.STATE_ON;
    }

    private static void registerScreenReceiver() {
        mScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!mIsMonitoring) return;
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    mInteractive = false;
                    mHandler.removeCallbacks(mThermalMonitor);
                    releaseThrottle();
                } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    mInteractive = true;
                    mHandler.removeCallbacks(mThermalMonitor);
                    mHandler.post(mThermalMonitor);
                }
            }
        };

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private static void unregisterScreenReceiver() {
        if (mContext != null && mScreenReceiver != null) {
            try {
                mContext.unregisterReceiver(mScreenReceiver);
            } catch (Exception ignored) {}
        }
        mScreenReceiver = null;
    }

    private static void registerTaskStackListener() {
        mTaskStackListener = new TaskStackListener() {
            @Override
            public void onTaskStackChanged() {
                if (mIsMonitoring && mHandler != null && mThermalMonitor != null) {
                    mHandler.removeCallbacks(mThermalMonitor);
                    mHandler.post(mThermalMonitor);
                }
            }
        };
        try {
            ActivityTaskManager.getService().registerTaskStackListener(mTaskStackListener);
        } catch (Exception e) {
            Log.w(TAG, "Failed to register TaskStackListener", e);
        }
    }

    private static void unregisterTaskStackListener() {
        if (mTaskStackListener != null) {
            try {
                ActivityTaskManager.getService().unregisterTaskStackListener(mTaskStackListener);
            } catch (Exception ignored) {}
            mTaskStackListener = null;
        }
    }

    public static void requestImmediateCheck() {
        final Handler handler = mHandler;
        if (handler == null) {
            return;
        }
        handler.post(() -> {
            if (mIsMonitoring && mInteractive) {
                mHandler.removeCallbacks(mThermalMonitor);
                mHandler.post(mThermalMonitor);
            }
        });
    }

    public static String getStatusSummary(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(Constants.KEY_THERMAL_LIMITER, false)) {
            return context.getString(org.lineageos.settings.R.string.accessibility_quick_settings_off);
        }
        if (getSelectedApps(context).isEmpty()) {
            return context.getString(org.lineageos.settings.R.string.thermal_limiter_status_no_apps);
        }
        if (mCurrentThermalState == STATE_NORMAL) {
            return context.getString(org.lineageos.settings.R.string.thermal_limiter_status_idle);
        }
        return context.getString(org.lineageos.settings.R.string.thermal_limiter_status_throttling);
    }
}
