# Connect Button
IFTTT Connect Button SDK is a library that helps facilitate the integration of the Connect Button and [Connect API](https://platform.ifttt.com/docs/connect_api). 

## Set up ConnectButton
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

## Advanced
This section describes some key components that are used in the SDK, and can be used separately to facilitate the integration.

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

### Tracking
In order to continually innovate and improve our SDK, IFTTT may collect certain usage statistics from the software including but not limited to an anonymized unique identifier, version number of the software, and user interactions with elements of the UI provided by the SDK. It is common practice, and your responsibility as a user of the IFTTT SDK, to inform your customers that they may opt-out of information collection. The instructions in this section explain how you can enable opt-out. When properly implemented by you, if consent is withheld, the information will not be collected.

The data collected is examined in the aggregate to improve the SDK and IFTTT’s associated services, and is maintained in accordance with IFTTT's [Privacy Policy](https://ifttt.com/terms).

#### Anonymous ID
By default, the SDK will track user interactions when users interact with the ConnectButton. In order to distinguish unique installs, we randomly generate a UUID per application installation (“anonymous id”), and send it along with the event requests.

#### Disable tracking
You may call the `ConnectButton.disableTracking(context)` if you wish to opt-out from tracking. After this method is called, all tracking will be disabled for all of the ConnectButton instances within the app **for as long as it is in-memory**. If you want to persist the user's preference for disabling tracking, you should store the preference within your persistent storage, and call this method every time the app is started.
