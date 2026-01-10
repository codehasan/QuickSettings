package io.github.codehasan.quicksettings.services.tile;

import static android.media.AudioManager.STREAM_MUSIC;
import static java.lang.String.valueOf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class VolumeService extends TileService {
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
    public void onTileAdded() {
        updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        // Register receiver to listen for external volume changes (e.g. physical buttons)
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(volumeReceiver, filter);

        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        // Unregister to save resources when tile is not visible
        unregisterReceiver(volumeReceiver);
    }

    private void updateTile() {
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
            tile.setSubtitle(valueOf(currentVolume));
        }

        tile.updateTile();
    }

    @Override
    public void onClick() {
        if (audioManager != null) {
            audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
        }
    }
}