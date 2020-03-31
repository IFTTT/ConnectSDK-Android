package com.ifttt.connect;

import android.os.Parcel;
import android.os.Parcelable;
import com.squareup.moshi.Json;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Data structure for features within a Connection.
 */
public final class Feature implements Parcelable {

    /**
     * Unique identifier for the feature.
     */
    public final String id;

    /**
     * User-friendly title for the feature.
     */
    public final String title;

    /**
     * User-friendly description for the feature.
     */
    public final String description;

    /**
     * URL string for the feature icon asset.
     */
    @Json(name = "icon_url") public final String iconUrl;

    /**
     * A list of {@link UserFeature} representing the set of feature instances that a given user has enabled. Null if
     * the user has not enabled this feature, or the API authentication is anonymous.
     */
    @Nullable public final List<UserFeature> userFeatures;

    public Feature(String id, String title, String description, String iconUrl,
            @Nullable List<UserFeature> userFeatures) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
        this.userFeatures = userFeatures;
    }

    protected Feature(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        iconUrl = in.readString();
        userFeatures = in.createTypedArrayList(UserFeature.CREATOR);
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
        dest.writeTypedList(userFeatures);
    }
}
