# Connect API
The Connect API module serves as a base and the API layer for Connect Button and Connect Location SDK. It includes a Connect API client (`ConnectionApiClient`) and a set of data structure to represent a `Connection`.

In the following section, we refer to Connect Button and Connect Location SDK as "SDK".

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

## ConnectionApiClient
`ConnectionApiClient` is the IFTTT Connection API wrapper, it can be used to make API calls with pre-defined data structures mentioned above. To start, use the `ConnectionApiClient.Builder` to get an instance:
* a `UserTokenProvider` is required to construct a `ConnectionApiClient.Builder`. `UserTokenProvider#getUserToken` is used to provide the user token to all API calls to Connect API when possible. Note that because this method is going to be called for all API calls, you should consider caching the value if possible. In addition, if your app supports changing users (via log in/out), you should make sure the implementation of the UserTokenProvider takes into account the user login status. 
* `setInviteCode(String inviteCode)`:   The invite code is used to access preview services on IFTTT Platform. 

With the instance, you can use `ConnectionApiClient#api()` to get access to the APIs for Connection data.
```java
// Implement this interface to fetch user token with your backend.
UserTokenProvider userTokenProvider = ...;
ConnectionApiClient apiClient = new ConnectionApiClient.Builder(context, userTokenProvider)  
        .setInviteCode("invite_code") // Optional, only needed if your service is not yet published on IFTTT Platform.
        .build();

ConnectionApi api = apiClient.api();
```

The APIs available are:
* `showConnection`: returns metadata for a Connection.
* `user`: return the authenticated user.

## Connection and Service data structure
This module provides data structure from the Connection API: Connection, Service, User, Feature, UserFeature, UserFeatureStep, UserFeatureField and various field value representations. Some example fields that we provide are
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
