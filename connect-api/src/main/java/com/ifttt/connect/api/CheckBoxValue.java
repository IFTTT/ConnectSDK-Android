package com.ifttt.connect.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CheckBoxValue implements Parcelable {
        public final String label;
        public final String value;

        public CheckBoxValue(String label, String value) {
            this.label = label;
            this.value = value;
        }

        protected CheckBoxValue(Parcel in) {
            label = in.readString();
            value = in.readString();
        }

        public static final Creator<CheckBoxValue> CREATOR = new Creator<CheckBoxValue>() {
            @Override
            public CheckBoxValue createFromParcel(Parcel in) {
                return new CheckBoxValue(in);
            }

            @Override
            public CheckBoxValue[] newArray(int size) {
                return new CheckBoxValue[size];
            }
        };

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CheckBoxValue that = (CheckBoxValue) o;
            return Objects.equals(label, that.label) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, value);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(label);
            dest.writeString(value);
        }
    }
