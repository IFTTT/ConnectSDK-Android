package com.ifttt;

import android.os.Parcel;
import android.os.Parcelable;
import com.squareup.moshi.Json;

/**
 * Data structure for a service.
 */
@FieldAreNonnullByDefault
public final class Service implements Parcelable {
    @Json(name = "service_id") public final String id;
    @Json(name = "service_name") public final String name;
    @Json(name = "service_short_name") public final String shortName;

    /**
     * A primary service's triggers or actions don't have to be used in the Connection, it can also be the owner service.
     * One use case of the primary service is to display the branding for this Connection.
     */
    @Json(name = "is_primary") public final boolean isPrimary;

    @Json(name = "monochrome_icon_url") public final String monochromeIconUrl;
    @HexColor @Json(name = "brand_color") public final int brandColor;
    public final String url;

    public Service(String id, String name, String shortName, boolean isPrimary, String monochromeIconUrl,
            int brandColor, String url) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.isPrimary = isPrimary;
        this.monochromeIconUrl = monochromeIconUrl;
        this.brandColor = brandColor;
        this.url = url;
    }

    protected Service(Parcel in) {
        id = in.readString();
        name = in.readString();
        shortName = in.readString();
        isPrimary = in.readByte() != 0;
        monochromeIconUrl = in.readString();
        brandColor = in.readInt();
        url = in.readString();
    }

    public static final Creator<Service> CREATOR = new Creator<Service>() {
        @Override
        public Service createFromParcel(Parcel in) {
            return new Service(in);
        }

        @Override
        public Service[] newArray(int size) {
            return new Service[size];
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
        parcel.writeString(shortName);
        parcel.writeByte((byte) (isPrimary ? 1 : 0));
        parcel.writeString(monochromeIconUrl);
        parcel.writeInt(brandColor);
        parcel.writeString(url);
    }
}
