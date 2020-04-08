package com.ifttt.connect;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

/**
 * Data structure representing an enabled instance of a {@link Feature}. This class contains information about the
 * configuration of the feature, stored in {@link #userFeatureSteps}.
 */
public final class UserFeature implements Parcelable  {

    public final String id;
    public final String featureId;
    public final boolean enabled;
    public final List<UserFeatureStep> userFeatureSteps;

    public UserFeature(String id, String featureId, boolean enabled, List<UserFeatureStep> userFeatureSteps) {
        this.id = id;
        this.featureId = featureId;
        this.enabled = enabled;
        this.userFeatureSteps = userFeatureSteps;
    }

    protected UserFeature(Parcel in) {
        id = in.readString();
        featureId = in.readString();
        enabled = in.readBoolean();
        userFeatureSteps = in.createTypedArrayList(UserFeatureStep.CREATOR);
    }

    public static final Creator<UserFeature> CREATOR = new Creator<UserFeature>() {
        @Override
        public UserFeature createFromParcel(Parcel in) {
            return new UserFeature(in);
        }

        @Override
        public UserFeature[] newArray(int size) {
            return new UserFeature[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(featureId);
        dest.writeBoolean(enabled);
        dest.writeTypedList(userFeatureSteps);
    }
}
