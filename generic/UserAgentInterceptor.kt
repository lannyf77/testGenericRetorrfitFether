package com.oath.doubleplay.muxer.fetcher.generic

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class UserAgentInterceptor(private val mUserAgent: String) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
                .addHeader("user-agent", mUserAgent)
                .build()
        return chain.proceed(request)
    }
}
