package io.github.codehasan.quicksettings.services.tile;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static io.github.codehasan.quicksettings.activity.PermissionActivity.EXTRA_PERMISSIONS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.quicksettings.Tile;

import androidx.annotation.RequiresPermission;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.activity.PermissionActivity;
import io.github.codehasan.quicksettings.services.common.BaseActiveTileService;
import io.github.codehasan.quicksettings.util.TileServiceUtil;

public class BluetoothService extends BaseActiveTileService {
    private BluetoothAdapter bluetoothAdapter;

    // Receiver to listen for system-wide Bluetooth changes
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                updateTile();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        // Register receiver to get real-time updates
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);

        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        // Always unregister to prevent memory leaks
        unregisterReceiver(bluetoothReceiver);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onClick() {
        super.onClick();

        if (isNearbyDevicesGranted()) {
            toggleBluetooth();
        } else {
            requestNearbyDevicesPermission();
        }
    }

    @Override
    public void updateTile() {
        // Safety check: if permission is missing on Android 12+, show Unavailable
        if (!isNearbyDevicesGranted()) {
            Tile tile = getQsTile();

            if (tile != null) {
                tile.setState(Tile.STATE_INACTIVE);
                tile.setSubtitle(getString(R.string.permission_required));
                tile.updateTile();
            }
            return;
        }

        if (bluetoothAdapter == null) return;

        Tile tile = getQsTile();
        if (tile == null) return;

        boolean isEnabled = bluetoothAdapter.isEnabled();

        // Map state: On -> Active, Off/Turning Off -> Inactive
        tile.setState(isEnabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(getString(isEnabled ? R.string.on : R.string.off));
        }
        tile.updateTile();
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    private void toggleBluetooth() {
        if (bluetoothAdapter == null) return;

        // Note: We do NOT call updateTile() here. We wait for the BroadcastReceiver
        // to tell us the state has actually changed.
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        } else {
            bluetoothAdapter.enable();
        }
    }

    private boolean isNearbyDevicesGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                        PackageManager.PERMISSION_GRANTED;
    }

    private void requestNearbyDevicesPermission() {
        String[] permissions = {Manifest.permission.BLUETOOTH_CONNECT};
        Intent permissionIntent = new Intent(this, PermissionActivity.class)
                .putExtra(EXTRA_PERMISSIONS, permissions);
        TileServiceUtil.startActivity(this, permissionIntent);
    }
}