# Connect Location
Connect Location SDK is a library that provides native location functionality to connections using [IFTTT Location service](https://ifttt.com/location).

For better integration and user experience, you should use this library along with Connect Button SDK.

## Usage
### Setup Awareness API
Connect Location SDK uses Google [Awareness API](https://developers.google.com/awareness) to power geofence monitoring functionality. Before integrating with the SDK, you should configure your app to use Awareness SDK first.

Detailed instruction can be found [here](https://developers.google.com/awareness/android-api/get-started), you only need the following 4 steps:
* [Set up Google Play services](https://developers.google.com/awareness/android-api/get-started#set_up_google_play_services)
* [Update gradle dependencies](https://developers.google.com/awareness/android-api/get-started#update_gradle_dependencies)
* [Add your API key](https://developers.google.com/awareness/android-api/get-started#add_your_api_key)
* [Declare Android permissions in AndroidManifest.xml](https://developers.google.com/awareness/android-api/get-started#declare_android_permissions_in_androidmanifestxml) 

### Initialization
To set up the SDK, you should call
```java
CredentialProvider provider = new CredentialProvider() {
    // ...
};
String connectionId = "Location connection id";

ConnectLocation.init(context, connectionId, provider);

// If you want to use your own ConnectionApiClient instance
ConnectionApiClient client = ...;
ConnectLocation.init(context, connectionId, provider, client);
``` 
This method should be called as soon as you know the user in the application lifecycle. For example, you should call it in your Application class if you know your user has logged in when the app starts.

### Usage with ConnectButton
The Connect Location SDK provides a convenient method to set it up ConnectButton:
```java
ConnectLocation.getInstance().setupWithConnectButton(ConnectButton connectButton);
```
This method should be called **after** `ConnectLocation.init` is called.

