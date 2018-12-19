package com.ifttt;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Data structure for an Connection.
 */
@FieldAreNonnullByDefault
public final class Connection implements Parcelable {

    /**
     * User status for a Connection. If the API calls are user authenticated, the Connection status will be one of
     * {@link Status#enabled}, {@link Status#disabled} and {@link Status#never_enabled}.
     *
     * For unauthenticated calls or user cannot be found, the Connection status will always be {@link Status#unknown}.
     */
    public enum Status {
        /**
         * User status for the Connection indicating the Connection is currently activated for the user.
         */
        enabled,

        /**
         * User status for the Connection indicating the Connection is currently disabled for the user.
         */
        disabled,

        /**
         * User status for the Connection indicating the Connection has never been activated for the user, who needs to go
         * through the activation flow to turn it on.
         */
        never_enabled,

        /**
         * Unknown user status for the Connection.
         */
        unknown
    }

    public final String id;
    public final String name;
    public final String description;
    public final Status status;

    /**
     * Date when the Connection is published, can be null if the Connection is not yet published.
     */
    @Nullable public final Date publishedAt;
    public final int enabledCount;

    /**
     * date when the Connection last ran, can be null if the Connection is not activated for the user or it has never run.
     */
    @Nullable public final Date lastRunAt;

    /**
     * URL string that links to the owner service's website.
     */
    public final String url;
    public final List<Service> services;

    // Cached primary service object.
    @Nullable private Service primaryService;

    public Connection(String id, String name, String description, Status status, @Nullable Date publishedAt,
            int enabledCount, @Nullable Date lastRunAt, String url, List<Service> services) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.publishedAt = publishedAt;
        this.enabledCount = enabledCount;
        this.lastRunAt = lastRunAt;
        this.url = url;
        this.services = services;
    }

    /**
     * @return the primary {@link Service} for this Connection. A primary service's triggers or actions don't have to be
     * used in the Connection.
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

    protected Connection(Parcel in) {
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
        services = in.createTypedArrayList(Service.CREATOR);
    }

    public static final Creator<Connection> CREATOR = new Creator<Connection>() {
        @Override
        public Connection createFromParcel(Parcel in) {
            return new Connection(in);
        }

        @Override
        public Connection[] newArray(int size) {
            return new Connection[size];
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
        parcel.writeTypedList(services);
    }
}
