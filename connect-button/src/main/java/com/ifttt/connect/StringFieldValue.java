package com.ifttt.connect;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data structure for field types that have string values.
 */
public final class StringFieldValue implements Parcelable {
    public final String value;

    public StringFieldValue(String value) {
        this.value = value;
    }

    protected StringFieldValue(Parcel in) {
        value = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<StringFieldValue> CREATOR = new Creator<StringFieldValue>() {
        @Override
        public StringFieldValue createFromParcel(Parcel in) {
            return new StringFieldValue(in);
        }

        @Override
        public StringFieldValue[] newArray(int size) {
            return new StringFieldValue[size];
        }
    };
}
