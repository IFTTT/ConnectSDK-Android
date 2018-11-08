package com.ifttt;

import android.os.Parcel;
import android.os.Parcelable;
import com.ifttt.ui.IftttConnectButton;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Data structure for an Applet.
 */
@FieldAreNonnullByDefault
public final class Applet implements Parcelable {

    /**
     * User status for an Applet. If the API calls are user authenticated, the Applet status will be one of
     * {@link Status#enabled}, {@link Status#disabled} and {@link Status#never_enabled}.
     *
     * For unauthenticated calls or user cannot be found, the Applet status will always be {@link Status#unknown}.
     */
    public enum Status {
        /**
         * User status for the Applet indicating the Applet is currently activated for the user.
         */
        enabled,

        /**
         * User status for the Applet indicating the Applet is currently disabled for the user.
         */
        disabled,

        /**
         * User status for the Applet indicating the Applet has never been activated for the user, who needs to go
         * through the activation flow to turn it on.
         */
        never_enabled,

        /**
         * Unknown user status for the Applet.
         */
        unknown
    }

    public final String id;
    public final String name;
    public final String description;
    public final Status status;

    /**
     * Date when the Applet is published, can be null if the Applet is not yet published.
     */
    @Nullable public final Date publishedAt;
    public final int enabledCount;

    /**
     * date when the Applet last ran, can be null if the Applet is not activated for the user or it has never run.
     */
    @Nullable public final Date lastRunAt;

    /**
     * URL string that links to the owner service's website.
     */
    public final String url;
    public final List<Service> services;

    /**
     * URL string that links to this Applet's authentication flow. This value should not be used on its own, and the
     * Applet authentication flow should be handled by {@link IftttConnectButton}.
     */
    public final String embeddedUrl;

    // Cached primary service object.
    @Nullable private Service primaryService;

    public Applet(String id, String name, String description, Status status, @Nullable Date publishedAt,
            int enabledCount, @Nullable Date lastRunAt, String url, String embeddedUrl, List<Service> services) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.publishedAt = publishedAt;
        this.enabledCount = enabledCount;
        this.lastRunAt = lastRunAt;
        this.url = url;
        this.embeddedUrl = embeddedUrl;
        this.services = services;
    }

    /**
     * @return the primary {@link Service} for this Applet. A primary service's triggers or actions don't have to be
     * used in the Applet.
     */
    public Service getPrimaryService() {
        if (primaryService == null) {
            for (Service service : services) {
                if (service.isPrimary) {
                    primaryService = service;
                    break;
                }
            }
        }

        if (primaryService == null) {
            throw new AssertionError("Primary service should not be null.");
        }

        return primaryService;
    }

    protected Applet(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        status = Status.valueOf(in.readString());

        long publishedAtTimestamp = in.readLong();
        if (publishedAtTimestamp < 0) {
            publishedAt = null;
        } else {
            publishedAt = new Date(publishedAtTimestamp);
        }

        enabledCount = in.readInt();

        long lastRunAtTimestamp = in.readLong();
        if (lastRunAtTimestamp < 0) {
            lastRunAt = null;
        } else {
            lastRunAt = new Date(lastRunAtTimestamp);
        }

        url = in.readString();
        embeddedUrl = in.readString();
        services = in.createTypedArrayList(Service.CREATOR);
    }

    public static final Creator<Applet> CREATOR = new Creator<Applet>() {
        @Override
        public Applet createFromParcel(Parcel in) {
            return new Applet(in);
        }

        @Override
        public Applet[] newArray(int size) {
            return new Applet[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(description);
        parcel.writeString(status.name());
        parcel.writeLong(publishedAt != null ? publishedAt.getTime() : -1L);
        parcel.writeInt(enabledCount);
        parcel.writeLong(lastRunAt != null ? lastRunAt.getTime() : -1L);
        parcel.writeString(url);
        parcel.writeString(embeddedUrl);
        parcel.writeTypedList(services);
    }
}
