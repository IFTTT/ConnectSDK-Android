

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
To get started, after setting up the dependency, you can instantiate an `IftttApiClient` and add an `IftttConnectButton` to show your users the UI for a Connection. For example, in your Activity,

```java
IftttApiClient iftttApiClient = new IftttApiClient.Builder().build()
OAuthCodeProvider provider = new OAuthCodeProvider() {
	@Override
	String getOAuthCode() {
		return "user_oauth_code";
	}
};

IftttConnectButton connectButton = findViewById(R.id.ifttt_connect_button);
iftttApiClient.api().showConnection("id")
    .execute(new PendingResult.ResultCallback<Connection>() {
        @Override
        public void onSuccess(Connection connection) {
            // Show Connection UI using IftttConnectButton.
            iftttConnectButton.setup("user_email", iftttApiClient, "redirect_uri", provider);
            iftttConnectButton.setConnection(connection);
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

In addition, you can also use the `Connection` object returned from the API to render the UI around the IFTTT Connect Button, so that your users can see the full context of the Connection.


## IftttApiClient
`IftttApiClient` contains the IFTTT API wrappers, you'll be able to make API calls to the IFTTT API. To start, use the `IftttApiClient.Builder` to build an instance with the following methods:

* `setInviteCode(String inviteCode)`:   The invite code is used to access preview services on IFTTT Platform. 

With the instance, you can use `IftttApiClient#api()` to get access to the APIs for Connection data.
```java
IftttApiClient iftttApiClient = new IftttApiClient.Builder()  
        .setInviteCode("invite_code") // Optional, only needed if your service is not yet published.
        .build();

IftttApi iftttApi = iftttApiClient.api();
```

The APIs available are:
* `showConnection`: returns metadata for a Connection.
* `disableConnection`: disable a Connection. User authentication level required.
* `user`: return the authenticated user.
* `userToken`: given the user's OAuth token to your service, plus your IFTTT Service key, return the IFTTT user token.

For more details about authentication level in IFTTT API and the SDK, please go to the following Authentication section.

## Authentication
The SDK supports two different types of API authentication: anonymous and user authentication. 

With anonymous authentication level, you will not be able to get Connection data about whether the user has a specific Connection enabled, nor can you make the API call to disable a Connection on behalf of the user. You will be able to retrieve Connection data that is publicly available. For example, Connection name, description and Service brand color, etc.

To use user authentication level, you will need to retrieve an **IFTTT user token** from IFTTT API, and pass it to the `IftttApiClient` instance, before making any API calls. To do so,

```java
// Fetch IFTTT user token.
...

iftttApiClient.setUserToken(userToken);

// Make API calls.
...
```

After setting the user token, subsequent API calls will be user-specific:
* `IftttApi#showConnection` will return Connection status for the user, whether it is `never_enabled`, `enabled` or `disabled`.
* `IftttApi#disableConnection` can be used to disable a Connection. 

### Provide IFTTT user token through your backend server
If you choose to fetch the user token from your backend server and then pass it down to your app, you can do so via this REST API
```
POST /v2/user_token?user_id={{user_id}}
Host: api.ifttt.com
IFTTT-Service-Key: {{service_key}}
Content-Type: application/json

{
  "token": "{{token}}"
}
```
The request body must contain a `token`, which is **the OAuth access token that you issued to IFTTT on behalf of this user** when they connected their IFTTT account to your service.

Once you have the user token, pass it to the `IftttApiClient#setUserToken` method, and all of the subsequent requests will be user authenticated.

### Provide OAuth credential through SDK
The `IftttApiClient` also wraps the above API call in `IftttApi#userToken(String, String)`. Same as the API, you'll need to pass in the user's OAuth token, as well as the IFTTT Service key to make the API call. We **do not recommend** you to include the IFTTT Service key in your APK, but instead pass it to your app via more secure ways. For example, you could have an endpoint from your backend server to return the IFTTT Service key to your app.

