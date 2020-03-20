package com.ifttt.connect;

import android.os.Parcel;
import android.os.Parcelable;
import javax.annotation.Nullable;

/**
 * Data structure representing one configuration field for a {@link UserFeature}. Currently, the supported value types
 * are
 * - {@link CollectionFieldValue}
 * - {@link StringArrayFieldValue}
 * - {@link LocationFieldValue}
 * - {@link StringFieldValue}
 */
public final class UserFeatureField<T extends Parcelable> implements Parcelable {

    @Nullable public final T value;
    public final String fieldId;

    public UserFeatureField(@Nullable T value, String fieldId) {
        this.value = value;
        this.fieldId = fieldId;
    }

    protected UserFeatureField(Parcel in) {
        String className = in.readString();
        T parceledValue = null;
        if (className != null) {
            try {
                parceledValue = in.readParcelable(Class.forName(className).getClassLoader());
            } catch (ClassNotFoundException ignored) {
            }
        }

        value = parceledValue;
        fieldId = in.readString();
    }

    public static final Creator<UserFeatureField> CREATOR = new Creator<UserFeatureField>() {
        @Override
        public UserFeatureField createFromParcel(Parcel in) {
            return new UserFeatureField(in);
        }

        @Override
        public UserFeatureField[] newArray(int size) {
            return new UserFeatureField[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(value.getClass().getName());
        dest.writeParcelable(value, flags);
        dest.writeString(fieldId);
    }
}
