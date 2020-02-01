package com.ifttt.connect.ui;

import com.squareup.moshi.Json;
import java.util.List;
import java.util.Map;

class EventsList{
    @Json(name = "events") List<Map<String, String>> events;

    EventsList(List<Map<String, String>> events) {
        this.events = events;
    }
}
