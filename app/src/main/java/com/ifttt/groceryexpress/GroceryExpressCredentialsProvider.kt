package com.ifttt.groceryexpress

import com.ifttt.connect.ui.CredentialsProvider

class GroceryExpressCredentialsProvider(private val emailPreferencesHelper: EmailPreferencesHelper):
    CredentialsProvider {

    override fun getUserToken() = ApiHelper.getUserToken(emailPreferencesHelper.getEmail())

    override fun getOAuthCode() = emailPreferencesHelper.getEmail()
}
