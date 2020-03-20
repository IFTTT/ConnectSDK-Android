package com.ifttt.connect;

import android.os.Parcel;
import android.os.Parcelable;
import com.squareup.moshi.Json;

/**
 * Data structure for features within a Connection.
 */
public final class Feature implements Parcelable {

    public final String id;
    public final String title;
    public final String description;
    @Json(name = "icon_url") public final String iconUrl;

    public Feature(String id, String title, String description, String iconUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
    }

    protected Feature(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        iconUrl = in.readString();
    }

    public static final Creator<Feature> CREATOR = new Creator<Feature>() {
        @Override
        public Feature createFromParcel(Parcel in) {
            return new Feature(in);
        }

        @Override
        public Feature[] newArray(int size) {
            return new Feature[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(iconUrl);
    }
}
