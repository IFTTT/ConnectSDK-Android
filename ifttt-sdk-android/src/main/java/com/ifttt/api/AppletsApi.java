package com.ifttt.api;

import android.support.annotation.Nullable;
import com.ifttt.Applet;
import java.util.List;

/**
 * API endpoints for showing Applets, currently including showing a list of Applets for a service and showing a single
 * Applet.
 */
public interface AppletsApi {

    /**
     * Sorting method for listing Applets for a given service.
     */
    enum Order {

        /**
         * Sorted by published date in descending order.
         */
        published_at_desc,

        /**
         * Sorted by published date in ascending order.
         */
        published_at_asc,

        /**
         * Sorted by enabled count in descending order.
         */
        enabled_count_desc,

        /**
         * Sorted by enabled count in ascending order.
         */
        enabled_count_asc
    }

    /**
     * Filtering method for listing Applets for a given service.
     */
    enum Platform {
        /**
         * Exclude Applets using iOS services.
         */
        android,

        /**
         * Exclude Applets using Android services.
         */
        ios
    }

    /**
     * API for listing Applets belong to a service. Optionally, a {@link Platform} can be specified to exclude Applets
     * that are using other platform services, and an {@link Order} can be specified to sort Applets based on published
     * date or enabled count.
     *
     * @param serviceId Applets' owner service id.
     * @param platform Filtering platforms other than the specified one.
     * @param order Ordering of the Applets.
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<List<Applet>> listApplets(String serviceId, @Nullable Platform platform, @Nullable Order order);

    /**
     * API for showing an Applet from a service.
     *
     * @param serviceId Applet's owner service id.
     * @param appletId Applet id.
     * @return A {@link PendingResult} for the API call execution.
     */
    PendingResult<Applet> showApplet(String serviceId, String appletId);
}
