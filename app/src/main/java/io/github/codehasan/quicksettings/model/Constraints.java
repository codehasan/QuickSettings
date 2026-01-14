package io.github.codehasan.quicksettings.model;

/**
 * Constraints for TileService
 */
public class Constraints {
    public int minSdk = 24;
    public boolean requiresAccessibility = false;
    public boolean requiresRoot = false;
    public boolean requiresWriteSecureSettings = false;
    public CharSequence description;
}
