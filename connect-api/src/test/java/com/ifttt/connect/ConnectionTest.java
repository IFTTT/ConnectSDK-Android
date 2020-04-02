package com.ifttt.connect;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static com.ifttt.connect.TestUtils.loadConnection;

@RunWith(RobolectricTestRunner.class)
public final class ConnectionTest {

    private Connection connection;

    @Before
    public void setUp() throws Exception {
        connection = loadConnection(getClass().getClassLoader());
    }

    @Test
    public void testConnection() throws Exception {
        assertThat(connection.status).isNotNull();
        assertThat(connection.status).isEqualTo(Connection.Status.unknown);
    }

    @Test
    public void testPrimaryService() {
        assertThat(connection.getPrimaryService()).isNotNull();
        assertThat(connection.getPrimaryService().id).isEqualTo("instagram");
    }

    @Test
    public void testFeatures() {
        assertThat(connection.features).hasSize(2);
        assertThat(connection.features.get(0).title).isEqualTo("Required feature");
    }

    @Test
    public void testUserFeatures() {
        assertThat(connection.features.get(0).userFeatures).hasSize(1);
        assertThat(connection.features.get(0).userFeatures.get(0)).isNotNull();
        assertThat(connection.features.get(0).userFeatures.get(0).featureId).isEqualTo("pmtch6832j");
        assertThat(connection.features.get(0).userFeatures.get(0).id).isEqualTo("5c7f6d49-5fa9-4305-a7db-39ca38484e6e");
    }

    @Test
    public void testUserFeatureSteps() {
        assertThat(connection.features.get(0).userFeatures.get(0).userFeatureSteps).hasSize(2);
        assertThat(connection.features.get(0).userFeatures.get(0).userFeatureSteps.get(0).stepId).isEqualTo("606");
        assertThat(connection.features.get(0).userFeatures.get(0).userFeatureSteps.get(0).id).isEqualTo("707");
        assertThat(connection.features.get(0).userFeatures.get(0).userFeatureSteps.get(0).stepType).isEqualTo(
            FeatureStep.StepType.Trigger);
    }

    @Test
    public void testUserFeatureStepFields() {
        assertThat(connection.features.get(0).userFeatures.get(0).userFeatureSteps.get(0).fields).hasSize(1);
        assertThat(connection.features.get(0).userFeatures.get(0).userFeatureSteps.get(0).fields.get(0).fieldId).isEqualTo("path");
        assertThat(connection.features.get(0).userFeatures.get(0).userFeatureSteps.get(0).fields.get(0).value).isInstanceOf(
            StringFieldValue.class);

        assertThat(connection.features.get(1).userFeatures.get(0).userFeatureSteps.get(0).fields).hasSize(1);
        assertThat(connection.features.get(1).userFeatures.get(0).userFeatureSteps.get(0).fields.get(0).fieldId).isEqualTo("location");
        assertThat(connection.features.get(1).userFeatures.get(0).userFeatureSteps.get(0).fields.get(0).value).isInstanceOf(
            LocationFieldValue.class);
    }
}
