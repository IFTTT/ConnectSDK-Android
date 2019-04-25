package com.ifttt;

import android.os.Parcel;
import android.os.Parcelable;
import com.squareup.moshi.Json;

/**
 * Cover image data structure for a Connection, including image URLs for different dimensions.
 */
public final class CoverImage implements Parcelable {
    /**
     * URL String for cover image in 480x240.
     */
    @Json(name = "480w_url") final String imageUrl480w;

    /**
     * URL String for cover image in 720x360.
     */
    @Json(name = "720w_url") final String imageUrl720w;

    /**
     * URL String for cover image in 1080x540.
     */
    @Json(name = "1080w_url") final String imageUrl1080w;

    /**
     * URL String for cover image in 1440x720.
     */
    @Json(name = "1440w_url") final String imageUrl1440w;

    /**
     * URL String for cover image in 2880x1440.
     */
    @Json(name = "2880w_url") final String imageUrl2880w;

    /**
     * URL String for cover image in 4320x2160.
     */
    @Json(name = "4320w_url") final String imageUrl4320w;

    public CoverImage(String imageUrl480w, String imageUrl720w, String imageUrl1080w, String imageUrl1440w,
            String imageUrl2880w, String imageUrl4320w) {
        this.imageUrl480w = imageUrl480w;
        this.imageUrl720w = imageUrl720w;
        this.imageUrl1080w = imageUrl1080w;
        this.imageUrl1440w = imageUrl1440w;
        this.imageUrl2880w = imageUrl2880w;
        this.imageUrl4320w = imageUrl4320w;
    }

    protected CoverImage(Parcel in) {
        imageUrl480w = in.readString();
        imageUrl720w = in.readString();
        imageUrl1080w = in.readString();
        imageUrl1440w = in.readString();
        imageUrl2880w = in.readString();
        imageUrl4320w = in.readString();
    }

    public static final Creator<CoverImage> CREATOR = new Creator<CoverImage>() {
        @Override
        public CoverImage createFromParcel(Parcel in) {
            return new CoverImage(in);
        }

        @Override
        public CoverImage[] newArray(int size) {
            return new CoverImage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imageUrl480w);
        dest.writeString(imageUrl720w);
        dest.writeString(imageUrl1080w);
        dest.writeString(imageUrl1440w);
        dest.writeString(imageUrl2880w);
        dest.writeString(imageUrl4320w);
    }

    @Override
    public String toString() {
        return "CoverImage{" + "imageUrl480w='" + imageUrl480w + '\'' + ", imageUrl720w='" + imageUrl720w + '\''
                + ", imageUrl1080w='" + imageUrl1080w + '\'' + ", imageUrl1440w='" + imageUrl1440w + '\''
                + ", imageUrl2880w='" + imageUrl2880w + '\'' + ", imageUrl4320w='" + imageUrl4320w + '\'' + '}';
    }
}
