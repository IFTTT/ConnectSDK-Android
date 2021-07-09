package com.ifttt.location;

/**
 * Interface representing cached data within ConnectLocation module.
 *
 * @see SharedPreferencesGeofenceCache
 * @see SharedPreferenceUserTokenCache
 */
interface Cache<T> {
    void write(T t);

    T read();

    void clear();
}
