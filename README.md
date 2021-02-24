

# IFTTT Connect Button SDK for Android

[![Build Status](https://travis-ci.org/IFTTT/ConnectSDK-Android.svg?branch=master)](https://travis-ci.org/IFTTT/ConnectSDK-Android)

### Connect API references
[![javadoc](https://javadoc.io/badge2/com.ifttt/connect-api/javadoc.svg)](https://javadoc.io/doc/com.ifttt/connect-api)

### Connect Button API references
[![javadoc](https://javadoc.io/badge2/com.ifttt/connect-button/javadoc.svg)](https://javadoc.io/doc/com.ifttt/connect-button)

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
* [Native IFTTT Location service support.](https://github.com/IFTTT/ConnectSDK-Android/tree/master/connect-location/README.md).

## Dependencies
This SDK uses the following libraries as dependencies:
* [Retrofit v2.6.0](http://square.github.io/retrofit/)
* [OkHttp v3.14.2](http://square.github.io/okhttp/)
* [Moshi v1.8.0](https://github.com/square/moshi)
* [Android X](https://developer.android.com/topic/libraries/support-library/androidx-overview)
	* Chrome Custom Tabs
	* AppCompat

## Requirement
* Android SDK version 21 or higher.
* Java 8

## Installation
### Gradle
```groovy
// Required for Connect API integration.
implementation "com.ifttt:connect-api:2.4.0"
// Connect Button UI.
implementation "com.ifttt:connect-button:2.4.0"
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
                ConnectButton.Configuration.newBuilder("user_email", Uri.parse("redirect_uri"))
                    .withConnectionId("connection_id")
                    .withCredentialProvider(provider)  
                    .build();
        connectButton.setup(configuration);       
    }
}
```

For more information about the user token used in `CredentialProvider`, please see [Authentication](https://github.com/IFTTT/IFTTTSDK-Android-v2#Authentication) section.

### Render on dark background
If you want to render the ConnectButton on a dark background, call `connectButton.setOnDarkBackground(true)`.

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
To enable the SDK to retrieve connection status for a specific user, as well as allowing the SDK to facilitate disabling a connection, it needs to be user-authenticated, which requires an IFTTT user token. This token is identical to the token returned by your service when a user authenticates via the [service authentication flow](https://platform.ifttt.com/docs/api_reference#authentication-flow).

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
	* **[DEPRECATED]** valuePropositions: a list of value propositions associated with the Connection. This field is deprecated, please use the `features` list instead.
	* features: a list of feature information for the Connection. Each feature contains its ID, a user-facing title, description, and a URL for the feature icon.
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

There are two callback methods within the listener:
* `onStateChanged(ConnectButtonState currentState, ConnectButtonState previousState, Connection connection)`: We added a `connection` to this callback method, available starting from v7. If your app has registered this listener, please add the `connection` param in the overriden callback method. The older version of this method without the connection param is not available anymore.
* `onError(ErrorResponse errorResponse)`


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
* `user`: return the authenticated user.
* `userToken`: given the user's OAuth token to your service, plus your IFTTT Service key, return the IFTTT user token.

**Note:** When setting up a ConnectButton, if there is no ConnectionApiClient instance provided by the Configuration, a default one will be used. If your app manages user login, you should consider using your own ConnectApiClient instance, so that you can control the lifecycle of the authorization and tie it to the logged in user. 

### Tracking
In order to continually innovate and improve our SDK, IFTTT may collect certain usage statistics from the software including but not limited to an anonymized unique identifier, version number of the software, and user interactions with elements of the UI provided by the SDK. It is common practice, and your responsibility as a user of the IFTTT SDK, to inform your customers that they may opt-out of information collection. The instructions in this section explain how you can enable opt-out. When properly implemented by you, if consent is withheld, the information will not be collected.

The data collected is examined in the aggregate to improve the SDK and IFTTT’s associated services, and is maintained in accordance with IFTTT's [Privacy Policy](https://ifttt.com/terms).

#### Anonymous ID
By default, the SDK will track user interactions when users interact with the ConnectButton. In order to distinguish unique installs, we randomly generate a UUID per application installation (“anonymous id”), and send it along with the event requests.

#### Disable tracking
You may call the `ConnectButton.disableTracking(context)` if you wish to opt-out from tracking. After this method is called, all tracking will be disabled for all of the ConnectButton instances within the app **for as long as it is in-memory**. If you want to persist the user's preference for disabling tracking, you should store the preference within your persistent storage, and call this method every time the app is started.

### Configuration skipping
You can use `Configuration.Builder#skipConnectionConfiguration()` if you want to use your own connection configuration UI. Setting this parameter will instruct IFTTT to skip the connection configuration screen. Once a user clicks the connect button they will be taken through the usual connection flow however they will not see the connection configuration screen but will be redirected back to your app instead. After that you will be able to use the [field options endpoint](https://platform.ifttt.com/docs/connect_api#field-options) and the [update a connection endpoint](https://platform.ifttt.com/docs/connect_api#update-a-connection) to support your UI and allow the user to configure the connection. A user connection created with `skipConfig=true` is considered pending and will not fire it's triggers or allow you to run it's actions or queries until it's updated using the [update a connection endpoint](https://platform.ifttt.com/docs/connect_api#update-a-connection).

### Localization
The `ConnectButton` and the corresponding flow can display translated text.
No additional setup is required to display translated text, the SDK infers user locale from the `Context` within which the `ConnectButton` is initialized.
If no translations are found for the inferred locale, the default locale i.e.English will be used.
Text translation is supported for the following languages:
* English (Default)
* English - United Kingdom (en-rGB)
* Czech (cs)
* Danish (da)
* German (de)
* Spanish (es)
* Spanish - United States (es-rUS)
* Finnish (fi)
* French (fr)
* French - Canada (fr-rCA)
* Italian (it)
* Japanese (ja)
* Korean (ko)
* Norwegian-Bokmål (nb)
* Dutch (nl)
* Polish (pl)
* Portuguese - Brazil (pt-rBR)
* Portuguese - Portugal (pt-rPT)
* Russian (ru)
* Swedish (sv)
* Simplified Chinese (zh-rCN)
* Traditional Chinese (zh-rTW)

