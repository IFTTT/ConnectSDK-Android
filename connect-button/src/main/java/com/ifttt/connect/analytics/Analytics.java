package com.ifttt.connect.analytics;

import android.content.Context;
import com.ifttt.connect.BuildConfig;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

// Consider making builder or singleton pattern
public class Analytics {

    private AnalyticsPreferences preferences;
    private String previousEventName;
    private AnalyticsSender analyticsSender;

    Analytics(Context context) {
        preferences.create(context);
        // TODO: Add path tag for file
        analyticsSender = AnalyticsSender.getInstance(context, "");
    }

    public void optOut() {
        preferences.optOutOfAnalyticsTracking();
    }

    public void trackUiImpression(AnalyticsObject obj, AnalyticsLocation location, AnalyticsLocation sourceLocation) {
        // TODO
    }

    public void trackUiClick(AnalyticsObject obj, AnalyticsLocation location, AnalyticsLocation sourceLocation) {
        // TODO
    }

    public void trackSystemEvent(AnalyticsObject obj, AnalyticsLocation location, AnalyticsLocation sourceLocation) {
        // TODO
    }

    public void trackStateChangeEvent(AnalyticsObject obj, AnalyticsLocation location, AnalyticsLocation sourceLocation) {
        // TODO
    }

    public void trackScreenView(AnalyticsObject obj, AnalyticsLocation location, AnalyticsLocation sourceLocation) {
        // TODO
    }

    private void trackItemEvent(String name, AnalyticsObject obj, AnalyticsLocation location, AnalyticsLocation sourceLocation) {
        if (preferences.getAnalyticsTrackingOptOutPreference()) {
            return;
        }

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();

        data.put("object_id", obj.id);
        data.put("object_type", obj.type);

        // Special attributes that only certain types of objects have.
        mapAttributes(data, obj);

        data.put("location_type", location.type);
        data.put("location_id", location.id);
        data.put("source_location_type", sourceLocation.type);
        data.put("source_location_id", sourceLocation.id);

        data.put("sdk_anonymous_id", preferences.getAnonymousId());

        long timeZoneRawOffset = (long) TimeZone.getDefault().getRawOffset();
        long dstRawOffset = (long) TimeZone.getDefault().getDSTSavings();

        data.put("timezone_offset_secs", TimeUnit.SECONDS.convert(timeZoneRawOffset,TimeUnit.MILLISECONDS));
        data.put("dst_offset_secs", TimeUnit.SECONDS.convert(dstRawOffset, TimeUnit.MILLISECONDS));

        // TODO: Add sdk version code + name, android version
        // TODO: Add application name by accessing context application info

        if (previousEventName != null) {
            data.put("previous_event_name", previousEventName);
        }

        this.previousEventName = name;

        analyticsSender.enqueue(name, data);
    }

    private void mapAttributes(LinkedHashMap<String, Object> data, AnalyticsObject obj) {
        // TODO: Map attributes depending on AnalyticsObject type
    }
}
