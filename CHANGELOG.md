# Change Log

## ConnectButton
### v2.2.0
#### Breaking API changes
* Namespace changes: the following classes have been moved to `com.ifttt.connect.api`:
  * Connection
  * Service
  * User
  * PendingResult
  * ConnectionApiClient
* API change: ConnectButton.Configuration has been reworked, use `ConnectButton.Configuration.newBuilder(String email, Uri redirectUri)` to instantiate a builder and follow the steps with appropriate parameters:
  * for connection information: use `withConnection(Connection connection)` or `withConnectionId(String id)` 
  * for ConnectionApiClient: if you want to use your own ConnectionApiClient instance, use `withClient(ConnectionApiClient client, CredentialProvider)`; otherwise use `withCredentialProvider`
* API change: `ConnectionApiClient#setUserToken` has been removed, you should use `UserTokenCredential` to set up API authorization.
* API change: `ConnectionApiClient#Builder` has been reworked, now it requires a Context and a UserTokenProvider to instantiate.
* API deprecation: `ValueProposition` and `Connection#valuePropositions` has been deprecated. Use `Feature` and `Connection#features` instead. 
* API addition: more Connect API data structure support has been added
  * `Feature`: representing a Feature for the connection
  * `UserFeature`: representing a user-enabled feature for the connection
  * `UserFeatureStep`: representing a trigger/action/query in the user-enabled feature
  * `UserFeatureField`: representing a user configured field in the enabled feature
  * `LocationFieldValue`, `CollectionFieldValue`, `StringArrayFieldValue`, `StringFieldValue`: representing different types of the configured field value
* Feature addition:
  * Configuration skipping - Details in README
  * Localization - Details in README
