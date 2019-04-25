

# IFTTT Connect Button SDK for Android

IFTTT Connect Button SDK is a library that helps facilitate the integration of the Connect Button and [Connection API](https://platform.ifttt.com/docs/connection_api).

* [Features](https://github.com/IFTTT/IFTTTSDK-Android-v2#Features)
* [Dependencies](https://github.com/IFTTT/IFTTTSDK-Android-v2#Dependencies)
* [Requirements](https://github.com/IFTTT/IFTTTSDK-Android-v2#Requirements)
* [Installation](https://github.com/IFTTT/IFTTTSDK-Android-v2#Installation)
* [Usage](https://github.com/IFTTT/IFTTTSDK-Android-v2#Usage)
* [Advanced](https://github.com/IFTTT/IFTTTSDK-Android-v2#Advanced)

## Features
* A lightweight wrapper around the IFTTT Connection API, with predefined data structures and JSON adapters.
* Connect Button UI representation.

<img width="494" alt="connect_button_screenshot" src="https://user-images.githubusercontent.com/1761573/56772956-fec78f00-6770-11e9-91ca-4e875a2df5c0.png">

## Dependencies
This SDK uses the following libraries as dependencies:
* [Retrofit v2.4.0](http://square.github.io/retrofit/)
* [OkHttp v3.11.0](http://square.github.io/okhttp/)
* [Moshi v1.6.0](https://github.com/square/moshi)
* [Android X](https://developer.android.com/topic/libraries/support-library/androidx-overview)
	* Chrome Custom Tabs
	* AppCompat

## Requirement
* Android SDK version 18 or higher.
* Java 8

## Installation
### Gradle
TODO

### AAR
```groovy
implementation(name: 'connect-button-release', ext: 'aar')
implementation 'com.squareup.okhttp3:okhttp:3.11.0'
implementation 'com.squareup.retrofit2:retrofit:2.4.0'
implementation 'com.squareup.retrofit2:converter-moshi:2.4.0'
implementation 'com.squareup.moshi:moshi:1.6.0'
implementation 'com.squareup.moshi:moshi-adapters:1.6.0'
implementation 'androidx.browser:browser:1.0.0'
```
Including the library as an aar file doesn't come with any transitive dependencies, you will need to include them manually.

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
	
	<com.ifttt.ui.ConnectButton  
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

For more information about the user token used in `CredentialProvider`, please see [Advanced](https://github.com/IFTTT/IFTTTSDK-Android-v2#Advanced) authentication section.

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

### Authentication
The SDK supports two different types of API authentication: anonymous and user authentication. 

With anonymous authentication level, you will not be able to get Connection data about whether the user has a specific Connection enabled, nor can you make the API call to disable a Connection on behalf of the user. You will be able to retrieve Connection data that is publicly available. For example, Connection name, description and Service brand color, etc.

To use user authentication level, you will need to retrieve an **IFTTT user token** from IFTTT API, and pass it to the `ConnectionApiClient` instance, before making any API calls. To do so,

```java
// Fetch IFTTT user token.
...

connectionApiClient.setUserToken(userToken);

// Make API calls.
...
```

After setting the user token, subsequent API calls will be user-specific:
* `ConnectionApi#showConnection` will return Connection status for the user, whether it is `never_enabled`, `enabled` or `disabled`.
* `ConnectionApi#disableConnection` can be used to disable a Connection. 

**Note:** by default, ConnectButton handles the user authentication automatically with the help of a `CredentialProvider`. 
