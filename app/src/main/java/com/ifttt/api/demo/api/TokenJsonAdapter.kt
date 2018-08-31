package com.ifttt.api.demo.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Options
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.io.IOException

internal object TokenJsonAdapter {

    @JsonQualifier annotation class Token

    private val options = Options.of("token")

    @Token
    @FromJson
    @Throws(IOException::class)
    fun fromJson(jsonReader: JsonReader): String? {
        var userToken: String? = null

        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            val index = jsonReader.selectName(options)
            when (index) {
                -1 -> jsonReader.skipValue()
                0 -> userToken = if (jsonReader.peek() == JsonReader.Token.NULL) null else jsonReader.nextString()
                else -> throw IllegalStateException("Unknown index: " + index)
            }
        }

        return userToken
    }

    @ToJson
    fun toJson(jsonWriter: JsonWriter, @Token userToken: String) {
        throw UnsupportedOperationException()
    }
}
