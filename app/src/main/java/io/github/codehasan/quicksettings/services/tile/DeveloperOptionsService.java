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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.services.common.BaseStatelessTileService;
import io.github.codehasan.quicksettings.util.TileServiceUtil;

public class DeveloperOptionsService extends BaseStatelessTileService {

    @Override
    public void onClick() {
        if (isDeveloperOptionsEnabled()) {
            TileServiceUtil.startActivity(this,
                    new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.developer_options)
                    .setMessage(R.string.dev_options_disabled_msg)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        openDeviceInfoSettings();
                    })
                    .setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .create();
            showDialog(alertDialog);
        }
    }

    private void openDeviceInfoSettings() {
        try {
            TileServiceUtil.startActivity(this,
                    new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
        } catch (ActivityNotFoundException ignored) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.dev_options_error_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .create();
            showDialog(alertDialog);
        }
    }

    private boolean isDeveloperOptionsEnabled() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
    }
}
