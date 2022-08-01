package com.oath.doubleplay.muxer.fetcher.generic

import android.text.TextUtils
import com.google.gson.Gson
import com.oath.doubleplay.muxer.interfaces.IAuthProvider
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.util.*

class GenericDataRequest
constructor(
    val httpClient: OkHttpClient,
    val gson: Gson?
) : IGenericDataRequest {

    override suspend fun <T> fetchRemoteData(
        baseUrl: String,
        path: String,
        headers: HashMap<String, String>,
        params: HashMap<String, String>,
        callback: IFetchRemoteDataCommandCallback<IGenericData<T>>,
        POJOClassType: Class<T>?
    ) {
        val command = FetchRemoteDataCommand<T>(baseUrl, path, headers, params,
                body = null,
                callback = callback,
                POJOClassType = POJOClassType,
                httpClient = httpClient,
                gson = gson)
        command.execute()
    }

    override suspend fun <T> fetchRemoteDataWithPostBody(
        baseUrl: String,
        path: String,
        headers: HashMap<String, String>,
        params: HashMap<String, String>,
        body: RequestBody,
        callback: IFetchRemoteDataCommandCallback<IGenericData<T>>,
        POJOClassType: Class<T>?
    ) {
        val command = FetchRemoteDataCommand<T>(baseUrl, path, headers, params,
                body = body,
                callback = callback,
                POJOClassType = POJOClassType,
                httpClient = httpClient,
                gson = gson)
        command.executeNext()
    }
}

class CookiesInterceptor
constructor(private val authProvider: IAuthProvider?) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (authProvider != null) {
            val cookies = authProvider.getCookies()

            if (!TextUtils.isEmpty(cookies)) {
                request = request.newBuilder().addHeader(HEADER_NAME_COOKIE, cookies!!).build()
            }
        }
        return chain.proceed(request)
    }

    companion object {
        private val HEADER_NAME_COOKIE = "Cookie"
    }
}
