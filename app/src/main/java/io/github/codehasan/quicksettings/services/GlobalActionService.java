/*
 * Copyright 2025 Ratul Hasan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.codehasan.quicksettings.services;

import static io.github.codehasan.quicksettings.util.NullSafety.isNullOrEmpty;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class GlobalActionService extends AccessibilityService {
    public static final String ACTION_LOCK_SCREEN = "lock-screen";
    public static final String ACTION_POWER_DIALOG = "power-dialog";
    public static final String ACTION_SCREENSHOT = "screenshot";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !isNullOrEmpty(intent.getAction())) {
            switch (intent.getAction()) {
                case ACTION_LOCK_SCREEN:
                    lockScreen();
                    break;
                case ACTION_POWER_DIALOG:
                    showPowerDialog();
                    break;
                case ACTION_SCREENSHOT:
                    takeScreenShot();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void lockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

    public void takeScreenShot() {
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
    }

    public void showPowerDialog() {
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
    }
}
