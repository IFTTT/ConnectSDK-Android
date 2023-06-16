package com.ifttt.connect.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Objects;

/**
 * Data structure representing the checkbox field values.
 */
public final class CheckBoxFieldValue implements Parcelable {

    public final List<CheckBoxValue> value;

    public CheckBoxFieldValue(List<CheckBoxValue> value) {
        this.value = value;
    }

    protected CheckBoxFieldValue(Parcel in) {
        value = in.createTypedArrayList(CheckBoxValue.CREATOR);
    }

    public static final Creator<CheckBoxFieldValue> CREATOR = new Creator<CheckBoxFieldValue>() {
        @Override
        public CheckBoxFieldValue createFromParcel(Parcel in) {
            return new CheckBoxFieldValue(in);
        }

        @Override
        public CheckBoxFieldValue[] newArray(int size) {
            return new CheckBoxFieldValue[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckBoxFieldValue that = (CheckBoxFieldValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(value);
    }
}
