/*
 * Copyright 2025 Ratul Hasan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.codehasan.quicksettings.util;

import static io.github.codehasan.quicksettings.util.NullSafety.isNullOrEmpty;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class SystemPropUtil {
    public static final String TAG = "SystemPropertiesUtil";

    @Nullable
    public static String getProperty(String key) {
        String value = getSystemProperty(key);
        if (isNullOrEmpty(value)) {
            value = getProp(key);
        }
        return value;
    }

    @SuppressLint("PrivateApi")
    @Nullable
    private static String getSystemProperty(String key) {
        try {
            Method get = Class.forName("android.os.SystemProperties")
                    .getDeclaredMethod("get", String.class);
            get.setAccessible(true);
            return (String) get.invoke(null, key);
        } catch (Exception e) {
            Log.e(TAG, "Unable to use SystemProperties.get", e);
            return null;
        }
    }

    @Nullable
    private static String getProp(String key) {
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec("getprop " + key);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
            return reader.readLine();
        } catch (Exception e) {
            Log.e(TAG, "Unable to use getprop command", e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
