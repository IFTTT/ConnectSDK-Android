package com.ifttt.connect.ui;

import com.squareup.moshi.Json;
import java.util.List;

class EventsList{
    @Json(name = "events") private List<AnalyticsEventPayload> events;

    EventsList(List<AnalyticsEventPayload> events) {
        this.events = events;
    }
}
