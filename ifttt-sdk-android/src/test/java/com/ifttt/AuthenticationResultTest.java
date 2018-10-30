package com.ifttt;

import android.content.Intent;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public final class AuthenticationResultTest {

    @Test
    public void fromServiceConnection() {
        Intent intent = new Intent().setData(Uri.parse("test://url?next_step=service_connection&service_id=service"));
        AuthenticationResult result = AuthenticationResult.fromIntent(intent);

        assertThat(result.nextStep).isEqualTo(AuthenticationResult.NextStep.ServiceConnection);
        assertThat(result.serviceId).isEqualTo("service");
        assertThat(result.errorType).isNull();
    }

    @Test
    public void fromInvalidServiceConnection() {
        Intent intent = new Intent().setData(Uri.parse("test://url?next_step=service_connection"));
        AuthenticationResult result = AuthenticationResult.fromIntent(intent);

        assertThat(result.nextStep).isEqualTo(AuthenticationResult.NextStep.Unknown);
        assertThat(result.serviceId).isNull();
        assertThat(result.errorType).isNull();
    }

    @Test
    public void fromComplete() {
        Intent intent = new Intent().setData(Uri.parse("test://url?next_step=complete"));
        AuthenticationResult result = AuthenticationResult.fromIntent(intent);

        assertThat(result.nextStep).isEqualTo(AuthenticationResult.NextStep.Complete);
        assertThat(result.serviceId).isNull();
        assertThat(result.errorType).isNull();
    }

    @Test
    public void fromUnknownState() {
        Intent intent = new Intent().setData(Uri.parse("test://url?"));
        AuthenticationResult result = AuthenticationResult.fromIntent(intent);

        assertThat(result.nextStep).isEqualTo(AuthenticationResult.NextStep.Unknown);
        assertThat(result.serviceId).isNull();
        assertThat(result.errorType).isNull();
    }

    @Test
    public void fromError() {
        Intent intent = new Intent().setData(Uri.parse("test://url?next_step=error&error_type=account_creation"));
        AuthenticationResult result = AuthenticationResult.fromIntent(intent);

        assertThat(result.nextStep).isEqualTo(AuthenticationResult.NextStep.Error);
        assertThat(result.serviceId).isNull();
        assertThat(result.errorType).isEqualTo("account_creation");
    }
}
