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

import static android.media.AudioManager.STREAM_MUSIC;
import static java.lang.String.valueOf;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;

import io.github.codehasan.quicksettings.services.common.BaseStatefulTileService;
import io.github.codehasan.quicksettings.util.TileServiceUtil;

public class VolumeService extends BaseStatefulTileService {
    private AudioManager audioManager;

    // Receiver to handle volume changes while the tile is visible
    private final BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                updateTile();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        // Register receiver to listen for external volume changes (e.g. physical buttons)
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver, filter);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        // Unregister to save resources when tile is not visible
        unregisterReceiver(volumeReceiver);
    }

    @Override
    public void updateTile() {
        if (audioManager == null) return;

        Tile tile = getQsTile();
        if (tile == null) return;

        // Check if volume is fixed (cannot be changed)
        if (audioManager.isVolumeFixed()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else {
            int currentVolume = audioManager.getStreamVolume(STREAM_MUSIC);

            // Active if volume > 0, Inactive if 0 (muted)
            tile.setState(currentVolume > 0 ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(valueOf(currentVolume));
            }
        }

        tile.updateTile();
    }

    @Override
    public void onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                TileServiceUtil.startActivity(this, new Intent(Settings.Panel.ACTION_VOLUME));
                return;
            } catch (ActivityNotFoundException ignored) {
            }
        }

        if (audioManager != null) {
            audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
        }
    }
}