package io.github.codehasan.quicksettings.util;

import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;

import java.util.List;

import io.github.codehasan.quicksettings.services.GlobalActionService;

public class AccessibilityServiceUtil {
    public static boolean isAccessibilityServiceEnabled(@NonNull Context context) {
        AccessibilityManager manager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

        if (manager != null) {
            List<AccessibilityServiceInfo> services =
                    manager.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK);

            for (AccessibilityServiceInfo enabledService : services) {
                ServiceInfo info = enabledService.getResolveInfo().serviceInfo;

                if (info.packageName.equals(context.getPackageName()) &&
                        info.name.equals(GlobalActionService.class.getName()))
                    return true;
            }
        }
        return false;
    }
}
