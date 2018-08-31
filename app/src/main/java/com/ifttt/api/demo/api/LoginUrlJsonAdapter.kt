package com.ifttt.api.demo.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.io.IOException

internal object LoginUrlJsonAdapter {
    @JsonQualifier annotation class LoginUrl

    private val options = JsonReader.Options.of("login_url")

    @LoginUrl
    @FromJson
    @Throws(IOException::class)
    fun fromJson(jsonReader: JsonReader): String {
        var loginUrl: String? = null

        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            val index = jsonReader.selectName(options)
            when (index) {
                -1 -> jsonReader.skipValue()
                0 -> loginUrl = jsonReader.nextString()
                else -> throw IllegalStateException("Unknown index: " + index)
            }
        }

        if (loginUrl == null) {
            throw IllegalStateException("Login url is not presented in the response.")
        }

        return loginUrl
    }

    @ToJson
    fun toJson(jsonWriter: JsonWriter, @LoginUrl loginUrl: String) {
        throw UnsupportedOperationException()
    }
}