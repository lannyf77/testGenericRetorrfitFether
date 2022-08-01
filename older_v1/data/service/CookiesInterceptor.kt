package com.doubleplay.data.service

import android.net.Uri
import android.text.TextUtils
import com.doubleplay.data.interfaces.ICookieProvider
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class CookiesInterceptor @Inject
constructor(private val mCookieProvider: ICookieProvider?) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (mCookieProvider != null) {
            val cookies = mCookieProvider.getCookies(Uri.parse(request.url().toString()))

            if (!TextUtils.isEmpty(cookies)) {
                request = request.newBuilder().addHeader(HEADER_NAME_COOKIE, cookies).build()
            }
        }
        return chain.proceed(request)
    }

    companion object {
        private val HEADER_NAME_COOKIE = "Cookie"
    }
}