package com.ifttt.connect.api;

import android.os.Parcel;
import android.os.Parcelable;
import javax.annotation.Nullable;

/**
 * Data structure representing the location field values.
 */
public final class LocationFieldValue implements Parcelable {

    /**
     * Latitude value for the location.
     */
    public final double lat;

    /**
     * Longitude value for the location.
     */
    public final double lng;

    /**
     * Radius value for the geofence type of location. This value can be null for non-geofence type location fields.
     */
    @Nullable public final Double radius;

    /**
     * Address value associated with the latitude and longitude values.
     */
    public final String address;

    public LocationFieldValue(double lat, double lng, @Nullable Double radius, String address) {
        this.lat = lat;
        this.lng = lng;
        this.radius = radius;
        this.address = address;
    }

    protected LocationFieldValue(Parcel in) {
        lat = in.readDouble();
        lng = in.readDouble();
        if (in.readByte() == 0) {
            radius = null;
        } else {
            radius = in.readDouble();
        }
        address = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(lat);
        dest.writeDouble(lng);
        if (radius == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(radius);
        }
        dest.writeString(address);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LocationFieldValue> CREATOR = new Creator<LocationFieldValue>() {
        @Override
        public LocationFieldValue createFromParcel(Parcel in) {
            return new LocationFieldValue(in);
        }

        @Override
        public LocationFieldValue[] newArray(int size) {
            return new LocationFieldValue[size];
        }
    };
}
