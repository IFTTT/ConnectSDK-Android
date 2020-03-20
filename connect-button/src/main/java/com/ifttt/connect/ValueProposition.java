package com.ifttt.connect;

import android.os.Parcel;
import android.os.Parcelable;
import com.squareup.moshi.Json;

/**
 * Value proposition data structure, including the icon url and the description.
 *
 * @deprecated use {@link Feature} instead.
 */
@Deprecated
public final class ValueProposition implements Parcelable {
    @Json(name = "icon_url") final String iconUrl;
    final String description;

    public ValueProposition(String iconUrl, String description) {
        this.iconUrl = iconUrl;
        this.description = description;
    }

    protected ValueProposition(Parcel in) {
        iconUrl = in.readString();
        description = in.readString();
    }

    public static final Creator<ValueProposition> CREATOR = new Creator<ValueProposition>() {
        @Override
        public ValueProposition createFromParcel(Parcel in) {
            return new ValueProposition(in);
        }

        @Override
        public ValueProposition[] newArray(int size) {
            return new ValueProposition[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(iconUrl);
        dest.writeString(description);
    }

    @Override
    public String toString() {
        return "ValueProposition{" + "iconUrl='" + iconUrl + '\'' + ", description='" + description + '\'' + '}';
    }
}
