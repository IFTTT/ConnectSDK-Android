package com.ifttt.location;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ifttt.connect.api.Feature;
import com.ifttt.connect.api.FeatureStep;
import com.ifttt.connect.api.LocationFieldValue;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserFeatureField;
import com.ifttt.connect.api.UserFeatureStep;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public final class LocationEventUploadHelperTest {

    private final Activity activity = Robolectric.buildActivity(TestActivity.class).get();

    @Test
    public void installationIdShouldBeGeneratedOnce() {
        String oldId = LocationEventUploadHelper.getInstallationId(activity);
        String newId = LocationEventUploadHelper.getInstallationId(activity);

        assertThat(oldId).isEqualTo(newId);
    }

    @Test
    public void shouldNotExtractNullUserFeature() {
        List<Feature> features = features(false, false, null);
        Map<String, List<UserFeatureField<LocationFieldValue>>> result
            = LocationEventUploadHelper.extractLocationUserFeatures(features, true);
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldNotExtractDisabledUserFeatures() {
        List<Feature> features = features(true, false, null);
        Map<String, List<UserFeatureField<LocationFieldValue>>> result
            = LocationEventUploadHelper.extractLocationUserFeatures(features, true);
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldOnlyExtractLocationUserFeatureSteps() {
        LocationFieldValue value = new LocationFieldValue(37D, -122D, 100D, "IFTTT");
        List<Feature> features = features(true, true, value);
        Map<String, List<UserFeatureField<LocationFieldValue>>> result
            = LocationEventUploadHelper.extractLocationUserFeatures(features, true);

        assertThat(result).isNotEmpty();
        assertThat(result.get("user_feature_step_id")).hasSize(1);
        assertThat(result.get("user_feature_step_id").get(0).value).isEqualTo(value);
    }

    @Test
    public void shouldExtractAllIfEnablesOnlyFalse() {
        LocationFieldValue value = new LocationFieldValue(37D, -122D, 100D, "IFTTT");
        List<Feature> features = features(true, false, value);
        Map<String, List<UserFeatureField<LocationFieldValue>>> result
            = LocationEventUploadHelper.extractLocationUserFeatures(features, false);

        assertThat(result).isNotEmpty();
        assertThat(result.get("user_feature_step_id")).hasSize(1);
        assertThat(result.get("user_feature_step_id").get(0).value).isEqualTo(value);
    }

    private List<Feature> features(
        boolean hasUserFeature,
        boolean hasEnabledUserFeature,
        @Nullable LocationFieldValue withLocationFieldValue
    ) {
        List<Feature> features = new ArrayList<>();

        List<UserFeatureField> fields = new ArrayList<>();
        fields.add(new UserFeatureField<>(
            withLocationFieldValue != null ? withLocationFieldValue : new LocationFieldValue(0D, 0D, 100D, "address"),
            GeofenceProvider.FIELD_TYPE_LOCATION_ENTER,
            "field_id"
        ));
        fields.add(new UserFeatureField(new TestFieldValue(), "test_field", "field_id_test"));

        List<UserFeatureStep> userFeatureSteps = new ArrayList<>();
        userFeatureSteps.add(new UserFeatureStep(FeatureStep.StepType.Trigger,
            "user_feature_step_id",
            "stepId",
            fields
        ));

        List<UserFeature> userFeatures = new ArrayList<>();
        userFeatures.add(new UserFeature("id", "feature_id", hasEnabledUserFeature, userFeatureSteps));

        if (hasUserFeature) {
            features.add(new Feature("id", "title", "description", "icon", userFeatures));
        } else {
            features.add(new Feature("id", "title", "description", "icon", null));
        }

        return features;
    }

    @Test
    public void shouldExtractStepIdFromEncodedValue() {
        String stepId = "step_id";
        String entryEncoded = LocationEventUploadHelper.getEnterFenceKey(stepId);
        String exitEncoded = LocationEventUploadHelper.getEnterFenceKey(stepId);

        assertThat(LocationEventUploadHelper.extractStepId(entryEncoded)).isEqualTo(stepId);
        assertThat(LocationEventUploadHelper.extractStepId(exitEncoded)).isEqualTo(stepId);
    }

    @Test
    public void shouldNotChangeStepIdWithoutEncoding() {
        String stepId = "step_id";
        assertThat(LocationEventUploadHelper.extractStepId(stepId)).isEqualTo(stepId);
    }

    private static final class TestFieldValue implements Parcelable {

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }
    }
}
