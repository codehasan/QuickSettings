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

import static android.provider.Settings.Secure.LOCATION_MODE;
import static io.github.codehasan.quicksettings.util.RootUtil.isRootAvailable;
import static io.github.codehasan.quicksettings.util.RootUtil.runRootCommands;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.services.common.BaseStatefulTileService;
import io.github.codehasan.quicksettings.util.TileServiceUtil;

public class GhostModeService extends BaseStatefulTileService {
    private BluetoothAdapter bluetoothAdapter;
    private LocationManager locationManager;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Receiver for Bluetooth and Location (System Broadcasts)
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTile();
        }
    };

    // Callback for Network Changes (Wi-Fi & Cellular)
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(@NonNull Network network) {
                    // Network lost (turned off), update UI on main thread
                    handler.post(GhostModeService.this::updateTile);
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                    // Network state changed, update UI
                    handler.post(GhostModeService.this::updateTile);
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = (bm != null) ? bm.getAdapter() : BluetoothAdapter.getDefaultAdapter();

        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        // Register BroadcastReceiver for Bluetooth & Location
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(LocationManager.MODE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(stateReceiver, filter);

        // Register NetworkCallback for Cellular/WiFi connectivity changes
        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        unregisterReceiver(stateReceiver);
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    @Override
    public void onClick() {
        super.onClick();

        Tile tile = getQsTile();
        if (tile != null && tile.getState() == Tile.STATE_ACTIVE) {
            return;
        }

        File rootMarker = new File(getFilesDir(), "root_granted");
        boolean previouslyGranted = rootMarker.exists();

        // SCENARIO 1: FIRST RUN (Asking for Permission)
        if (!previouslyGranted) {
            // 1. Collapse the panel immediately so user can see the popup
            TileServiceUtil.closePanels(this);

            // 2. Run the blocking check in the BACKGROUND
            executor.execute(() -> {
                if (isRootAvailable()) {
                    // Success! Create marker and run commands
                    try {
                        rootMarker.createNewFile();
                    } catch (IOException ignored) {
                    }

                    // Proceed to disable radios
                    performGhostModeOperations();
                } else {
                    // Failure!
                    handler.post(this::showRootUnavailableDialog);
                }
            });
        } else {
            // SCENARIO 2: NORMAL RUN (Already have Permission)
            // We still run in background to prevent UI freeze
            executor.execute(() -> {
                if (isRootAvailable()) {
                    performGhostModeOperations();
                } else {
                    // Root lost? Delete marker.
                    rootMarker.delete();

                    // Failure!
                    handler.post(this::showRootUnavailableDialog);
                }
            });
        }
    }

    @Override
    public void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean everythingOff = isBluetoothOff() &&
                isWiFiOff() &&
                isCellularDataOff() &&
                isLocationOff();

        int newState = everythingOff ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;

        // Only update if state actually changed to reduce flicker
        if (tile.getState() != newState) {
            tile.setState(newState);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.setSubtitle(getString(everythingOff ? R.string.on : R.string.off));
            }
            tile.updateTile();
        }
    }

    private boolean isWiFiOff() {
        if (wifiManager == null) return true;
        int state = wifiManager.getWifiState();
        return state == WifiManager.WIFI_STATE_DISABLED ||
                state == WifiManager.WIFI_STATE_DISABLING;
    }

    private boolean isBluetoothOff() {
        if (bluetoothAdapter == null) return true;
        int state = bluetoothAdapter.getState();
        return state == BluetoothAdapter.STATE_OFF ||
                state == BluetoothAdapter.STATE_TURNING_OFF;
    }

    private boolean isCellularDataOff() {
        if (connectivityManager == null) return true;

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return true; // No network connected = Data effectively off

        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (caps == null) return true;

        return !caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    private boolean isLocationOff() {
        if (locationManager == null) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return !locationManager.isLocationEnabled();
        } else {
            return getSecureSetting(LOCATION_MODE, "0").equals("0");
        }
    }

    private void performGhostModeOperations() {
        runRootCommands(
                "svc wifi disable",
                "svc data disable",
                "svc bluetooth disable",
                "settings put secure location_mode 0"
        );
        handler.post(this::updateTile);
    }

    private void showRootUnavailableDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.ghost_mode_unsupported_msg)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .create();
        try {
            showDialog(alertDialog);
        } catch (WindowManager.BadTokenException ignored) {
            Toast.makeText(this,
                    R.string.no_root_access,
                    Toast.LENGTH_LONG).show();
        }
    }
}