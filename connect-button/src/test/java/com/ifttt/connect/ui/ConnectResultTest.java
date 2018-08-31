package com.ifttt.connect.ui;

import android.content.Intent;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public final class ConnectResultTest {

    @Test
    public void fromInvalidServiceConnection() {
        Intent intent = new Intent().setData(Uri.parse("test://url?next_step=service_connection"));
        ConnectResult result = ConnectResult.fromIntent(intent);

        assertThat(result.nextStep).isEqualTo(ConnectResult.NextStep.Unknown);
        assertThat(result.errorType).isNull();
    }

    @Test
    public void fromComplete() {
        Intent intent = new Intent().setData(Uri.parse("test://url?next_step=complete"));
        ConnectResult result = ConnectResult.fromIntent(intent);

        assertThat(result.nextStep).isEqualTo(ConnectResult.NextStep.Complete);
        assertThat(result.errorType).isNull();
    }

    @Test
    public void fromUnknownState() {
        Intent intent = new Intent().setData(Uri.parse("test://url?"));
        ConnectResult result = ConnectResult.fromIntent(intent);

        assertThat(result.nextStep).isEqualTo(ConnectResult.NextStep.Unknown);
        assertThat(result.errorType).isNull();
    }

    @Test
    public void fromError() {
        Intent intent = new Intent().setData(Uri.parse("test://url?next_step=error&error_type=account_creation"));
        ConnectResult result = ConnectResult.fromIntent(intent);

        assertThat(result.nextStep).isEqualTo(ConnectResult.NextStep.Error);
        assertThat(result.errorType).isEqualTo("account_creation");
    }
}
