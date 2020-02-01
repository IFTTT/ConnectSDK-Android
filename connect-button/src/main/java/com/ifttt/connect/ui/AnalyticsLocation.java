package com.ifttt.connect.ui;

import android.content.Context;

final class AnalyticsLocation {

    private String id;
    private String type;
    private static final String TYPE_CONNECT_BUTTON = "connect_button";

    AnalyticsLocation(String id, String type) {
        this.id = id;
        this.type = type;
    }

    static AnalyticsLocation fromConnectButton(Context context) {
        return new AnalyticsLocation(context.getPackageName(),TYPE_CONNECT_BUTTON);
    }

    static AnalyticsLocation fromConnectButtonEmail(String connectionId) {
        return new AnalyticsLocation(connectionId, TYPE_CONNECT_BUTTON);
    }
}
