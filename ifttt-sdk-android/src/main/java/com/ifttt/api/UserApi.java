package com.ifttt.api;

import com.ifttt.User;

/**
 * API endpoint for retrieving {@link User} information.
 */
public interface UserApi {
    /**
     * @return A {@link PendingResult} for the API call execution to get the user information.
     */
    PendingResult<User> user();
}
