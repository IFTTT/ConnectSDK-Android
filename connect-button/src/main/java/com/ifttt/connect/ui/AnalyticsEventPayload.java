package com.ifttt.connect.ui;

import java.util.Map;

class AnalyticsEventPayload {

    private String name;
    private String timestamp;
    private Map properties;

    AnalyticsEventPayload(String name, String timestamp, Map<String, String> properties) {
        this.name = name;
        this.timestamp = timestamp;
        this.properties = properties;
    }
}
