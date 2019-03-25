package com.ifttt.ui;

import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckReturnValue;

/**
 * Helper class that facilitates the returning user flow: check the installed email apps on the device, which will be
 * used by the web view to prompt users to open the email app to check for sign-in email.
 */
final class EmailAppsChecker {

    private static final List<String> APP_LIST;

    static {
        ArrayList<String> list = new ArrayList<>(4);
        list.add("com.google.android.gm");
        list.add("com.microsoft.office.outlook");
        list.add("com.samsung.android.email.provider");
        list.add("com.yahoo.mobile.client.android.mail");
        APP_LIST = Collections.unmodifiableList(list);
    }

    private final PackageManager packageManager;

    EmailAppsChecker(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    @CheckReturnValue
    List<String> detectEmailApps() {
        ArrayList<String> detected = new ArrayList<>();
        for (String app : APP_LIST) {
            try {
                packageManager.getApplicationInfo(app, 0);
                detected.add(app);
            } catch (PackageManager.NameNotFoundException e) {
                // Could not find email app installed.
            }
        }

        return detected;
    }
}
