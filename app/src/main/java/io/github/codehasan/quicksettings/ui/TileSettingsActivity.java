/*
 * Copyright 2025 Ratul Hasan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.codehasan.quicksettings.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

import io.github.codehasan.quicksettings.services.tile.BluetoothService;
import io.github.codehasan.quicksettings.services.tile.LockScreenService;
import io.github.codehasan.quicksettings.services.tile.PowerMenuService;
import io.github.codehasan.quicksettings.services.tile.ScreenshotService;
import io.github.codehasan.quicksettings.services.tile.VolumeService;

public class TileSettingsActivity extends AppCompatActivity {
    public static final String TAG = "TileSettingsActivity";
    public static final Set<String> accessibilityClasses = Set.of(
            LockScreenService.class.getName(),
            PowerMenuService.class.getName(),
            ScreenshotService.class.getName()
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String launcher = getLauncherClass();

        if (launcher.equals(BluetoothService.class.getName())) {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        } else if (launcher.equals(VolumeService.class.getName())) {
            startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));
        } else if (accessibilityClasses.contains(launcher)) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } else {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + getPackageName())));
        }

        finish();
    }

    private String getLauncherClass() {
        ComponentName componentName;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            componentName = getIntent().getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);
        } else {
            componentName = getIntent().getParcelableExtra(
                    Intent.EXTRA_COMPONENT_NAME, ComponentName.class);
        }
        Log.d(TAG, "Launcher: " + componentName);
        return componentName == null ? "" : componentName.getClassName();
    }
}
