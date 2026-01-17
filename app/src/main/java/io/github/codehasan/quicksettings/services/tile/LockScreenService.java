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

import static io.github.codehasan.quicksettings.util.RootUtil.isRootAvailable;
import static io.github.codehasan.quicksettings.util.RootUtil.runRootCommands;

import android.app.AlertDialog;
import android.os.Build;
import android.view.KeyEvent;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.services.GlobalActionService;
import io.github.codehasan.quicksettings.services.common.AccessibilityTile;

public class LockScreenService extends AccessibilityTile {

    @Override
    public String getAction() {
        return GlobalActionService.ACTION_LOCK_SCREEN;
    }

    @Override
    public void onClick() {
        executor.execute(() -> {
            boolean hasRoot = isRootAvailable();

            if (hasRoot) {
                runRootCommands("input keyevent " + KeyEvent.KEYCODE_POWER);
            } else {
                handler.post(this::performNormalFlow);
            }
        });
    }

    private void performNormalFlow() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            String message = getString(
                    R.string.not_supported_below_msg,
                    Build.VERSION_CODES.P,
                    Build.VERSION.SDK_INT);

            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .create();
            showDialog(alertDialog);
            return;
        }
        super.onClick();
    }
}
