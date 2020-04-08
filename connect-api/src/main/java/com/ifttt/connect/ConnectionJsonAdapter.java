package com.ifttt.connect;

import com.ifttt.connect.api.ConnectionApi;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.ToJson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * JSON adapter for single Connection object from {@link ConnectionApi#showConnection(String)}.
 */
final class ConnectionJsonAdapter {

    private final JsonReader.Options connectionOptions = JsonReader.Options.of("id",
        "name",
        "description",
        "user_status",
        "url",
        "services",
        "cover_image",
        "features",
        "user_connection"
    );
    private final JsonReader.Options userConnectionOptions = JsonReader.Options.of("user_features");
    private final JsonReader.Options userFeatureOptions = JsonReader.Options.of("id",
        "feature_id",
        "enabled",
        "user_feature_triggers",
        "user_feature_queries",
        "user_feature_actions"
    );
    private final JsonReader.Options triggerOptions = JsonReader.Options.of("feature_trigger_id", "id", "user_fields");
    private final JsonReader.Options queryOptions = JsonReader.Options.of("feature_query_id", "id", "user_fields");
    private final JsonReader.Options actionOptions = JsonReader.Options.of("feature_action_id", "id", "user_fields");
    private final JsonReader.Options fieldOptions = JsonReader.Options.of("field_id", "field_type", "value");

    @FromJson
    Connection fromJson(
        JsonReader jsonReader,
        JsonAdapter<List<FeatureJson>> delegate,
        JsonAdapter<LocationFieldValue> locationDelegate,
        JsonAdapter<CoverImage> coverImageDelegate,
        JsonAdapter<List<Service>> servicesDelegate,
        JsonAdapter<CollectionFieldValue> collectionFieldDelegate,
        JsonAdapter<List<String>> stringListDelegate
    ) throws IOException {
        String id = null;
        String name = null;
        String description = null;
        Connection.Status userStatus = null;
        String url = null;
        List<Service> services = null;
        CoverImage coverImage = null;
        List<FeatureJson> featureJsonList = new ArrayList<>();
        List<Feature> features = new ArrayList<>();
        Map<String, List<UserFeature>> userFeatureGroup = new LinkedHashMap<>();

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            int connectionIndex = jsonReader.selectName(connectionOptions);
            switch (connectionIndex) {
                case 0:
                    id = jsonReader.nextString();
                    break;
                case 1:
                    name = jsonReader.nextString();
                    break;
                case 2:
                    description = jsonReader.nextString();
                    break;
                case 3:
                    if (jsonReader.peek() == JsonReader.Token.STRING) {
                        userStatus = Connection.Status.valueOf(jsonReader.nextString());
                    } else {
                        userStatus = Connection.Status.unknown;
                    }
                    break;
                case 4:
                    url = jsonReader.nextString();
                    break;
                case 5:
                    services = servicesDelegate.fromJson(jsonReader);
                    break;
                case 6:
                    coverImage = coverImageDelegate.fromJson(jsonReader);
                    break;
                case 7:
                    featureJsonList = delegate.fromJson(jsonReader);
                    break;
                case 8:
                    checkNonNull(featureJsonList);
                    userFeatureGroup = fromJsonToUserFeature(jsonReader,
                        locationDelegate,
                        stringListDelegate,
                        collectionFieldDelegate
                    );
                    break;
                default:
                    jsonReader.skipValue();
            }
        }
        jsonReader.endObject();

        checkNonNull(id);
        checkNonNull(name);
        checkNonNull(description);
        checkNonNull(url);
        checkNonNull(services);

        for (FeatureJson featureJson : featureJsonList) {
            features.add(new Feature(featureJson.id,
                featureJson.title,
                featureJson.description,
                featureJson.icon_url,
                userFeatureGroup.get(featureJson.id)
            ));
        }

        return new Connection(id, name, description, userStatus, url, services, coverImage, features);
    }

    @ToJson
    void toJson(JsonWriter jsonWriter, Connection connection) {
        throw new UnsupportedOperationException();
    }

    private Map<String, List<UserFeature>> fromJsonToUserFeature(
        JsonReader jsonReader,
        JsonAdapter<LocationFieldValue> locationDelegate,
        JsonAdapter<List<String>> stringListDelegate,
        JsonAdapter<CollectionFieldValue> collectionFieldDelegate
    ) throws IOException {
        ArrayList<UserFeature> userFeatures = new ArrayList<>();
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            int index = jsonReader.selectName(userConnectionOptions);
            if (index != 0) {
                jsonReader.skipValue();
                continue;
            }

            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                String id = null;
                String featureId = null;
                boolean enabled = true;
                List<UserFeatureStep> steps = new ArrayList<>();

                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    int featureIndex = jsonReader.selectName(userFeatureOptions);
                    switch (featureIndex) {
                        case 0:
                            id = jsonReader.nextString();
                            break;
                        case 1:
                            featureId = jsonReader.nextString();
                            break;
                        case 2:
                            enabled = jsonReader.nextBoolean();
                            break;
                        case 3:
                            // Triggers
                            parseUserSteps(jsonReader,
                                FeatureStep.StepType.Trigger,
                                triggerOptions,
                                locationDelegate,
                                collectionFieldDelegate,
                                stringListDelegate,
                                steps
                            );
                            break;
                        case 4:
                            // Queries
                            parseUserSteps(jsonReader,
                                FeatureStep.StepType.Query,
                                queryOptions,
                                locationDelegate,
                                collectionFieldDelegate,
                                stringListDelegate,
                                steps
                            );
                            break;
                        case 5:
                            // Actions.
                            parseUserSteps(jsonReader,
                                FeatureStep.StepType.Action,
                                actionOptions,
                                locationDelegate,
                                collectionFieldDelegate,
                                stringListDelegate,
                                steps
                            );
                            break;
                        default:
                            jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();

                checkNonNull(id);
                checkNonNull(featureId);

                userFeatures.add(new UserFeature(id, featureId, enabled, steps));
            }
            jsonReader.endArray();
        }
        jsonReader.endObject();

        Map<String, List<UserFeature>> userFeatureGroup = new LinkedHashMap<>();
        for (UserFeature userFeature : userFeatures) {
            if (userFeatureGroup.get(userFeature.featureId) == null) {
                userFeatureGroup.put(userFeature.featureId, new ArrayList<>());
            }
            userFeatureGroup.get(userFeature.featureId).add(userFeature);
        }

        return userFeatureGroup;
    }

    private void parseUserSteps(
        JsonReader jsonReader,
        FeatureStep.StepType type,
        JsonReader.Options options,
        JsonAdapter<LocationFieldValue> locationDelegate,
        JsonAdapter<CollectionFieldValue> collectionFieldDelegate,
        JsonAdapter<List<String>> stringArrayDelegate,
        List<UserFeatureStep> steps
    ) throws IOException {
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            String stepId = null;
            String id = null;
            List<UserFeatureField> fields = new ArrayList<>();
            while (jsonReader.hasNext()) {
                int triggerIndex = jsonReader.selectName(options);
                switch (triggerIndex) {
                    case 0:
                        stepId = jsonReader.nextString();
                        break;
                    case 1:
                        id = jsonReader.nextString();
                        break;
                    case 2:
                        jsonReader.beginArray();
                        while (jsonReader.hasNext()) {
                            String fieldId = null;
                            String fieldType = null;
                            jsonReader.beginObject();
                            while (jsonReader.hasNext()) {
                                int fieldIndex = jsonReader.selectName(fieldOptions);
                                switch (fieldIndex) {
                                    case 0:
                                        fieldId = jsonReader.nextString();
                                        break;
                                    case 1:
                                        fieldType = jsonReader.nextString();
                                        break;
                                    case 2:
                                        checkNonNull(fieldId);
                                        checkNonNull(fieldType);

                                        if (FIELD_TYPES_LOCATION.contains(fieldType)) {
                                            LocationFieldValue locationFieldValue
                                                = locationDelegate.fromJson(jsonReader);
                                            checkNonNull(locationFieldValue);

                                            fields.add(new UserFeatureField<>(locationFieldValue, fieldType, fieldId));
                                        } else if (FIELD_TYPES_COLLECTION.contains(fieldType)) {
                                            CollectionFieldValue collectionFieldValue
                                                = collectionFieldDelegate.fromJson(jsonReader);
                                            checkNonNull(collectionFieldDelegate);

                                            fields.add(new UserFeatureField<>(collectionFieldValue,
                                                fieldType,
                                                fieldId
                                            ));
                                        } else if (FIELD_TYPE_CHECKBOX.equals(fieldType)) {
                                            List<String> arrayValue = stringArrayDelegate.fromJson(jsonReader);
                                            checkNonNull(arrayValue);

                                            StringArrayFieldValue stringArrayFieldValue = new StringArrayFieldValue(
                                                arrayValue);
                                            fields.add(new UserFeatureField<>(stringArrayFieldValue,
                                                fieldType,
                                                fieldId
                                            ));
                                        } else {
                                            fields.add(new UserFeatureField<>(new StringFieldValue(jsonReader.nextString()),
                                                fieldType,
                                                fieldId
                                            ));
                                        }
                                        break;
                                    default:
                                        jsonReader.skipValue();
                                }
                            }
                            jsonReader.endObject();
                        }
                        jsonReader.endArray();
                        break;
                    default:
                        jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            checkNonNull(stepId);
            checkNonNull(id);

            steps.add(new UserFeatureStep(type, id, stepId, fields));
        }
        jsonReader.endArray();
    }

    private static final class FeatureJson {
        final String id;
        final String title;
        final String description;
        final String icon_url;
        final List<FeatureTrigger> feature_triggers;
        final List<FeatureQuery> feature_queries;
        final List<FeatureAction> feature_actions;

        FeatureJson(
            String id,
            String title,
            String description,
            String icon_url,
            List<FeatureTrigger> feature_triggers,
            List<FeatureQuery> feature_queries,
            List<FeatureAction> feature_actions
        ) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.icon_url = icon_url;
            this.feature_triggers = feature_triggers;
            this.feature_queries = feature_queries;
            this.feature_actions = feature_actions;
        }
    }

    private static final class FeatureTrigger {
        final String label;
        final String id;
        final String trigger_id;
        final String service_id;
        final List<Field> fields;

        FeatureTrigger(String label, String id, String trigger_id, String service_id, List<Field> fields) {
            this.label = label;
            this.id = id;
            this.trigger_id = trigger_id;
            this.service_id = service_id;
            this.fields = fields;
        }
    }

    private static final class FeatureQuery {
        final String label;
        final String id;
        final String query_id;
        final String service_id;
        final List<Field> fields;

        FeatureQuery(String label, String id, String query_id, String service_id, List<Field> fields) {
            this.label = label;
            this.id = id;
            this.query_id = query_id;
            this.service_id = service_id;
            this.fields = fields;
        }
    }

    private static final class FeatureAction {
        final String label;
        final String id;
        final String action_id;
        final String service_id;
        final List<Field> fields;

        FeatureAction(String label, String id, String action_id, String service_id, List<Field> fields) {
            this.label = label;
            this.id = id;
            this.action_id = action_id;
            this.service_id = service_id;
            this.fields = fields;
        }
    }

    private static final class Field {
        final String id;
        final String type;

        Field(String id, String type) {
            this.id = id;
            this.type = type;
        }
    }

    private static void checkNonNull(@Nullable Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Cannot be null.");
        }
    }

    private static final Set<String> FIELD_TYPES_LOCATION = new HashSet<>(Arrays.asList("LOCATION_ENTER",
        "LOCATION_EXIT",
        "LOCATION_ENTER_OR_EXIT",
        "LOCATION_RADIUS",
        "LOCATION_POINT"
    ));
    private static final Set<String> FIELD_TYPES_COLLECTION = new HashSet<>(Arrays.asList("COLLECTION_SELECT",
        "DOUBLE_COLLECTION_SELECT"
    ));
    private static final String FIELD_TYPE_CHECKBOX = "CHECKBOX_MULTI";
}
