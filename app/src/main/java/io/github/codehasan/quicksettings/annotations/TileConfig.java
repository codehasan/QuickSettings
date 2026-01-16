package io.github.codehasan.quicksettings.annotations;

import androidx.annotation.StringRes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TileConfig {
    @StringRes int description();

    int minSdk() default 24;

    boolean requiresRoot() default false;

    boolean requiresAccessibility() default false;

    String[] requiresPermissions() default {};
}