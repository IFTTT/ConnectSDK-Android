package com.ifttt.connect.ui;

import android.content.Context;
import androidx.annotation.VisibleForTesting;

final class AnalyticsLocation {

    final String id;
    final String type;

    private static final String TYPE_CONNECT_BUTTON = "connect_button";

    static final AnalyticsLocation WORKS_WITH_IFTTT = new AnalyticsLocation("", TYPE_CONNECT_BUTTON);

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    AnalyticsLocation(String id, String type) {
        this.id = id;
        this.type = type;
    }

    static AnalyticsLocation fromConnectButton(Context context) {
        return new AnalyticsLocation(context.getPackageName(),TYPE_CONNECT_BUTTON);
    }

    static AnalyticsLocation fromConnectButtonWithId(String connectionId) {
        return new AnalyticsLocation(connectionId, TYPE_CONNECT_BUTTON);
    }
}
