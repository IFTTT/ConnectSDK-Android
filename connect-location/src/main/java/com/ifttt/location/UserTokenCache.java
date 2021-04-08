package com.ifttt.location;

interface UserTokenCache {
    void store(String token);

    String get();

    void clear();
}
