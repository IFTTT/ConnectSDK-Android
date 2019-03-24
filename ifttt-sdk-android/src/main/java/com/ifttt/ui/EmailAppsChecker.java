package com.ifttt.ui;

import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckReturnValue;

/**
 * Helper class that facilitates the returning user flow: check the installed email apps on the device, which will be
 * used by the web view to prompt users to open the email app to check for sign-in email.
 */
final class EmailAppsChecker {

    private static final Map<String, String> MAP;

    static {
        LinkedHashMap<String, String> map = new LinkedHashMap<>(4);
        map.put("com.google.android.gm", "gmail");
        map.put("com.microsoft.office.outlook", "ms-outlook");
        map.put("com.samsung.android.email.provider", "samsung-mail");
        map.put("com.yahoo.mobile.client.android.mail", "yahoo-mail");
        MAP = Collections.unmodifiableMap(map);
    }

    private final PackageManager packageManager;

    EmailAppsChecker(PackageManager packageManager) {
        this.packageManager = packageManager;
    }

    @CheckReturnValue
    List<String> detectEmailApps() {
        ArrayList<String> detected = new ArrayList<>();
        for (Map.Entry<String, String> entry : MAP.entrySet()) {
            try {
                packageManager.getApplicationInfo(entry.getKey(), 0);
                detected.add(entry.getValue());
            } catch (PackageManager.NameNotFoundException e) {
                // Could not find email app installed.
            }
        }

        return detected;
    }
}
