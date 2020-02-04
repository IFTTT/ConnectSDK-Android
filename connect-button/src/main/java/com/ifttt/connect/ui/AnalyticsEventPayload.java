package com.ifttt.connect.ui;

import java.util.Map;

final class AnalyticsEventPayload {

    private final String name;
    private final String timestamp;
    private final Map properties;

    AnalyticsEventPayload(String name, String timestamp, Map<String, String> properties) {
        this.name = name;
        this.timestamp = timestamp;
        this.properties = properties;
    }
}
