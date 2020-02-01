package com.ifttt.connect.ui;

import com.ifttt.connect.Connection;

class AnalyticsObject {

    String id;
    String type;

    private static String TYPE_CONNECTION = "connection";

    AnalyticsObject(String id, String type) {
        this.id = id;
        this.type = type;
    }

    static class GenericAnalyticsObject extends AnalyticsObject {

        GenericAnalyticsObject(String id, String type) {
            super(id, type);
        }
    }

    static class ConnectionAnalyticsObject extends AnalyticsObject {

        String status;

        ConnectionAnalyticsObject(String id, String status) {
            super(id, TYPE_CONNECTION);
            this.status = status;
        }
    }

    static ConnectionAnalyticsObject fromConnnection(Connection connection) {
        return new ConnectionAnalyticsObject(connection.id, connection.status.toString());
    }

    static GenericAnalyticsObject fromConnectButtonEmail() {
        return new GenericAnalyticsObject(null, "connection_email");
    }
}
