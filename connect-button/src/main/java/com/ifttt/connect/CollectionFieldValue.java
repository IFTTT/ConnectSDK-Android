package com.ifttt.connect;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data structure representing the dropdown or double dropdown field value.
 */
public final class CollectionFieldValue implements Parcelable {

    /**
     * Group identifier, this can be used to represent grouped values, such as a double dropdown field.
     */
    public final String group;

    /**
     * User-friendly name for the value.
     */
    public final String label;

    /**
     * Unique identifier for the value, this is usually only used in Connect API requests.
     */
    public final String value;

    public CollectionFieldValue(String group, String label, String value) {
        this.group = group;
        this.label = label;
        this.value = value;
    }

    protected CollectionFieldValue(Parcel in) {
        group = in.readString();
        label = in.readString();
        value = in.readString();
    }

    public static final Creator<CollectionFieldValue> CREATOR = new Creator<CollectionFieldValue>() {
        @Override
        public CollectionFieldValue createFromParcel(Parcel in) {
            return new CollectionFieldValue(in);
        }

        @Override
        public CollectionFieldValue[] newArray(int size) {
            return new CollectionFieldValue[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(group);
        dest.writeString(label);
        dest.writeString(value);
    }
}
