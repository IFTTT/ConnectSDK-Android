package com.ifttt.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.VisibleForTesting;
import com.ifttt.ConnectionApiClient;
import javax.annotation.Nullable;

import static com.ifttt.ui.ConnectResult.NextStep.Complete;
import static com.ifttt.ui.ConnectResult.NextStep.Unknown;

/**
 * Data structure for Connection enable flow status from the web view. To get an instance, use
 * {@link ConnectResult#fromIntent(Intent)} on your deep link handler Activity.
 *
 * An instance of the class is used to communicate to an {@link BaseConnectButton} instance, so that the View can
 * properly display the Connection status to your users.
 *
 * @see BaseConnectButton#setConnectResult(ConnectResult)
 */
public final class ConnectResult implements Parcelable {

    public static final ConnectResult UNKNOWN = new ConnectResult(Unknown, null, null);

    public enum NextStep {
        /**
         * A status that indicates the Connection enable flow has been completed, and the Connection has been enabled.
         */
        Complete,

        /**
         * An unknown status, usually this indicates that there is an error during the flow.
         */
        Unknown,

        /**
         * An error status, this indicates that the enable flow has encountered errors on the in-app browser.
         */
        Error
    }

    /**
     * Status for {@link BaseConnectButton}. It is used to help the View reflect the current status
     * of the Connection enable flow and show guidance to users.
     */
    public final NextStep nextStep;

    /**
     * Additional information when {@link #nextStep} is {@link NextStep#Complete}. This is used to authenticate API
     * calls in {@link ConnectionApiClient}.
     */
    @Nullable public final String userToken;

    /**
     * Additional information when {@link #nextStep} is {@link NextStep#Error}. In other cases this field is null.
     */
    @Nullable public final String errorType;

    /**
     * Retrieve an instance of {@link ConnectResult} from a deep link Intent.
     *
     * @param intent Intent object that should come from your deep link handler Activity.
     * @return An instance of ConnectResult that can be used in setting up {@link BaseConnectButton}.
     */
    public static ConnectResult fromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            return UNKNOWN;
        }

        String nextStepParam = data.getQueryParameter("next_step");
        if ("complete".equals(nextStepParam)) {
            String userToken = data.getQueryParameter("user_token");
            return new ConnectResult(Complete, userToken, null);
        } else if ("error".equals(nextStepParam)) {
            String errorType = data.getQueryParameter("error_type");
            return new ConnectResult(NextStep.Error, null, errorType);
        }

        return UNKNOWN;
    }

    @VisibleForTesting
    ConnectResult(NextStep nextStep, @Nullable String userToken, @Nullable String errorType) {
        this.nextStep = nextStep;
        this.userToken = userToken;
        this.errorType = errorType;
    }

    protected ConnectResult(Parcel in) {
        userToken = in.readString();
        nextStep = (NextStep) in.readSerializable();
        errorType = in.readString();
    }

    public static final Creator<ConnectResult> CREATOR = new Creator<ConnectResult>() {
        @Override
        public ConnectResult createFromParcel(Parcel in) {
            return new ConnectResult(in);
        }

        @Override
        public ConnectResult[] newArray(int size) {
            return new ConnectResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(nextStep);
        dest.writeString(errorType);
    }
}
