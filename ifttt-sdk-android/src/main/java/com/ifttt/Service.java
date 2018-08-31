package com.ifttt;

import android.os.Parcel;
import android.os.Parcelable;
import com.squareup.moshi.Json;

/**
 * Data structure for a service.
 */
public final class Service implements Parcelable {
    @Json(name = "service_id") public final String id;
    @Json(name = "service_name") public final String name;

    /**
     * A primary service's triggers or actions don't have to be used in the Applet, it can also be the owner service.
     * One use case of the primary service is to display the branding for this Applet.
     */
    @Json(name = "is_primary") public final boolean isPrimary;

    @Json(name = "monochrome_icon_url") public final String monochromeIconUrl;
    @Json(name = "color_icon_url") public final String colorIconUrl;
    @HexColor @Json(name = "brand_color") public final int brandColor;
    public final String url;

    public Service(String id, String name, boolean isPrimary, String monochromeIconUrl, String colorIconUrl,
            int brandColor, String url) {
        this.id = id;
        this.name = name;
        this.isPrimary = isPrimary;
        this.monochromeIconUrl = monochromeIconUrl;
        this.colorIconUrl = colorIconUrl;
        this.brandColor = brandColor;
        this.url = url;
    }

    protected Service(Parcel in) {
        id = in.readString();
        name = in.readString();
        isPrimary = in.readByte() != 0;
        monochromeIconUrl = in.readString();
        colorIconUrl = in.readString();
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
        parcel.writeByte((byte) (isPrimary ? 1 : 0));
        parcel.writeString(monochromeIconUrl);
        parcel.writeString(colorIconUrl);
        parcel.writeInt(brandColor);
        parcel.writeString(url);
    }
}
