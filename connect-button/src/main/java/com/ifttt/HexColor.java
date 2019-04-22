package com.ifttt;

import com.squareup.moshi.JsonQualifier;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JSON qualifier annotation for integer representation of color values. This will be used in {@link Service} to
 * convert string representation of the color value into integers.
 */
@Retention(RUNTIME)
@JsonQualifier
@interface HexColor {
}
