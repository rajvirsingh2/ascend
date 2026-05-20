package com.ascend.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class HMACInterceptor @Inject constructor() : Interceptor {

    // loaded from BuildConfig — set at build time, never hardcoded in source
    private val secret = com.ascend.app.BuildConfig.HMAC_SECRET

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // only sign mutating methods
        if (request.method == "GET" || request.method == "OPTIONS") {
            return chain.proceed(request)
        }

        val timestamp = Instant.now().epochSecond
        val message = "$timestamp:${request.method}:${request.url.encodedPath}"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val signature = mac.doFinal(message.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val signed = request.newBuilder()
            .addHeader("X-Timestamp", timestamp.toString())
            .addHeader("X-Signature", signature)
            .build()

        return chain.proceed(signed)
    }
}