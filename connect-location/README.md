# IFTTT Connect Location SDK for Android

[![javadoc](https://javadoc.io/badge2/com.ifttt/connect-location/javadoc.svg)](https://javadoc.io/doc/com.ifttt/connect-location)

IFTTT Connect Location SDK is an add-on library to the [ConnectButton SDK](https://github.com/IFTTT/ConnectSDK-Android/blob/master/README.md), it provides native geo-fencing functionality (through [Google Awareness API](https://developers.google.com/awareness)) for connections using [IFTTT Location service](https://ifttt.com/location).

* [Dependencies](#)
* [Requirements](#)
* [Installation](#)
* [Usage](#)

## Dependencies
* [ConnectButton SDK](https://github.com/IFTTT/ConnectSDK-Android)
* [Google Awareness API](https://developers.google.com/awareness)

## Requirement
* Android SDK version 21 or higher.
* Java 8
* [ACCESS_FINE_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_FINE_LOCATION) permission.
* [ACCESS_BACKGROUND_LOCATION](https://developer.android.com/reference/android/Manifest.permission#ACCESS_BACKGROUND_LOCATION) permission if targeting Android SDK 29+.

## Installation
### Gradle
```groovy
// Required for Connect API integration.
implementation "com.ifttt:connect-api:2.5.2"

// Required for Connect Button UI integration.
implementation "com.ifttt:connect-button:2.5.2"

// Location service integration. 
implementation "com.ifttt.connect-location:2.5.2"
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

In this method, a `LocationStatusCallback` is required as a callback mechanism to notify your app to prompt Location permission, in case the user has not yet granted the permission and the connection is connected.

### Set up monitoring for returning users
Since the geo-fencing functionality operates mostly in the background, as well as in context that's outside of the Activity/Fragment that the ConnectButton is shown, your app needs to make sure that if the user has a connected Location connection, the geo-fences can be set up and the permission is granted.

To help "activating" the geo-fences for a given user, call `ConnectLocation.getInstance().activate` method on the activity that the user can grant the location permission. Ideally, this Activity should be your app's home screen, since you want to make sure the geo-fences can start capturing location changes as soon as the connection is connected. Under the hood, this method does the following things:
* Fetch the latest `Connection` data for the user
* Check if there is any enabled geo-fence that needs to be set up
* Check if the app has `ACCESS_FINE_LOCATION` permission
  * If the app has the permission, it will update the registered geo-fences if necessary
  * If the app doesn't have the permission, it will call the `LocationStatusCallback#onRequestLocationPermission` to inform your app to prompt the permission request

`LocationStatusCallback#onLocationStatusUpdated` is added in v2.3.4, and can be used to listen to the geo-fences status changes:
* when there is at least one geo-fence registered, `onLocationStatusUpdated` will be invoked with `activated` parameter being `true`
* if there is no geo-fence registered, `onLocationStatusUpdated` will be invoked with `activated` parameter being `false`

### Deactivate geo-fences
When your user logs out, you should reflect this state via returning null in the `UserTokenProvider` passed to the ConnectLocation instance, doing so allows it to unregister the geo-fences accordingly. However, if you would like to make sure the geo-fences are correctly removed as soon as the users log out from your app, you can call `ConnectLocation.getInstance().deactivate()` directly.

### Background location limit
On Android 8 and above, the Android OS enforces a [background location limit](https://developer.android.com/about/versions/oreo/background-location-limits) on apps that run in the background. This limit [affects](https://developer.android.com/about/versions/oreo/background-location-limits#apis) the geo-fences API.

In order to make sure your app can still receive reliable geo-fence updates, you may need to set it up so that it can be considered a foreground app. One of the ways you can do it is via a [foreground service](https://developer.android.com/guide/components/foreground-services). Running a foreground service makes sure that when it is active, the app will not be considered as a background app, even if it is not currently on the foreground, and therefore can receive geo-fence updates without the OS limit.

However, a foreground service requires a persistent notification to be displayed while it is active. You should think about the impact of such notification to your users, when you implement the foreground service.

An example implementation of the foreground service can be found in the [example app](https://github.com/IFTTT/ConnectSDK-Android/blob/master/app/src/main/java/com/ifttt/groceryexpress/LocationForegroundService.kt).

### Logging
You can enable logging by calling the `ConnectLocation.setLoggingEnabled(true)` method to see some basic logs on your `Logcat` window. By default, logging is disabled. If you had enabled logging and need to disable it for certain parts of the user flow, call `ConnectLocation.setLoggingEnabled(false)`
