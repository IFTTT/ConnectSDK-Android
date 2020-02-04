package com.ifttt.connect.ui;

import java.util.List;

final class EventsList{
    private List<AnalyticsEventPayload> events;

    EventsList(List<AnalyticsEventPayload> events) {
        this.events = events;
    }
}
