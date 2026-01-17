/*
 * Copyright 2025 Ratul Hasan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.codehasan.quicksettings.services.tile;

import static io.github.codehasan.quicksettings.util.NullSafety.isNullOrEmpty;
import static io.github.codehasan.quicksettings.util.RootUtil.isRootAvailable;
import static io.github.codehasan.quicksettings.util.RootUtil.runRootCommands;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.services.common.StatefulTile;
import io.github.codehasan.quicksettings.util.TileServiceUtil;

public class PlayProtectService extends StatefulTile {
    private static final String PLAY_PROTECT_KEY = "package_verifier_user_consent";
    private static final String PLAY_PROTECT_OFF_VALUE = "-1";
    private static final String PLAY_PROTECT_ON_VALUE = "1";

    @Override
    public void onClick() {
        executor.execute(() -> {
            boolean hasRoot = isRootAvailable();
            boolean hasSecureSettings = hasSecureSettingsPermission();

            if (hasRoot || hasSecureSettings) {
                String newState = isPlayProtectEnabled()
                        ? PLAY_PROTECT_OFF_VALUE : PLAY_PROTECT_ON_VALUE;

                // AUTOMATION FLOW: We have power to toggle it
                if (hasRoot) {
                    runRootCommands("settings put global " + PLAY_PROTECT_KEY + " " + newState);
                } else {
                    writeGlobalSetting(PLAY_PROTECT_KEY, newState);
                }
                handler.post(this::updateTile);
            } else {
                // MANUAL FLOW: We have no powers, ask user to do it
                handler.post(this::openPlayProtectSettings);
            }
        });
    }

    @Override
    public void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean isEnabled = isPlayProtectEnabled();

        tile.setState(isEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(getString(isEnabled ? R.string.on : R.string.off));
        }
        tile.updateTile();
    }

    private void openPlayProtectSettings() {
        Intent playProtect = new Intent();
        playProtect.setClassName(
                "com.google.android.gms",
                "com.google.android.gms.security.settings.VerifyAppsSettingsActivity");
        TileServiceUtil.startActivity(this, playProtect);
    }

    public boolean isPlayProtectEnabled() {
        String state = getGlobalSetting(PLAY_PROTECT_KEY);
        return !isNullOrEmpty(state) && state.equals(PLAY_PROTECT_ON_VALUE);
    }
}