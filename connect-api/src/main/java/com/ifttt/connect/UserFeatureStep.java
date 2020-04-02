package com.ifttt.connect;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

/**
 * Data structure representing an enabled instance of a {@link FeatureStep}. The configuration information for this step
 * can be found in the {@link UserFeatureField} list.
 */
public final class UserFeatureStep implements Parcelable {

    public final FeatureStep.StepType stepType;
    public final String id;
    public final String stepId;
    public final List<UserFeatureField> fields;

    public UserFeatureStep(
        FeatureStep.StepType stepType, String id, String stepId, List<UserFeatureField> fields
    ) {
        this.stepType = stepType;
        this.id = id;
        this.stepId = stepId;
        this.fields = fields;
    }

    protected UserFeatureStep(Parcel in) {
        stepType = (FeatureStep.StepType) in.readSerializable();
        id = in.readString();
        stepId = in.readString();
        fields = in.createTypedArrayList(UserFeatureField.CREATOR);
    }

    public static final Creator<UserFeatureStep> CREATOR = new Creator<UserFeatureStep>() {
        @Override
        public UserFeatureStep createFromParcel(Parcel in) {
            return new UserFeatureStep(in);
        }

        @Override
        public UserFeatureStep[] newArray(int size) {
            return new UserFeatureStep[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(stepType);
        dest.writeString(id);
        dest.writeString(stepId);
        dest.writeTypedList(fields);
    }
}
