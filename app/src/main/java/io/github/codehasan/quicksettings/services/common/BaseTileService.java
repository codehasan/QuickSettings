package io.github.codehasan.quicksettings.services.common;

import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.service.quicksettings.TileService;

import io.github.codehasan.quicksettings.model.Constraints;

public abstract class BaseTileService extends TileService {
    public abstract Constraints getConstraints();

    public boolean hasSecureSettingsPermission() {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean writeSecureSetting(String key, String value) {
        return Settings.Secure.putString(getContentResolver(), key, value);
    }

    public String getSecureSetting(String key, String defaultValue) {
        String value = Settings.Secure.getString(getContentResolver(), key);
        return value == null ? defaultValue : value;
    }

    public boolean writeGlobalSetting(String key, String value) {
        return Settings.Global.putString(getContentResolver(), key, value);
    }

    public String getGlobalSetting(String key, String defaultValue) {
        String value = Settings.Global.getString(getContentResolver(), key);
        return value == null ? defaultValue : value;
    }
}
