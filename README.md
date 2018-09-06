

# IFTTT Connect Button SDK for Android
## Overview
IFTTT Connect Button SDK is a library that helps facilitate the integration of the IFTTT Connect Button and its APIs.

The SDK includes
* a lightweight wrapper around the IFTTT REST API, with predefined data structures and JSON adapters.
* IFTTT Connect Button UI representation.

## Dependencies
This SDK uses the following libraries as dependencies:
* [Retrofit v2.4.0](http://square.github.io/retrofit/)
* [OkHttp v3.11.0](http://square.github.io/okhttp/)
* [Moshi v1.6.0](https://github.com/square/moshi)
* [Android X](https://developer.android.com/topic/libraries/support-library/androidx-overview)
	* Chrome Custom Tabs
	* AppCompat

## Requirement
Android SDK version 19 or higher.

## Get started
To get started, after setting up the dependency, you can instantiate an `IftttApiClient` and add an `IftttConnectButton` to show your users the UI for an Applet. For example, in your Activity,

```java
IftttApiClient iftttApiClient = new IftttApiClient.Builder()  
        .setRedirectUri("your_redirect_uri")  
        .build()
IftttConnectButton connectButton = findViewById(R.id.ifttt_connect_button);
iftttApiClient.api().showApplet("applet-id")
    .execute(new PendingResult.ResultCallback<Applet>() {
        @Override
        public void onSuccess(Applet applet) {
            // Show Applets UI using IftttConnectButton.
            iftttConnectButton.setupWithIftttApiClient(iftttApiClient);
            iftttConnectButton.setApplet(applet);
        }

        @Override
        public void onFailure(ErrorResponse errorResponse) {
            // Show error UI.
        }
});
```

And in your layout xml file,
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout  
  android:layout_width="match_parent"  
  android:layout_height="match_parent"  
  android:orientation="vertical">
	<!-- Other views -->
	
	<com.ifttt.ui.IftttConnectButton  
	  android:id="@+id/ifttt_connect_button"  
	  android:layout_width="match_parent"  
	  android:layout_height="wrap_content"/>
	  
	<!-- Other views -->
</LinearLayout>
```

In addition, you can also use the `Applet` object returned from the API to render the UI around the IFTTT Connect Button, so that your users can see the full context of the Applet.


## IftttApiClient
`IftttApiClient` contains the IFTTT API wrappers, you'll be able to make API calls to the IFTTT API using this class to get structured Applet and Service data. To start, use the `IftttApiClient.Builder` to build an instance with the following methods:

* `setRedirectUri(String uri)`: The redirect URI string will be used for redirecting an Applet authentication back to your app. This is required if you are going to use `IftttConnectButton`.  This URI must match one of the redirect URIs that you specify on [IFTTT Platform](https://platform.ifttt.com/). You can set up the redirect URI under the "Service" tab. 
* `setInviteCode(String inviteCode)`:   The invite code is used to access preview services on IFTTT Platform. 

With the instance, you can use `IftttApiClient#api()` to get access to the APIs for Applet data.
```java
IftttApiClient iftttApiClient = new IftttApiClient.Builder()  
        .setRedirectUri("your_redirect_uri") // Required for setting up with IftttConnectButton.
        .setInviteCode("invite_code") // Optional, only needed if your service is not yet published.
        .build();

IftttApi iftttApi = iftttApiClient.api();
```

The APIs available are:
* `showApplet`: returns metadata for a single Applet.
* `disableApplet`: turn off an already authenticated Applet. User authentication level required.
* `user`: return the authenticated user.
* `userToken`: given the user's OAuth token or refresh token to your service, plus your IFTTT Service key, return the IFTTT user token.

## Authentication
The SDK supports two different types of API authentication: anonymous and user authentication. 

With anonymous authentication level, you will not be able to get Applet data about whether the user has authenticated a specific Applet, nor can you make the API call to turn on or turn off an Applet on behalf of the user. You will be able to retrieve Applet and Service data that is publicly available. For example, Applet name, description and Service brand color, etc.

To use user authentication level, you will need to retrieve an **IFTTT user token** from IFTTT API, and pass it to the `IftttApiClient` instance, before making any API calls. To do so,

```java
// Fetch IFTTT user token.
...

iftttApiClient.setUserToken(userToken);

// Make API calls.
...
```

After setting the user token, subsequent API calls will be user-specific:
* `IftttApi#showApplet` will return Applet status for the user, whether it is `never_enabled`, `enabled` or `disabled`.
* `IftttApi#disableApplet` can be used to turn off a user authenticated Applet. 

### Provide IFTTT user token through your backend server
If you choose to fetch the user token from your backend server and then pass it down to your app, you can do so via this REST API
```
POST /v1/services/{{service_id}}/user_token?user_id={{user_id}}
Host: api.ifttt.com
IFTTT-Service-Key: {{service_key}}
Content-Type: application/json

{
  "token": "{{token}}"
}
```
The request body must contains a `token`, which is **the OAuth access token or refresh token that you issued to IFTTT on behalf of this user** when they connected their IFTTT account to your service.

Once you have the user token, pass it to the `IftttApiClient#setUserToken` method, and all of the subsequent requests will be user authenticated.

### [DRAFT] Provide OAuth credential through SDK
The `IftttApiClient` also wraps the above API call in `IftttApi#userToken(String, String)`. Same as the API, you'll need to pass in the user's OAuth token or refresh token, as well as the IFTTT Service key to make the API call. We **do not recommend** you to include the IFTTT Service key in your APK, but instead pass it to your app via more secure ways. For example, you could have an endpoint from your backend server to return the IFTTT Service key to your app.

### Invite code
If your service has not yet been published on IFTTT, you should provide the invite code to the `IftttApiClient` to the SDK for full access to the IFTTT API. You can find your invite code under [**Invite URL** on the Service tab](https://platform.ifttt.com/mkt/general) on the IFTTT platform.

## Applet and Service data structure
The SDK provides the basic data structure for the data returned from the IFTTT API: Applet, Service and User. It also includes a few utility methods and tweaks for easier development. Some example fields that we provides are
* Applet:
	* id
	* title
	* description
	* status: Applet status. 
		* `never_enabled`: the Applet has never been authenticated by the user.
		* `enabled`: the Applet has been authenticated and is now enabled.
		* `disabled`: the Applet has been authenticated and is now disabled.
		* `unknown`: the IftttApiClient doesn't have the user token set, therefore cannot get the Applet status for the user. You should treat this status as `never_enabled`.
    * `getPrimaryService` : a method that returns the primary service for the Applet.
* Service: 
	* id
	* name
	* color icon URL
	* monochrome icon URL
	* brand color

## IFTTT Connect Button
`IftttConnectButton` is the UI representation of the IFTTT Connect Button experience. It provides methods to set up an Applet for authentication and configuration.

You will need to an `IftttApiClient` instance for the View for it to enable the Applet authentication and configuration features, it can be passed in to the View by calling `IftttConnectButton#setupWithIftttApiClient(IftttApiClient)`.

**Note**: a redirect URL is **required** in the `IftttApiClient` instance for it to be used with `IftttConnectButton`. You can set it up using `IftttApiClient.Builder#setRedirectUri`.

**[DRAFT]**
At this point, you should also pass the user's OAuth token to the Connect Button, so that we can use it to connect your service on IFTTT automatically on behalf of the user during the Applet authentication flow. This is to streamline the flow and reduce the steps it would take for users to successfully enable an Applet.


### Applet activation
If an Applet has a user state `State.unknown` or `State.never_enabled`, it can be authenticated through `IftttConnectButton`.  The View will open up a [Chrome Custom Tabs](https://developer.chrome.com/multidevice/android/customtabs) to show the Applet authentication flow on web.

### Communication between your app and ifttt.com
Because part of the Applet authentication will happen on ifttt.com, you will need to set up a deep link handler Activity in your app, in order for ifttt.com or the IFTTT app to redirect users back to your app once the authentication is completed.

To do so, you can set up an Activity following this [documentation](https://developer.android.com/training/app-links/deep-linking), with the host and path matching the redirect URI you pass in when initializing the `IftttApiClient`.  When your activity receives a deep linking intent, use `AuthenticationResult#fromIntent(Intent)` method to extract the result, and pass it in to the `IftttConnectButton#setAuthenticationResult(AuthenticationResult)`. 

Because `IftttConnectButton` holds the authentication states, we recommend you to use the same Activity that contains the view as your deep link handler Activity, so that you can refer to the same View when the redirect happens. To do this, set up the Manifest as follow:
```xml
<activity  
  android:name=".YourActivity"  
  android:exported="true"  
  android:launchMode="singleTop">
  <!-- IntentFilters and other Activity setup -->
</activity>
```
and override the `Activity#onNewIntent(Intent)` method to capture the deep link result, and then pass it in to the view:
```java
public final class YourActivity extends Activity {

	private IftttConnectButton iftttConnectButton;
	
	@Override  
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		
		// Activity implementations
		...
		
		iftttConnectButton = findViewById(R.id.ifttt_connect_button);
		
		...
	}
	
	@Override  
	protected void onNewIntent(Intent intent) {  
		AuthenticationResult result = AuthenticationResult.fromIntent(intent);  
		iftttConnectButton.setAuthenticationResult(result);  
	}
}	
```

### Complete Applet authentication
When an `AuthenticationResult#nextStep` is `Complete`, it means that the Applet authentication has done, and the Applet is now enabled. 

However, at this point, the `Applet` object that the View has is still in its original status. If the IftttApiClient doesn't have user token set, you won't be able to allow your users to turn on or off the Applet. To enable this feature, you should fetch the user token through the SDK or your backend server, and refresh the View with a new Applet object.

To do that, 
```java
public final class YourActivity extends Activity {
	
	...
	
	@Override  
	protected void onNewIntent(Intent intent) {  
		AuthenticationResult result = AuthenticationResult.fromIntent(intent);
		iftttConnectButton.setAuthenticationResult(result);
		if (result.nextStep == AuthenticationResult.NextStep.Complete) {
			// Fetch user token for this user.
			...
			iftttApiClient.setUserToken(userToken);
			// Refresh IftttConnectButton
			iftttApiClient.api().showApplet("applet_id").execute(new ResultCallback<Applet>() {
				@Override  
				public void onSuccess(Applet applet) {  
				    iftttConnectButton.setApplet(applet);
				}
				...
			})
		}
	}
}	
```
