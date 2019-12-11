

# IFTTT Connect Button SDK for Android

[![Build Status](https://travis-ci.org/IFTTT/ConnectSDK-Android.svg?branch=master)](https://travis-ci.org/IFTTT/ConnectSDK-Android)

IFTTT Connect Button SDK is a library that helps facilitate the integration of the Connect Button and [Connect API](https://platform.ifttt.com/docs/connect_api).

* [Features](https://github.com/IFTTT/IFTTTSDK-Android-v2#Features)
* [Dependencies](https://github.com/IFTTT/IFTTTSDK-Android-v2#Dependencies)
* [Requirements](https://github.com/IFTTT/IFTTTSDK-Android-v2#Requirements)
* [Installation](https://github.com/IFTTT/IFTTTSDK-Android-v2#Installation)
* [Usage](https://github.com/IFTTT/IFTTTSDK-Android-v2#Usage)
* [Authentication](https://github.com/IFTTT/IFTTTSDK-Android-v2#Authentication)
* [Advanced](https://github.com/IFTTT/IFTTTSDK-Android-v2#Advanced)

## Features
* A lightweight wrapper around the IFTTT Connection API, with predefined data structures and JSON adapters.
* Connect Button UI representation.

## Dependencies
This SDK uses the following libraries as dependencies:
* [Retrofit v2.6.0](http://square.github.io/retrofit/)
* [OkHttp v3.14.2](http://square.github.io/okhttp/)
* [Moshi v1.8.0](https://github.com/square/moshi)
* [Android X](https://developer.android.com/topic/libraries/support-library/androidx-overview)
	* Chrome Custom Tabs
	* AppCompat

## Requirement
* Android SDK version 18 or higher.
* Java 8

## Installation
### Gradle
```groovy
implementation "com.ifttt:connect-button:2.0.2"
```

## Usage
### Set up ConnectButton
To get started, after setting up the dependency, add a `ConnectButton` to your layout and set it up with a `Configuration`. For example, in your layout xml file,

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout  
  android:layout_width="match_parent"  
  android:layout_height="match_parent"  
  android:orientation="vertical">
	<!-- Other views -->
	
	<com.ifttt.connect.ui.ConnectButton  
	  android:id="@+id/connect_button"  
	  android:layout_width="match_parent"  
	  android:layout_height="wrap_content"/>
	  
	<!-- Other views -->
</LinearLayout>
```

and In your Activity,
```java
public class YourActivity extends Activity {
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) { 
        ConnectButton connectButton = findViewById(R.id.connect_button);  
        CredentialsProvider provider = new CredentialsProvider() {  
            @Override  
          public String getOAuthCode() {
                // Your user's OAuth code, this will be used to authenticate the user to IFTTT.
                return "user_oauth_code";  
          }  
          
            @Override  
          public String getUserToken() {  
                return "ifttt_user_token";  
          }  
        };
        
        ConnectButton.Configuration configuration =  
                ConnectButton.Configuration.Builder.withConnectionId("connection_id", "user_email", provider,  
          Uri.parse("redirect_url")).build();
        connectButton.setup(configuration);       
    }
}
```

For more information about the user token used in `CredentialProvider`, please see [Authentication](https://github.com/IFTTT/IFTTTSDK-Android-v2#Authentication) section.

### Listen to Connection status
ConnectButton helps initiate connection enable flows for users, which involve opening web views within your app. Currently, we are using [Chrome Custom Tabs](https://developer.chrome.com/multidevice/android/customtabs) for the web views. 

During the flow, the web view will redirect back to your app using the Uri that you pass in to the `Configuration` object when setting up the ConnectButton. To set up the redirect, in your AndroidManifest.xml, 

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask"> <!-- Recommended launch mode for the activity, so that we can refresh the same ConnectButton UI after redirect. -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
    </intent-filter>

    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>

        <!-- IMPORTANT: make sure the scheme and host matches the <redirect_uri> parameter passed in to ConnectButton.Configuration -->
        <data
            android:host="redirect_host"
            android:scheme="redirect_scheme"/>
    </intent-filter>
</activity>
``` 

and in your Activity,  override `onNewIntent` method, 

```java
public class YourActivity extends Activity {

    @Override  
    protected void onNewIntent(Intent intent) {  
        super.onNewIntent(intent);  
      
        ConnectResult connectResult = ConnectResult.fromIntent(intent);  
        connectButton.setConnectResult(connectResult);  
    }       
}
```

At this point, the ConnectButton is set up to display the connection status for a given user, as well as initiating a connection flow.

## Authentication
To enable the SDK to retrieve connection status for a specific user, as well as allowing the SDK to facilitate disabling a connection, it needs to be user-authenticated, which requires an IFTTT user token.

A user-authenticated request is one that includes an `Authorization` header containing a user-specific token that IFTTT has issued to your service. This approach lets you make calls to the API from places like mobile apps or browsers where it would be inappropriate to expose your service key.

### Exchange a user token
**URL**: `POST https://connect.ifttt.com/v2/user_token`

This endpoint can be used to obtain a token for a specific user, allowing you to make user-authenticated requests. 

##### Example: Get a user token, Service-authenticated with user ID and OAuth token
<div class="example-list">
  <ul>
    <li>
      <span class="example-list-heading">HTTP Request</span>
      <code>
<pre>
POST /v2/user_token?user_id=123&access_token=abc
Host: connect.ifttt.com
IFTTT-Service-Key: 6e7c8978c07a3b5918a237b9b5b1bb70
Content-Type: application/json
</pre>
      </code>
    </li>
    <li>
      <span class="example-list-heading">Response</span>
      <code>
<pre>
{
  "type": "user_token",
  "user_token": "e1hMBWw44mJM902c6ye9mmuS3nd4A_8eTCU99D4a5KQW7cT1"
}
</pre>
      </code>
    </li>
  </ul>
</div>

To clarify the variables used in this example:

|Variable|Value|Details|
|--------|-----|-------|
| `user_id` | `123` | The id of the user on your service, which must match the id provided by your [User information] endpoint |
| `access_token` | `abc` | The OAuth access token that you issued to IFTTT on behalf of the user when they connected their IFTTT account to your service |
| `IFTTT-Service-Key` | `6e7...` |  Your secret service key |
| `user_token` | `e1h...` | The new user token you'll use to make requests to the IFTTT API on behalf of the IFTTT user |

Within these parameters,
* You can find the `IFTTT-Service-Key` in the [API tab](https://platform.ifttt.com/mkt/api) of the IFTTT Platform under the Service Key heading. You can use this approach when you’re making calls from your backend servers to the API.
* `access_token` is **the OAuth access token that you issued to IFTTT on behalf of this user** when they connected their IFTTT account to your service. This lets us verify the request more stringently than if you just provided your service key, without making the user go through a redundant OAuth flow.

### Important note about exchanging user token
Your IFTTT service key should be kept secret at all time. The service key can be used to make calls on behalf of any user, but a user token is limited to a single user. This makes user tokens much less sensitive. On the other hand, you’d never want to embed your service key into a mobile app because it could be read by end users. A similar concept is the AWS S3 credentials, you can find more details from [Google Play FAQs](https://support.google.com/faqs/answer/6032655?hl=en).

Because of this, **we strongly encourage you to** call this API on your backend, and return the user token back to your application, instead of making the API call directly within your application.


### Integrate with SDK
To integrate the user token exchange process with your backend API and the SDK, you need to set up your [CredentialProvider](https://github.com/IFTTT/IFTTTSDK-Android-v2#set-up-connectbutton) so that the `CredentialProvider#getUserToken` will return the user token when the SDK needs it. 

**Note:** the `getUserToken` method will be called on a background thread.

## Advanced
This section describes some key components that are used in the SDK, and can be used separately to facilitate the integration.

### Connection and Service data structure
The SDK provides the basic data structure for the data returned from the Connection API: Connection, Service and User. It also includes a few utility methods and tweaks for easier development. Some example fields that we provide are
* Connection:
	* id
	* title
	* description
	* status:
		* `never_enabled`: the Connection has never been authenticated by the user.
		* `enabled`: the Connection is enabled.
		* `disabled`: the Connection is disabled.
		* `unknown`: the ConnectionApiClient doesn't have the user token set, therefore cannot get the Connection status for the user. You should treat this status as `never_enabled`.
	* coverImage: a set of image URLs for the cover image associated with the Connection in different dimensions.
	* valuePropositions: a list of value propositions associated with the Connection.
    * `getPrimaryService` : a method that returns the primary service for the Connection.
* Service: 
	* id
	* name
	* color icon URL
	* monochrome icon URL
	* brand color
	
### ConnectButton

<img width="469" alt="connect_button_screenshot" src="https://user-images.githubusercontent.com/1761573/56773012-45b58480-6771-11e9-9958-b0965f5518a3.png">

#### Button state change listeners
In case your app wants to listen to the button state changes, you can register a `ButtonStateChangeListener` via `ConnectButton#addButtonStateChangeListener`.

To unregister the listener, call `ConnectButton#removeButtonStateChangeListener`.

#### Layout limitation.
Because the View is designed to be used to occupy the screen width, `ConnectButton` has a minimum width of 300dp and a maximum width 330dp. Please make sure to give enough space for the View on your UI. Between the minimum and the maximum width, the View will try to keep a set start and end margin on the button view.    

### ConnectionApiClient
`ConnectionApiClient` is the IFTTT Connection API wrapper, it can be used to make API calls with pre-defined data structures mentioned above. To start, use the `ConnectionApiClient.Builder` to get an instance with the following methods:

* `setInviteCode(String inviteCode)`:   The invite code is used to access preview services on IFTTT Platform. 

With the instance, you can use `ConnectionApiClient#api()` to get access to the APIs for Connection data.
```java
ConnectionApiClient apiClient = new ConnectionApiClient.Builder()  
        .setInviteCode("invite_code") // Optional, only needed if your service is not yet published on IFTTT Platform.
        .build();

ConnectionApi api = apiClient.api();
```

The APIs available are:
* `showConnection`: returns metadata for a Connection.
* `disableConnection`: disable a Connection. User authentication level required.
* `user`: return the authenticated user.
* `userToken`: given the user's OAuth token to your service, plus your IFTTT Service key, return the IFTTT user token.

**Note:** When setting up a ConnectButton, if there is no ConnectionApiClient instance provided by the Configuration, a default one will be used.

### Authentication with IftttApiClient

To setup authentication for an IftttApiClient instance, you will need to retrieve an [IFTTT user token](https://github.com/IFTTT/IFTTTSDK-Android-v2#Authentication), and call

```java
// Fetch IFTTT user token.
...

connectionApiClient.setUserToken(userToken);

// Make API calls.
...
```

After that, subsequent API calls will be user-authenticated:
* `ConnectionApi#showConnection` will return Connection status for the user, whether it is `never_enabled`, `enabled` or `disabled`.
* `ConnectionApi#disableConnection` can be used to disable a Connection. 
