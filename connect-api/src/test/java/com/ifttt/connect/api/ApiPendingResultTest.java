package com.ifttt.connect.api;

import com.squareup.moshi.Moshi;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import retrofit2.mock.Calls;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

@RunWith(JUnit4.class)
public final class ApiPendingResultTest {

    @Test
    public void testExecution() {
        ApiPendingResult<Connection> pendingResult = new ApiPendingResult<>(Calls.<Connection>failure(new IOException()),
                new Moshi.Builder().build().adapter(ErrorResponse.class));
        final AtomicReference<ErrorResponse> errorResponseAtomicReference = new AtomicReference<>();
        pendingResult.execute(new PendingResult.ResultCallback<Connection>() {
            @Override
            public void onSuccess(Connection result) {
                fail();
            }

            @Override
            public void onFailure(ErrorResponse errorResponse) {
                errorResponseAtomicReference.set(errorResponse);
            }
        });

        assertThat(errorResponseAtomicReference.get().code).isEqualTo("exception");
        assertThat(errorResponseAtomicReference.get().message).isEqualTo("Unexpected error");
    }
}
