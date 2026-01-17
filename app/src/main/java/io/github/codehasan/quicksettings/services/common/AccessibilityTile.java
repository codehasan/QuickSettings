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

import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.BackgroundServiceStartNotAllowedException;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.services.GlobalActionService;
import io.github.codehasan.quicksettings.util.TileServiceUtil;

public abstract class AccessibilityTile extends StatelessTile {

    public abstract String getAction();

    @Override
    public void onClick() {
        super.onClick();

        boolean isAccessibilityEnabled = isAccessibilityServiceEnabled();

        if (!isAccessibilityEnabled) {
            showAccessibilityActionDialog(false);
            return;
        }

        try {
            Intent lockScreenIntent = new Intent(this, GlobalActionService.class)
                    .setAction(getAction());
            startService(lockScreenIntent);
        } catch (BackgroundServiceStartNotAllowedException e) {
            // Service is killed by OEM
            showAccessibilityActionDialog(true);
        }
    }

    public boolean isAccessibilityServiceEnabled() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

        if (manager != null) {
            List<AccessibilityServiceInfo> services =
                    manager.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK);

            for (AccessibilityServiceInfo enabledService : services) {
                ServiceInfo info = enabledService.getResolveInfo().serviceInfo;

                if (info.packageName.equals(getPackageName()) &&
                        info.name.equals(GlobalActionService.class.getName()))
                    return true;
            }
        }
        return false;
    }

    private void showAccessibilityActionDialog(boolean killed) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(killed ?
                        R.string.accessibility_service_killed_msg :
                        R.string.accessibility_service_disabled_msg)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    openAccessibilityServiceSettings();
                })
                .setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .create();
        showDialog(alertDialog);
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
