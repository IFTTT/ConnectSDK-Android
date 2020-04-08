package com.ifttt.location;

import android.app.PendingIntent;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ifttt.connect.Connection;
import com.ifttt.connect.Feature;
import com.ifttt.connect.FeatureStep;
import com.ifttt.connect.LocationFieldValue;
import com.ifttt.connect.UserFeature;
import com.ifttt.connect.UserFeatureField;
import com.ifttt.connect.UserFeatureStep;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public final class AwarenessGeofenceProviderTest {

    private LocationFieldValue value = new LocationFieldValue(1.0D, 1.0D, 100D, "");
    private UserFeatureField<LocationFieldValue> field = new UserFeatureField<>(value, "LOCATION_ENTER", "id");
    private UserFeatureStep step = new UserFeatureStep(FeatureStep.StepType.Trigger,
        "step_id",
        "stepId", ImmutableList.of(field)
    );
    private UserFeature userFeature = new UserFeature("id", "featureId", true, ImmutableList.of(step));
    private Feature feature = new Feature("id", "title", "description", "iconUrl", ImmutableList.of(userFeature));

    private PendingIntent enter = PendingIntent.getBroadcast(ApplicationProvider.getApplicationContext(),
        0,
        new Intent("action_1"),
        PendingIntent.FLAG_UPDATE_CURRENT
    );
    private PendingIntent exit = PendingIntent.getBroadcast(ApplicationProvider.getApplicationContext(),
        1,
        new Intent("action_2"),
        PendingIntent.FLAG_UPDATE_CURRENT
    );

    @Test
    public void shouldRegisterEnterGeofence() {
        AwarenessGeofenceProvider.diffFences(
            Connection.Status.enabled,
            ImmutableList.of(feature),
            ImmutableSet.of(),
            enter,
            exit,
            new AwarenessGeofenceProvider.DiffCallback() {
                @Override
                public void onAddFence(
                    String key, AwarenessFence value, PendingIntent pendingIntent
                ) {
                    assertThat(key).isEqualTo("step_id");
                    assertThat(pendingIntent).isEqualTo(enter);
                }

                @Override
                public void onRemoveFence(String key) {
                    fail();
                }
            }
        );
    }

    @Test
    public void shouldRegisterExitGeofence() {
        UserFeatureField<LocationFieldValue> field = new UserFeatureField<>(value, "LOCATION_EXIT", "id");
        UserFeatureStep step = new UserFeatureStep(FeatureStep.StepType.Trigger,
            "step_id",
            "stepId", ImmutableList.of(field)
        );
        UserFeature userFeature = new UserFeature("id", "featureId", true, ImmutableList.of(step));
        Feature feature = new Feature("id", "title", "description", "iconUrl", ImmutableList.of(userFeature));

        AwarenessGeofenceProvider.diffFences(
            Connection.Status.enabled,
            ImmutableList.of(feature),
            ImmutableSet.of(),
            enter,
            exit,
            new AwarenessGeofenceProvider.DiffCallback() {
                @Override
                public void onAddFence(
                    String key, AwarenessFence value, PendingIntent pendingIntent
                ) {
                    assertThat(key).isEqualTo("step_id");
                    assertThat(pendingIntent).isEqualTo(exit);
                }

                @Override
                public void onRemoveFence(String key) {
                    fail();
                }
            }
        );
    }

    @Test
    public void shouldRegisterEnterExit() {
        AtomicReference<List<String>> keysRef = new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<PendingIntent>> pendingIntentRef = new AtomicReference<>(new ArrayList<>());
        UserFeatureField<LocationFieldValue> field = new UserFeatureField<>(value, "LOCATION_ENTER_OR_EXIT", "id");
        UserFeatureStep step = new UserFeatureStep(FeatureStep.StepType.Trigger,
            "step_id",
            "stepId", ImmutableList.of(field)
        );
        UserFeature userFeature = new UserFeature("id", "featureId", true, ImmutableList.of(step));
        Feature feature = new Feature("id", "title", "description", "iconUrl", ImmutableList.of(userFeature));

        AwarenessGeofenceProvider.diffFences(
            Connection.Status.enabled,
            ImmutableList.of(feature),
            ImmutableSet.of(),
            enter,
            exit,
            new AwarenessGeofenceProvider.DiffCallback() {
                @Override
                public void onAddFence(
                    String key, AwarenessFence value, PendingIntent pendingIntent
                ) {
                    keysRef.get().add(key);
                    pendingIntentRef.get().add(pendingIntent);
                }

                @Override
                public void onRemoveFence(String key) {
                    fail();
                }
            }
        );

        assertThat(keysRef.get().get(0)).isEqualTo("step_id/enter");
        assertThat(keysRef.get().get(1)).isEqualTo("step_id/exit");
        assertThat(pendingIntentRef.get().get(0)).isEqualTo(enter);
        assertThat(pendingIntentRef.get().get(1)).isEqualTo(exit);
    }

    @Test
    public void shouldNotRegisterExistingGeofences() {
        AwarenessGeofenceProvider.diffFences(
            Connection.Status.enabled,
            ImmutableList.of(feature),
            ImmutableSet.of("step_id"),
            enter,
            exit,
            new AwarenessGeofenceProvider.DiffCallback() {
                @Override
                public void onAddFence(
                    String key, AwarenessFence value, PendingIntent pendingIntent
                ) {
                    fail();
                }

                @Override
                public void onRemoveFence(String key) {
                    fail();
                }
            }
        );
    }

    @Test
    public void shouldNotRegisterWhenConnectionDisabled() {
        AwarenessGeofenceProvider.diffFences(
            Connection.Status.disabled,
            ImmutableList.of(feature),
            ImmutableSet.of(),
            enter,
            exit,
            new AwarenessGeofenceProvider.DiffCallback() {
                @Override
                public void onAddFence(
                    String key, AwarenessFence value, PendingIntent pendingIntent
                ) {
                    fail();
                }

                @Override
                public void onRemoveFence(String key) {
                    fail();
                }
            }
        );
    }

    @Test
    public void shouldNotRegisterWhenFeatureDisabled() {
        UserFeatureStep step = new UserFeatureStep(FeatureStep.StepType.Trigger,
            "step_id",
            "stepId", ImmutableList.of(field)
        );
        UserFeature userFeature = new UserFeature("id", "featureId", false, ImmutableList.of(step));
        Feature feature = new Feature("id", "title", "description", "iconUrl", ImmutableList.of(userFeature));

        AwarenessGeofenceProvider.diffFences(
            Connection.Status.disabled,
            ImmutableList.of(feature),
            ImmutableSet.of(),
            enter,
            exit,
            new AwarenessGeofenceProvider.DiffCallback() {
                @Override
                public void onAddFence(
                    String key, AwarenessFence value, PendingIntent pendingIntent
                ) {
                    fail();
                }

                @Override
                public void onRemoveFence(String key) {
                    fail();
                }
            }
        );
    }

    @Test
    public void shouldRemoveOutdatedGeofences() {
        AwarenessGeofenceProvider.diffFences(
            Connection.Status.enabled,
            ImmutableList.of(),
            ImmutableSet.of("step_id"),
            enter,
            exit,
            new AwarenessGeofenceProvider.DiffCallback() {
                @Override
                public void onAddFence(
                    String key, AwarenessFence value, PendingIntent pendingIntent
                ) {
                    fail();
                }

                @Override
                public void onRemoveFence(String key) {
                    assertThat(key).isEqualTo("step_id");
                }
            }
        );
    }
}
