package com.ifttt.connect.ui;

class AnalyticsObject {

    String id;
    String type;

    private static String TYPE_CONNECTION = "connection";

    AnalyticsObject(String id, String type) {
        this.id = id;
        this.type = type;
    }

    class Generic extends AnalyticsObject {

        Generic(String id, String type) {
            super(id, type);
        }
    }

    class Connection extends AnalyticsObject {

        String status;
        String connectionType;

        Connection(String id, String status, String connectionType) {
            super(id, TYPE_CONNECTION);
            this.status = status;
            this.connectionType = connectionType;
        }
    }
}
