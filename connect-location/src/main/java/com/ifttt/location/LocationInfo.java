package com.ifttt.location;

import com.squareup.moshi.Json;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

final class LocationInfo {

    private static final DateFormat LOCATION_EVENT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",
        Locale.US
    );

    @Json(name = "channel_id") final String channelId = "941030000";
    @Json(name = "trigger_subscription_id") final String triggerSubscriptionId;
    @Json(name = "record_id") final String recordId = UUID.randomUUID().toString();
    @Json(name = "occurred_at") final String occurredAt;
    @Json(name = "event_type") final String eventType;
    @Json(name = "region_type") final String regionType = "geo";
    @Json(name = "installation_id") final String installationId;

    private LocationInfo(
        String triggerSubscriptionId, String occurredAt, String eventType, String installationId
    ) {
        this.triggerSubscriptionId = triggerSubscriptionId;
        this.occurredAt = occurredAt;
        this.eventType = eventType;
        this.installationId = installationId;
    }

    static LocationInfo entry(String triggerSubscriptionId, String installationId) {
        return new LocationInfo(triggerSubscriptionId,
            LOCATION_EVENT_DATE_FORMAT.format(System.currentTimeMillis()),
            "entry",
            installationId
        );
    }

    static LocationInfo exit(String triggerSubscriptionId, String installationId) {
        return new LocationInfo(triggerSubscriptionId,
            LOCATION_EVENT_DATE_FORMAT.format(System.currentTimeMillis()),
            "exit",
            installationId
        );
    }
}
