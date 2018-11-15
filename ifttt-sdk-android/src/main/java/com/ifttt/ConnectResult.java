package com.ifttt;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.ifttt.ui.IftttConnectButton;
import javax.annotation.Nullable;

import static com.ifttt.ConnectResult.NextStep.Complete;
import static com.ifttt.ConnectResult.NextStep.ServiceAuthentication;
import static com.ifttt.ConnectResult.NextStep.Unknown;

/**
 * Data structure for Connection enable flow status from the web view. To get an instance, use
 * {@link ConnectResult#fromIntent(Intent)} on your deep link handler Activity.
 *
 * An instance of the class is used to communicate to an {@link IftttConnectButton} instance, so that the View can
 * properly display the Connection status to your users.
 *
 * @see IftttConnectButton#setConnectResult(ConnectResult)
 */
public final class ConnectResult implements Parcelable {

    public enum NextStep {
        /**
         * A status that indicates the next step is a service authentication. A service authentication means that the
         * user will go through an OAuth flow on a web view (via Chrome Custom Tabs). The result will come back as a
         * deep link into your app and can be handled by {@link ConnectResult}.
         */
        ServiceAuthentication,

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
     * Status for {@link IftttConnectButton}. It is used to help the View reflect the current status
     * of the Connection enable flow and show guidance to users.
     */
    public final NextStep nextStep;

    /**
     * Additional information when {@link #nextStep} is {@link NextStep#Complete}. This is used to differentiate between
     * a completed Connection enable flow with and without configuration.
     */
    @Nullable public final boolean completeFromConfig;

    /**
     * Additional information when {@link #nextStep} is {@link NextStep#ServiceAuthentication}. This is used to help the
     * IftttConnectButton show proper service information to guide users in the enable flow.
     *
     * When {@link ConnectResult#nextStep} is ServiceAuthentication, the {@link ConnectResult#serviceId}
     * will be non-null, and the value is the service id to be connected next.
     */
    @Nullable public final String serviceId;

    /**
     * Additional information when {@link #nextStep} is {@link NextStep#Error}. In other cases this field is null.
     */
    @Nullable public final String errorType;

    /**
     * Retrieve an instance of {@link ConnectResult} from a deep link Intent.
     *
     * @param intent Intent object that should come from your deep link handler Activity.
     * @return An instance of ConnectResult that can be used in setting up {@link IftttConnectButton}.
     */
    public static ConnectResult fromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            return new ConnectResult(Unknown, false, null, null);
        }

        String nextStepParam = data.getQueryParameter("next_step");
        if ("service_authentication".equals(nextStepParam)) {
            String serviceId = data.getQueryParameter("service_id");
            if (serviceId == null || serviceId.length() == 0) {
                return new ConnectResult(Unknown, false, null, null);
            }

            return new ConnectResult(ServiceAuthentication, false, serviceId, null);
        } else if ("complete".equals(nextStepParam)) {
            boolean fromConfig = data.getBooleanQueryParameter("config", false);
            return new ConnectResult(Complete, fromConfig, null, null);
        } else if ("error".equals(nextStepParam)) {
            String errorType = data.getQueryParameter("error_type");
            return new ConnectResult(NextStep.Error, false, null, errorType);
        }

        return new ConnectResult(Unknown, false, null, null);
    }

    private ConnectResult(NextStep nextStep, @Nullable boolean completeFromConfig, @Nullable String serviceId,
            @Nullable String errorType) {
        this.nextStep = nextStep;
        this.completeFromConfig = completeFromConfig;
        this.serviceId = serviceId;
        this.errorType = errorType;
    }

    protected ConnectResult(Parcel in) {
        serviceId = in.readString();
        completeFromConfig = in.readInt() == 1;
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
        dest.writeString(serviceId);
        dest.writeInt(completeFromConfig ? 1 : 0);
        dest.writeSerializable(nextStep);
        dest.writeString(errorType);
    }
}
