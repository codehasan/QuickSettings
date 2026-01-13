/*
 * Copyright 2025 Ratul Hasan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.codehasan.quicksettings.services.common;

import static io.github.codehasan.quicksettings.util.AccessibilityServiceUtil.isAccessibilityServiceEnabled;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.util.TileServiceUtil;

public class BaseAccessibilityTileService extends BaseInactiveTileService {

    @Override
    public void onClick() {
        if (!isAccessibilityServiceEnabled(this)) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.accessibility_service_disabled_msg)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        openAccessibilityServiceSettings();
                        dialog.dismiss();
                    })
                    .setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .create();
            showDialog(alertDialog);
        }
    }

    private void openAccessibilityServiceSettings() {
        try {
            TileServiceUtil.startActivity(this,
                    new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (ActivityNotFoundException ignored) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.accessibility_service_error_message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .create();
            showDialog(alertDialog);
        }
    }
}
