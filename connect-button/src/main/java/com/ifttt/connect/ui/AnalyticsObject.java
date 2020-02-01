package com.ifttt.connect.ui;

import com.ifttt.connect.Connection;

class AnalyticsObject {

    String id;
    String type;

    private static String TYPE_CONNECTION = "connection";
    private static String TYPE_CONNECTION_EMAIL = "connection_email";
    private static String TYPE_BUTTON = "button";
    private static String TYPE_MODAL = "modal";

    private static String ID_WORKS_WITH_IFTTT = "works_with_ifttt";
    private static String ID_CONNECT_INFORMATION = "connect_information";
    private static String ID_PRIVACY_POLICY = "privacy_policy";
    private static String ID_MANAGE = "manage";

    static final AnalyticsObject CONNECT_BUTTON_EMAIL = new AnalyticsObject("", TYPE_CONNECTION_EMAIL);
    static final AnalyticsObject WORKS_WITH_IFTTT = new AnalyticsObject(ID_WORKS_WITH_IFTTT, TYPE_BUTTON);
    static final AnalyticsObject CONNECT_INFORMATION_MODAL = new AnalyticsObject(ID_CONNECT_INFORMATION, TYPE_MODAL);
    static final AnalyticsObject PRIVACY_POLICY = new AnalyticsObject(ID_PRIVACY_POLICY, TYPE_BUTTON);
    static final AnalyticsObject MANAGE_CONNECTION = new AnalyticsObject(ID_MANAGE, TYPE_BUTTON);

    AnalyticsObject(String id, String type) {
        this.id = id;
        this.type = type;
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

    static AnalyticsObject fromService(String serviceModuleName) {
        return new AnalyticsObject(serviceModuleName.concat("_").concat("service_icon"), TYPE_BUTTON);
    }
}
