package io.github.codehasan.quicksettings.util;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.DataOutputStream;
import java.io.IOException;

public class RootUtil {
    public static final String TAG = "RootUtil";

    public static boolean isRootAvailable() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void runRootCommands(@NonNull String... commands) {
        Process su = null;
        DataOutputStream os = null;
        try {
            su = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(su.getOutputStream());

            for (String cmd : commands) {
                os.writeBytes(cmd + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            su.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "Failed to run su commands", e);
        } finally {
            try {
                if (os != null) os.close();
                if (su != null) su.destroy();
            } catch (IOException ignored) {
            }
        }
    }
}
