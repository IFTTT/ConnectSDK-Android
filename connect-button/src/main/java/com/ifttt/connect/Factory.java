package com.ifttt.connect;

/**
 * Returns an interceptor that adds common headers in the API call to IFTTT API.
 */
public class Factory {

    public static SdkInfoInterceptor getApiInterceptor(String anonymousId) {
        return new SdkInfoInterceptor(anonymousId);
    }
}
