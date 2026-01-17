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
import static io.github.codehasan.quicksettings.util.NullSafety.requireNonNullElse;
import static io.github.codehasan.quicksettings.util.RootUtil.isRootAvailable;
import static io.github.codehasan.quicksettings.util.RootUtil.isRootGranted;
import static io.github.codehasan.quicksettings.util.RootUtil.runRootCommands;
import static io.github.codehasan.quicksettings.util.RootUtil.setRootGranted;

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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.Objects;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.services.common.StatefulTile;
import io.github.codehasan.quicksettings.util.TileServiceUtil;

public class GhostModeService extends StatefulTile {
    private BluetoothAdapter bluetoothAdapter;
    private LocationManager locationManager;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;

    // Internal State Tracker for Hotspot
    private int latestHotspotState = 11;

    // Hidden API Constants
    private static final String ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    private static final String EXTRA_WIFI_AP_STATE = "wifi_state";
    private static final int WIFI_AP_STATE_ENABLING = 12;
    private static final int WIFI_AP_STATE_ENABLED = 13;

    // Receiver for Bluetooth, Location, and Hotspot
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = requireNonNullElse(intent.getAction(), "");
            if (ACTION_WIFI_AP_STATE_CHANGED.equals(action)) {
                // Cache the state from the intent to avoid reflection issues later
                latestHotspotState = intent.getIntExtra(EXTRA_WIFI_AP_STATE, 11);
                // Fallback for some OEMs that use a different key
                if (latestHotspotState == 0) {
                    latestHotspotState = intent.getIntExtra("wifi_ap_state", 11);
                }
            }
            updateTile();
        }
    };

    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(@NonNull Network network) {
                    handler.post(GhostModeService.this::updateTile);
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
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

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(LocationManager.MODE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ACTION_WIFI_AP_STATE_CHANGED);
        registerReceiver(stateReceiver, filter);

        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }

        refreshHotspotState();
        updateTile();
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

        if (!isRootGranted(this)) {
            TileServiceUtil.closePanels(this);
            executor.execute(() -> {
                if (isRootAvailable()) {
                    setRootGranted(this, true);
                    performGhostModeOperations();
                } else {
                    handler.post(this::showRootUnavailableDialog);
                }
            });
        } else {
            executor.execute(() -> {
                if (isRootAvailable()) {
                    performGhostModeOperations();
                } else {
                    setRootGranted(this, false);
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
                isLocationOff() &&
                isHotspotOff();

        int newState = everythingOff ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;

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
        return state != WifiManager.WIFI_STATE_ENABLED &&
                state != WifiManager.WIFI_STATE_ENABLING;
    }

    private boolean isBluetoothOff() {
        if (bluetoothAdapter == null) return true;
        int state = bluetoothAdapter.getState();
        return state != BluetoothAdapter.STATE_ON &&
                state != BluetoothAdapter.STATE_TURNING_ON;
    }

    private boolean isCellularDataOff() {
        if (connectivityManager == null) return true;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return true;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (caps == null) return true;
        return !caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    private boolean isLocationOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (locationManager == null) return true;
            return !locationManager.isLocationEnabled();
        } else {
            return Objects.equals(getSecureSetting(LOCATION_MODE), "0");
        }
    }

    private boolean isHotspotOff() {
        if (latestHotspotState == WIFI_AP_STATE_ENABLED ||
                latestHotspotState == WIFI_AP_STATE_ENABLING) {
            return false;
        }

        if (wifiManager == null) return true;
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("getWifiApState");
            method.setAccessible(true);
            int apState = (int) method.invoke(wifiManager);
            return apState != WIFI_AP_STATE_ENABLED &&
                    apState != WIFI_AP_STATE_ENABLING;
        } catch (Exception e) {
            return true;
        }
    }

    private void refreshHotspotState() {
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("getWifiApState");
            method.setAccessible(true);
            latestHotspotState = (int) method.invoke(wifiManager);
        } catch (Exception ignored) {
        }
    }

    private void performGhostModeOperations() {
        runRootCommands(
                // Grant WRITE_SETTINGS to disable tethering hotspot
                "appops set io.github.codehasan.quicksettings WRITE_SETTINGS allow",
                // WiFi
                "svc wifi disable",
                "cmd wifi set-wifi-enabled disabled",
                // Cellular Data
                "svc data disable",
                "cmd phone data disable",
                // Bluetooth
                "svc bluetooth disable",
                "settings put global bluetooth_on 0",
                // Location
                "cmd location set-location-enabled false",
                "settings put secure location_mode 0",
                // Tethering Hotspot
                "cmd wifi stop-softap"
        );
        if (bluetoothAdapter != null) {
            bluetoothAdapter.disable();
        }
        disableHotspotLegacy();
        handler.post(this::updateTile);
    }

    private void disableHotspotLegacy() {
        if (connectivityManager == null || wifiManager == null) return;

        // Android API 26-28
        try {
            Method stopTethering = connectivityManager.getClass()
                    .getDeclaredMethod("stopTethering", int.class);
            stopTethering.setAccessible(true);
            stopTethering.invoke(connectivityManager, 0);
        } catch (Exception e) {
            Log.e("GhostMode", "Failed to stop tethering", e);
        }

        try {
            Method setWifiApEnabled = wifiManager.getClass()
                    .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            setWifiApEnabled.setAccessible(true);
            setWifiApEnabled.invoke(wifiManager, null, false);
        } catch (Exception e) {
            Log.e("GhostMode", "Failed to disable wifi_ap", e);
        }
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