### Invite code
If your service has not yet been published on IFTTT, you should provide the invite code to the `IftttApiClient` to the SDK for full access to the IFTTT API. You can find your invite code under [**Invite URL** on the Service tab](https://platform.ifttt.com/mkt/general) on the IFTTT platform.

## Connection and Service data structure
The SDK provides the basic data structure for the data returned from the IFTTT API: Connection, Service and User. It also includes a few utility methods and tweaks for easier development. Some example fields that we provide are
* Connection:
	* id
	* title
	* description
	* status:
		* `never_enabled`: the Connection has never been authenticated by the user.
		* `enabled`: the Connection is enabled.
		* `disabled`: the Connection is disabled.
		* `unknown`: the IftttApiClient doesn't have the user token set, therefore cannot get the Connection status for the user. You should treat this status as `never_enabled`.
    * `getPrimaryService` : a method that returns the primary service for the Connection.
* Service: 
	* id
	* name
	* color icon URL
	* monochrome icon URL
	* brand color

## IFTTT Connect Button
`IftttConnectButton` is the UI representation of the IFTTT Connect Button experience.

To set up the Connect Button, you will need to call `IftttConnectButton#setup` method with
* `email`: An email string that represents the user account in your service. The email address is going to be used to try to match an existing IFTTT account. If there is no IFTTT account found, when the user starts enabling the Connection, a new IFTTT account will be automatically created using this email address.
* `iftttApiClient`: an IftttApiClient instance. This is used to make API calls to support disabling a Connection within the Connect Button.
* `redirectUri`: a URI string that matches your deep link Activity's manifest intent filter. This is necessary for the Connect Button to work, in order for the in-app browser to redirect users back to your app during the enable flow. This URI must match one of the redirect URIs that you specify on [IFTTT Platform](https://platform.ifttt.com/). You can set up the redirect URI under the "Service" tab. 
* `oAuthCodeProvider`: an Interface that you will need to implement, in order to provide the user's OAuth code when the user starts enabling a Connection. This is a necessary component that helps remove the step of service authentication to your service on IFTTT, to provide a seamless experience to users.

### Enable a Connection
If a Connection has a user state `State.unknown` or `State.never_enabled`, it can be enabled through `IftttConnectButton`.  The View will open up a [Chrome Custom Tabs](https://developer.chrome.com/multidevice/android/customtabs) to show the enable flow on web.

### Communication between your app and ifttt.com
Because part of the Connection enable flow happens on ifttt.com, you will need to set up a deep link handler Activity in your app, in order for ifttt.com to redirect users back to your app.

To do so, you can set up an Activity following this [documentation](https://developer.android.com/training/app-links/deep-linking), with the host and path matching the redirect URI you pass in when setting up the Connect Button.  When your activity receives a deep linking intent, use `ConnectResult#fromIntent(Intent)` method to extract the result, and pass it in to the `IftttConnectButton#setConnectResult(ConnectResult)`. 

Because `IftttConnectButton` holds the states for the Connection enable flow, we recommend you to use the same Activity that contains the view as your deep link handler Activity, so that you can refer to the same View when the redirect happens. To do this, set up the Manifest as follow:
```xml
<activity  
  android:name=".YourActivity"  
  android:exported="true"  
  android:launchMode="singleTask">
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
		ConnectResult result = ConnectResult.fromIntent(intent);  
		iftttConnectButton.setConnectResult(result);  
	}
}	
```

### Complete Connection enable flow
When an `ConnectResult#nextStep` is `Complete`, it means that the Connection is now enabled.

However, at this point, the `Connection` object that the View has is still in its original status. If the IftttApiClient doesn't have user token set, you won't be able retrieve the user status for the Connection. To do that, you should fetch the user token through the SDK or your backend server, fetch the Connection data again, and pass that to the Connect Button.

Here is an example,
```java
public final class YourActivity extends Activity {
	
	...
	
	@Override  
	protected void onNewIntent(Intent intent) {  
		ConnectResult result = ConnectResult.fromIntent(intent);
		iftttConnectButton.setConnectResult(result);
		if (result.nextStep == ConnectResult.NextStep.Complete) {
			// Fetch user token for this user.
			...
			iftttApiClient.setUserToken(userToken);
			// Refresh IftttConnectButton
			iftttApiClient.api().showConnection("id").execute(new ResultCallback<Connection>() {
				@Override  
				public void onSuccess(Connection connection) {  
				    iftttConnectButton.setConnection(connection);
				}
				...
			})
		}
	}
}	
```
