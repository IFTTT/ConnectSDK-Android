# IFTTT Connect Location SDK for Android

IFTTT Connect Location SDK is an add-on library to the [ConnectButton SDK](https://github.com/IFTTT/ConnectSDK-Android/blob/master/README.md), it provides native geo-fencing functionality (through [Google Awareness API](https://developers.google.com/awareness)) for connections using [IFTTT Location service](https://ifttt.com/location).

* [Dependencies](#)
* [Requirements](#)
* [Installation](#)
* [Usage](#)

## Dependencies
* [ConnectButton SDK](https://github.com/IFTTT/ConnectSDK-Android)
* [Google Awareness API](https://developers.google.com/awareness)

## Requirement
* Android SDK version 18 or higher.
* Java 8
* [ACCESS_FINE_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_FINE_LOCATION) permission.

## Installation
### Gradle
```groovy
implementation "com.ifttt.connect-location:2.3.0"
```

## Usage
### Prerequisite
To use this library, you should have a connection on your service on IFTTT that connects the IFTTT Location service. To learn more about creating connections, please visit [developer documentation](https://platform.ifttt.com/docs/connections).

In addition, your app should be integrating with ConnectButton SDK first, to provide UI functionality for your users to manage their connections.
### Set up Awareness API
Connect Location depends Awareness API's Location Fence component. To set up Awareness API for your project, please follow the instructions below and get the project set up:
* [Signup and API key](https://developers.google.com/awareness/android-api/get-a-key)
* [Set up Awareness dependency](https://developers.google.com/awareness/android-api/get-started)

### Initialization
To initialize the SDK, call `ConnectLocation.init` method at the earliest possible state within your app's lifecycle, ideally within your Application's `onCreate` method. This is to make sure the SDK can set up connection monitoring background job as early as possible.

### Integrate with ConnectButton SDK
In the Activity/Fragment that you place the `ConnectButton` View for the connection **that uses Location service**, call `ConnectLocation.getInstance().setUpWithConnectButton` method to allow ConnectLocation to listen to button state changes, and update the geo-fences accordingly.

In this method, a `LocationPermissionCallback` is required as a callback mechanism to notify your app to prompt Location permission, in case the user has not yet granted the permission and the connection is connected.

### Set up monitoring for returning users
Since the geo-fencing functionality operates mostly in the background, as well as in context that's outside of the Activity/Fragment that the ConnectButton is shown, your app needs to make sure that if the user has a connected Location connection, the geo-fences can be set up and the permission is granted.

To help "activating" the geo-fences for a given user, call `ConnectLocation.getInstance().activate` method on the activity that the user can grant the location permission. Ideally, this Activity should be your app's home screen, since you want to make sure the geo-fences can start capturing location changes as soon as the connection is connected. Under the hood, this method does the following things:
* Fetch the latest `Connection` data for the user
* Check if there is any enabled geo-fence that needs to be set up
* Check if the app has `ACCESS_FINE_LOCATION` permission
  * If the app has the permission, it will update the registered geo-fences if necessary
  * If the app doesn't have the permission, it will call the `LocationPermissionCallback#onRequestLocationPermission` to inform your app to prompt the permission request

Once your app received the location permission grant, you can call `ConnectLocation.getInstance().activate`, without passing the LocationPermissionCallback instance, to activate the geo-fences.


