package com.ifttt.location;

import android.content.Context;
import android.content.SharedPreferences;
import com.ifttt.connect.api.Feature;
import com.ifttt.connect.api.LocationFieldValue;
import com.ifttt.connect.api.UserFeature;
import com.ifttt.connect.api.UserFeatureField;
import com.ifttt.connect.api.UserFeatureStep;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.ifttt.location.GeofenceProvider.LOCATION_FIELD_TYPES_LIST;

final class LocationEventUploadHelper {

    private static final String SHARED_PREFERENCES_CONNECT_LOCATION = "ifttt_connection_location";
    private static final String PREFERENCES_KEY_INSTALLATION_ID = "ifttt_connection_location.installation_id";

    static String getInstallationId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_CONNECT_LOCATION,
            Context.MODE_PRIVATE
        );

        if (preferences.contains(PREFERENCES_KEY_INSTALLATION_ID)) {
            return preferences.getString(PREFERENCES_KEY_INSTALLATION_ID, "");
        } else {
            String installationId = UUID.randomUUID().toString();
            preferences.edit().putString(PREFERENCES_KEY_INSTALLATION_ID, installationId).apply();
            return installationId;
        }
    }

    static Map<String, List<UserFeatureField<LocationFieldValue>>> extractLocationUserFeatures(List<Feature> features) {
        Map<String, List<UserFeatureField<LocationFieldValue>>> results = new LinkedHashMap<>();
        for (Feature feature : features) {
            if (feature.userFeatures == null) {
                // Skip registration if the UserFeature is null or the connection is not enabled.
                continue;
            }

            for (UserFeature userFeature : feature.userFeatures) {
                if (!userFeature.enabled) {
                    // Skip processing disabled UserFeatureSteps.
                    continue;
                }

                for (UserFeatureStep step : userFeature.userFeatureSteps) {
                    for (UserFeatureField userFeatureField : step.fields) {
                        if (!LOCATION_FIELD_TYPES_LIST.contains(userFeatureField.fieldType)) {
                            continue;
                        }
                        if (step.id == null || !(userFeatureField.value instanceof LocationFieldValue)) {
                            continue;
                        }

                        if (results.get(step.id) == null) {
                            results.put(step.id, new ArrayList<>());
                        }
                        results.get(step.id).add(userFeatureField);
                    }
                }
            }
        }

        return results;
    }

    static String getEnterFenceKey(String id) {
        return id.concat("/enter");
    }

    static String getExitFenceKey(String id) {
        return id.concat("/exit");
    }

    static String extractStepId(String fenceKey) {
        int dividerIndex = fenceKey.indexOf("/");
        if (dividerIndex < 0) {
            return fenceKey;
        }

        return fenceKey.substring(0, dividerIndex);
    }

    private LocationEventUploadHelper() {
        throw new AssertionError("No instances.");
    }
}
