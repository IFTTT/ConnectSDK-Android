package com.ifttt;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.ifttt.ui.IftttConnectButton;
import javax.annotation.Nullable;

import static com.ifttt.AuthenticationResult.NextStep.Complete;
import static com.ifttt.AuthenticationResult.NextStep.ServiceConnection;
import static com.ifttt.AuthenticationResult.NextStep.Unknown;

/**
 * Data structure for Applet authentication status from the web view. To get an instance, use
 * {@link AuthenticationResult#fromIntent(Intent)} on your deep link handler Activity.
 *
 * An instance of the class is used to communicate to an {@link IftttConnectButton} instance, so that the View can
 * properly display the Applet status to your users.
 *
 * @see IftttConnectButton#setAuthenticationResult(AuthenticationResult)
 */
public final class AuthenticationResult implements Parcelable {

    public enum NextStep {
        /**
         * An authentication status that indicates the next step is a service connection. A service connection means
         * that the user will go through an OAuth flow on a web view (via Chrome Custom Tabs). The result will come
         * back as a deep link into your app and can be handled by {@link AuthenticationResult}.
         */
        ServiceConnection,

        /**
         * An authentication status that indicates the Applet authentication flow has been completed, and the Applet
         * has been enabled.
         */
        Complete,

        /**
         * An unknown authentication status, usually this indicates that there is an error during the authentication
         * flow.
         */
        Unknown,

        /**
         *
         */
        Error
    }

    /**
     * Authentication status for {@link IftttConnectButton}. It is used to help the View reflect the current status
     * of the Applet authentication flow and show guidance to users.
     */
    public final NextStep nextStep;

    /**
     * Additional information when {@link #nextStep} is {@link NextStep#ServiceConnection}. This is used to help the
     * IftttConnectButton show proper service information to guide users in the authentication flow.
     *
     * When {@link AuthenticationResult#nextStep} is ServiceConnection, the {@link AuthenticationResult#serviceId}
     * will be non-null, and the value is the service id to be connected next.
     */
    @Nullable public final String serviceId;

    /**
     * Additional information when {@link #nextStep} is {@link NextStep#Error}. In other cases this field is null.
     */
    @Nullable public final String errorType;

    /**
     * Retrieve an instance of {@link AuthenticationResult} from a deep link Intent.
     *
     * @param intent Intent object that should come from your deep link handler Activity.
     * @return An instance of AuthenticationResult that can be used in setting up {@link IftttConnectButton}.
     */
    public static AuthenticationResult fromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            return new AuthenticationResult(Unknown, null, null);
        }

        String nextStepParam = data.getQueryParameter("next_step");
        if ("service_connection".equals(nextStepParam)) {
            String serviceId = data.getQueryParameter("service_id");
            if (serviceId == null || serviceId.length() == 0) {
                return new AuthenticationResult(Unknown, null, null);
            }

            return new AuthenticationResult(ServiceConnection, serviceId, null);
        } else if ("complete".equals(nextStepParam)) {
            return new AuthenticationResult(Complete, null, null);
        } else if ("error".equals(nextStepParam)) {
            String errorType = data.getQueryParameter("error_type");
            return new AuthenticationResult(NextStep.Error, null, errorType);
        }

        return new AuthenticationResult(Unknown, null, null);
    }

    private AuthenticationResult(NextStep nextStep, @Nullable String serviceId, @Nullable String errorType) {
        this.nextStep = nextStep;
        this.serviceId = serviceId;
        this.errorType = errorType;
    }

    protected AuthenticationResult(Parcel in) {
        serviceId = in.readString();
        nextStep = (NextStep) in.readSerializable();
        errorType = in.readString();
    }

    public static final Creator<AuthenticationResult> CREATOR = new Creator<AuthenticationResult>() {
        @Override
        public AuthenticationResult createFromParcel(Parcel in) {
            return new AuthenticationResult(in);
        }

        @Override
        public AuthenticationResult[] newArray(int size) {
            return new AuthenticationResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(serviceId);
        dest.writeSerializable(nextStep);
    }
}
