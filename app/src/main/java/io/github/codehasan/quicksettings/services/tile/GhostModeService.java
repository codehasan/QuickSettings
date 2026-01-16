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
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.services.common.StatefulTile;
import io.github.codehasan.quicksettings.util.TileServiceUtil;

public class GhostModeService extends StatefulTile {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BluetoothAdapter bluetoothAdapter;
    private LocationManager locationManager;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;

    private static final int WIFI_AP_STATE_DISABLED = 11;
    private static final int WIFI_AP_STATE_DISABLING = 10;

    // Receiver for Bluetooth, Location, and Hotspot (System Broadcasts)
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
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        registerReceiver(stateReceiver, filter);

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
        if (activeNetwork == null) return true;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (caps == null) return true;
        return !caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    private boolean isLocationOff() {
        if (locationManager == null) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return !locationManager.isLocationEnabled();
        } else {
            return Objects.equals(getSecureSetting(LOCATION_MODE), "0");
        }
    }

    private boolean isHotspotOff() {
        if (wifiManager == null) return true;
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("getWifiApState");
            method.setAccessible(true);
            int apState = (int) method.invoke(wifiManager);

            return apState == WIFI_AP_STATE_DISABLED ||
                    apState == WIFI_AP_STATE_DISABLING;
        } catch (Exception e) {
            return true;
        }
    }

    private void performGhostModeOperations() {
        List<String> commands = new ArrayList<>();

        commands.add("svc wifi disable");
        commands.add("svc data disable");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            commands.add("svc bluetooth disable");
        } else {
            bluetoothAdapter.disable();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            commands.add("cmd connectivity stop-tethering");
        } else {
            disableHotspotLegacy();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            commands.add("cmd location set-location-enabled false");
        } else {
            commands.add("settings put secure location_mode 0");
        }

        runRootCommands(commands.toArray(new String[0]));
        handler.post(this::updateTile);
    }

    private void disableHotspotLegacy() {
        if (connectivityManager == null || wifiManager == null) return;

        // Android API 26 and above
        try {
            Method stopTethering = connectivityManager.getClass()
                    .getDeclaredMethod("stopTethering", int.class);
            stopTethering.setAccessible(true);
            stopTethering.invoke(connectivityManager, 0);
        } catch (Exception ignored) {
        }

        try {
            Method setWifiApEnabled = wifiManager.getClass()
                    .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            setWifiApEnabled.setAccessible(true);
            setWifiApEnabled.invoke(wifiManager, null, false);
        } catch (Exception ignored) {
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