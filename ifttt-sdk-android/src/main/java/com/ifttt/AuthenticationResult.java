package com.ifttt;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import javax.annotation.Nullable;

import static com.ifttt.AuthenticationResult.NextStep.Complete;
import static com.ifttt.AuthenticationResult.NextStep.ServiceConnection;
import static com.ifttt.AuthenticationResult.NextStep.Unknown;

/**
 * Data structure for Applet authentication result from web view redirects.
 */
public final class AuthenticationResult implements Parcelable {

    public enum NextStep {
        ServiceConnection, Complete, Unknown, Error
    }

    public final NextStep nextStep;
    @Nullable public final String serviceId;
    @Nullable public final String errorType;

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
}
