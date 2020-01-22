package com.ifttt.connect.analytics;

import java.util.LinkedHashMap;

class AnalyticsEventPayload {

    String name;
    LinkedHashMap<String, Object> data;

    AnalyticsEventPayload(String name, LinkedHashMap<String, Object> data) {
        this.name = name;
        this.data = data;
    }
}
