package com.ifttt.connect;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

/**
 * Data structure representing the checkbox field values.
 */
public final class StringArrayFieldValue implements Parcelable {
    public final List<String> value;

    public StringArrayFieldValue(List<String> value) {
        this.value = value;
    }

    protected StringArrayFieldValue(Parcel in) {
        value = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<StringArrayFieldValue> CREATOR = new Creator<StringArrayFieldValue>() {
        @Override
        public StringArrayFieldValue createFromParcel(Parcel in) {
            return new StringArrayFieldValue(in);
        }

        @Override
        public StringArrayFieldValue[] newArray(int size) {
            return new StringArrayFieldValue[size];
        }
    };
}
