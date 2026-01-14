package io.github.codehasan.quicksettings.services.common;

import android.Manifest;
import android.content.pm.PackageManager;
import android.service.quicksettings.TileService;

public class BaseTileService extends TileService {
    public boolean hasSecureSettingsPermission() {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
