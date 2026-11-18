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

package org.lineageos.settings.charging;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.utils.FileUtils;

public final class ChargingLimiter {
    public static final String KEY_ENABLED = "charging_limiter";
    public static final String KEY_PROFILE = "charging_limiter_profile";
    private static final String TAG = "ChargingLimiter";

    private static final String NODE_SCENARIO_FCC = "/sys/class/qcom-battery/scenario_fcc";
    private static final String NODE_XM_SMART_NIGHT = "/sys/class/xm_power/charger/smart_charge/smart_night";

    private static final String PROPERTY_ENABLED = "persist.sys.nt_chg.enabled";
    private static final String PROPERTY_PROFILE = "persist.sys.nt_chg.profile";

    private ChargingLimiter() {}

    public static boolean isSupported() {
        return true;
    }

    public static boolean isEnabled() {
        return isEnabled(null);
    }

    public static boolean isEnabled(Context context) {
        String prop = SystemProperties.get(PROPERTY_ENABLED, "");
        if (!prop.isEmpty()) {
            return "true".equalsIgnoreCase(prop) || "1".equals(prop);
        }
        if (context != null) {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(Constants.KEY_CHARGING_LIMITER, true);
        }
        return true;
    }

    public static String getProfile() {
        return getProfile(null);
    }

    public static String getProfile(Context context) {
        String profile = SystemProperties.get(PROPERTY_PROFILE, "");
        if (isValidProfile(profile)) {
            return profile;
        }
        if (context != null) {
            profile = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(Constants.KEY_CHARGING_LIMITER_PROFILE, "mid");
            if (isValidProfile(profile)) {
                return profile;
            }
        }
        return "mid";
    }

    public static boolean setEnabled(boolean enabled) {
        return setEnabled(null, enabled);
    }

    public static boolean setEnabled(Context context, boolean enabled) {
        setProperty(PROPERTY_ENABLED, Boolean.toString(enabled));
        if (context != null) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(Constants.KEY_CHARGING_LIMITER, enabled).apply();
        }
        return true;
    }

    public static boolean setProfile(String profile) {
        return setProfile(null, profile);
    }

    public static boolean setProfile(Context context, String profile) {
        if (!isValidProfile(profile)) {
            return false;
        }
        setProperty(PROPERTY_PROFILE, profile);
        if (context != null) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString(Constants.KEY_CHARGING_LIMITER_PROFILE, profile).apply();
        }
        return true;
    }

    public static boolean isValidProfile(String profile) {
        return "slow".equals(profile) || "mid".equals(profile) || "fast".equals(profile);
    }

    private static boolean setProperty(String key, String value) {
        try {
            SystemProperties.set(key, value);
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to change charging limiter property " + key, e);
            return false;
        }
    }
}
