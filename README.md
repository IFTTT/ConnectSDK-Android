# IFTTT Android SDK
## Overview
IFTTT Android SDK is a library that helps facilitate the integration of the IFTTT API. you can find the documentation of the API [here](https://platform.ifttt.com/docs/embedding_applets).

The SDK is designed to be a lightweight wrapper around the REST API, with predefined data structures and JSON adapters to help developers better use the API in their apps.

## Dependencies
This SDK uses the following libraries as dependencies:
* [Retrofit v2.3.0](http://square.github.io/retrofit/)
* [OkHttp v3.9.0](http://square.github.io/okhttp/)
* [Moshi v1.5.0](https://github.com/square/moshi)
* [Android Support Library Annotations v26.1.0](https://developer.android.com/topic/libraries/support-library/packages.html#annotations)

## Requirement
Android SDK version 15 or higher.

## Download
You can download the latest AAR [here](https://bintray.com/ifttt/maven/download_file?file_path=com%2Fifttt%2Fifttt-sdk-android%2F0.0.1%2Fifttt-sdk-android-0.0.1.aar) or via Gradle:
```groovy
implementation 'com.ifttt:ifttt-sdk-android:0.0.1'
```

## Example app
There is an example app in this repository to demonstrate how to use the SDK and your backend to integrate the IFTTT API on Android. You can find the example app [here](https://github.com/IFTTT/IFTTTSDK-Android/tree/master/app).


## Get started
### IftttApiClient
`IftttApiClient` is the main entrance point to access to the SDK. In your app, call `IftttApiClient.getInstance()` to get the singleton instance of the class. Then you can call the following methods for different APIs:
* `appletsApi()`: APIs for listing Applets or a single Applet from a service.
* `appletConfigApi()`: APIs for enabling and disabling an Applet.
* `userApi()`: API for retrieving IFTTT service and account information for the authenticated user.

For example, to get a list of Applets for your service:
```java
AppletsApi appletsApi = IftttApiClient.getInstance().appletsApi();
appletsApi.listApplets("your-service-id", AppletsApi.Platform.android, AppletsApi.Order.enabled_count_asc)
    .execute(new PendingResult.ResultCallback<List<Applet>>() {
        @Override
        public void onSuccess(List<Applet> applets) {
            // Show Applets UI.
        }

        @Override
        public void onFailure(ErrorResponse errorResponse) {
            // Show error UI.
        }
});
```

## Key components
### Data structure
The SDK provides the basic data structure for the data returned from the IFTTT API: Applet, Service and User. It also includes a few utility methods and tweaks for easier development. For more details of the data structure, please see [the API documentation](https://platform.ifttt.com/docs/embedding_applets#list-applets).
* Applet:
    * `getPrimaryService` method that returns the primary service for the Applet.
    * `getEmbedUri` method returns a URI object for opening the Applet activation web UI.
* Service: the `brandColor` field is converted from the string representation to the integer representation of the color value.

### Authentication
Some operations can be done without user authentication, such as fetching your service’s Applets. However, to get user-specific status (enabled vs. disabled), or to change an activated Applet’s status, you need to provide the IFTTT SDK with an IFTTT user token. You should fetch such a token on your backend service using the ["Get a user token" API](https://platform.ifttt.com/docs/embedding_applets#get-a-user-token). See [here](https://github.com/IFTTT/ifttt-api-example/blob/master/controllers/mobile_api_controller.rb#L39) for the example service’s implementation.

Once you have the user token, call `IftttApiClient#setUserToken` method to attach it to the instance, and all subsequent API calls will be authenticated with the user token.

Some behavior changes to the API when there is no user token presented:
* `AppletsApi#listApplets` and `AppletsApi#showApplet` will return Applet data with `user_status = unknown`.
* `AppletConfigApi#enableApplet` and `AppletConfigApi#disableApplet` will not work without user authentication or if the Applet’s current `user_status` is `never_enabled`.
* `UserApi#user` will return a User object with `authenticationLevel = none` and without `service_id` or `user_login`.

To clear the user token, call `IftttApiClient#clearUserToken` method.

### Invite code
If your service has not yet been published on IFTTT, you should provide the invite code to the `IftttApiClient` to the SDK for full access to the IFTTT API. You can find your invite code under [**Invite URL** on the Service tab](https://platform.ifttt.com/mkt/general) on the IFTTT platform.

Once you have the invite code, call the `IftttApiClient#setInviteCode` method to attach it to the instance, and all of the API requests will have it as a header.

To clear the invite code, call the `IftttApiClient#clearInviteCode` method.

### Applet activation
If an Applet has user state `State.unknown` or `State.never_enabled`, it can be activated through IFTTT web UI. For this, we strongly recommend using [Chrome Custom Tabs](https://developer.chrome.com/multidevice/android/customtabs) to provide the best experience to your users. You can check out the [example app](https://github.com/IFTTT/IFTTTSDK-Android/blob/master/app/src/main/java/com/ifttt/api/demo/AppletConfigurationClickListenerFactory.kt) to see details about how to use it.

`Applet#getEmbedUri` constructs a URI object that can be used in web view or browser to start the Applet activation flow with the following parameters:
* `redirectUri` (required) is the URL that should be opened and return users to your app once the Applet activation process is complete. It must be [registered on the IFTTT platform](https://platform.ifttt.com/services/ifttt_api_example/embedded_redirects).
* `user_id` (required)  is the user id of the current user on your service. This will help us ensure that the user is logged into the correct IFTTT account and streamline the process of logging into or signing up for IFTTT.
* `email` (optional but encouraged) is the email address of the current user. If you include it, we can try to streamline the process of logging into or signing up for IFTTT by looking for a matching account or prefilling the signup form. We won’t store the address unless the user signs up with it.
* `inviteCode` (optional) should be included if your service is not published yet.

**IMPORTANT**: We don’t encourage you to open this URL directly. Instead, you should generate (likely in cooperation with your backend service) a URL that will sign in the user to your service in the web session so that connecting your service is simple once they create an IFTTT account.

For example, the service used by the example app has [an authenticated API endpoint](https://github.com/IFTTT/ifttt-api-example/blob/master/controllers/mobile_api_controller.rb#L74-L81) that accepts a `redirect_to` as a URL. It returns a URL that, if opened, sets a session cookie to login the current user, then immediately redirects (with a 302) to the URL specified by `redirect_to`. The example app then opens this URL. When it’s time to connect your service to IFTTT, the user does not then need to sign into their account, but is instead sent straight to the OAuth authorization page.
