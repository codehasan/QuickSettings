package io.github.codehasan.quicksettings.model;

import androidx.annotation.StringRes;

/**
 * Constraints for TileService
 */
public class Constraints {
    public int minSdk = 24;
    public boolean requiresAccessibility = false;
    public boolean requiresRoot = false;
    public boolean requiresWriteSecureSettings = false;
    @StringRes
    public int description;
}
