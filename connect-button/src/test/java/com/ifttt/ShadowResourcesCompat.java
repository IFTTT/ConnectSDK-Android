package com.ifttt;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import androidx.annotation.FontRes;
import androidx.core.content.res.ResourcesCompat;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * For some reason we are still seeing the Resources$NotFoundException after upgrading to Robolectric 4.1. Adding this
 * shadow class to work around it for now.
 *
 * source: https://github.com/robolectric/robolectric/issues/3590.
 */
@Implements(ResourcesCompat.class)
public class ShadowResourcesCompat {

    @Implementation
    public static Typeface getFont(@Nonnull Context context, @FontRes int id) {
        return Typeface.DEFAULT;
    }

    @Implementation
    public static Typeface getFont(@Nonnull Context context, @FontRes int id, TypedValue value, int style,
            @Nullable ResourcesCompat.FontCallback fontCallback) throws Resources.NotFoundException {
        return Typeface.DEFAULT;
    }
}
