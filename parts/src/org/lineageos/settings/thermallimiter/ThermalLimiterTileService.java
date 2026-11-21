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

import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.Constants;

public class ThermalLimiterTileService extends TileService {
    private static ThermalLimiterTileService sInstance;

    @Override
    public void onStartListening() {
        super.onStartListening();
        sInstance = this;
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        if (sInstance == this) {
            sInstance = null;
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!ThermalLimiter.isSupported()) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = !prefs.getBoolean(Constants.KEY_THERMAL_LIMITER, false);
        prefs.edit().putBoolean(Constants.KEY_THERMAL_LIMITER, enabled).apply();

        if (enabled) {
            ThermalLimiter.startMonitoring(this);
        } else {
            ThermalLimiter.stopMonitoring();
        }

        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        if (!ThermalLimiter.isSupported()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else {
            boolean enabled = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(Constants.KEY_THERMAL_LIMITER, false);
            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);

            if (enabled) {
                Integer temp = ThermalLimiter.getBatteryTemperature(this);
                if (temp != null) {
                    tile.setSubtitle(String.format("%.1f°C", temp / 10.0f));
                } else {
                    tile.setSubtitle(null);
                }
            } else {
                tile.setSubtitle(null);
            }
        }
        tile.updateTile();
    }

    public static void refreshTile() {
        if (sInstance != null) {
            sInstance.updateTile();
        }
    }

    public static void updateTileState(String state, float temp) {
        if (sInstance != null) {
            Tile tile = sInstance.getQsTile();
            if (tile != null) {
                tile.setSubtitle(String.format("%s (%.1f°C)", state, temp));
                tile.updateTile();
            }
        }
    }
}
