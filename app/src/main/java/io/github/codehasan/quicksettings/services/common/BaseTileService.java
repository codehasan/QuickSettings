package io.github.codehasan.quicksettings.services.common;

import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.service.quicksettings.TileService;

import androidx.annotation.Nullable;

public abstract class BaseTileService extends TileService {
    public boolean hasSecureSettingsPermission() {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean writeSecureSetting(String key, String value) {
        return Settings.Secure.putString(getContentResolver(), key, value);
    }

    @Nullable
    public String getSecureSetting(String key) {
        return Settings.Secure.getString(getContentResolver(), key);
    }

    public boolean writeGlobalSetting(String key, String value) {
        return Settings.Global.putString(getContentResolver(), key, value);
    }

    @Nullable
    public String getGlobalSetting(String key) {
        return Settings.Global.getString(getContentResolver(), key);
    }
}
