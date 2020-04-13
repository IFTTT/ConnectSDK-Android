package com.ifttt.connect.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Nonnull
@TypeQualifierDefault(ElementType.FIELD)
@Retention(RUNTIME)
public @interface FieldAreNonnullByDefault {
}
