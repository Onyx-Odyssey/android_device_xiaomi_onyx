/*
 * Copyright (C) 2018 The LineageOS Project
 *           (C) 2026 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.lineageos.settings.otg;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class OTGTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!OTGModeSwitch.isSupported()) {
            return;
        }
        boolean newState = !OTGModeSwitch.isCurrentlyEnabled(this);
        OTGModeSwitch.setEnabled(this, newState);
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        if (!OTGModeSwitch.isSupported()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else {
            tile.setState(OTGModeSwitch.isCurrentlyEnabled(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }
}
