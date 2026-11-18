/*
 * Copyright (C) 2016 The OmniROM Project
 *           (C) 2026 Lunaris AOSP
 *           (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.lineageos.settings.otg;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;
import org.lineageos.settings.utils.FileUtils;

public class OTGModeSwitch implements OnPreferenceChangeListener {
    private static final String TAG = "OTGModeSwitch";

    public static final String FILE_NOTHING = "/proc/charger/nt_otg_enable";
    public static final String FILE_QCOM = "/sys/class/power_supply/usb/otg_enable";
    public static final String FILE_BATTERY_OTG = "/sys/class/power_supply/battery/otg_enable";

    private static String getOtgFile() {
        if (FileUtils.isFileWritable(FILE_NOTHING)) {
            return FILE_NOTHING;
        }
        if (FileUtils.isFileWritable(FILE_QCOM)) {
            return FILE_QCOM;
        }
        if (FileUtils.isFileWritable(FILE_BATTERY_OTG)) {
            return FILE_BATTERY_OTG;
        }
        return null;
    }

    public static boolean isSupported() {
        return true;
    }

    public static boolean isCurrentlyEnabled() {
        return isCurrentlyEnabled(null);
    }

    public static boolean isCurrentlyEnabled(Context context) {
        String file = getOtgFile();
        if (file != null) {
            String val = FileUtils.readOneLine(file);
            if (val != null) {
                val = val.trim();
                if (FILE_NOTHING.equals(file)) {
                    return "3".equals(val);
                }
                return "1".equals(val);
            }
        }
        if ("1".equals(SystemProperties.get("persist.sys.otg_enable", "0"))) {
            return true;
        }
        if (context != null) {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(Constants.KEY_OTG_SWITCH, false);
        }
        return false;
    }

    public static boolean setEnabled(Context context, boolean enabled) {
        String file = getOtgFile();
        if (file != null) {
            final String value = FILE_NOTHING.equals(file) ? (enabled ? "3" : "4") : (enabled ? "1" : "0");
            FileUtils.writeLine(file, value);
        }
        try {
            SystemProperties.set("persist.sys.otg_enable", enabled ? "1" : "0");
        } catch (Exception e) {
            Log.w(TAG, "Unable to set persist.sys.otg_enable", e);
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(Constants.KEY_OTG_SWITCH, enabled).apply();
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return setEnabled(preference.getContext(), (Boolean) newValue);
    }
}
