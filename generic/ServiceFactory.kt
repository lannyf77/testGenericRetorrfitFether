package com.oath.doubleplay.muxer.fetcher.generic

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import retrofit2.converter.gson.GsonConverterFactory

class ServiceFactory constructor() {

    companion object {
        /**
         * https://square.github.io/okhttp/3.x/okhttp/okhttp3/OkHttpClient.html
         * OkHttp performs best when you create a single OkHttpClient instance and reuse it for all of your HTTP calls.
         * This is because each client holds its own connection pool and thread pools.
         * Reusing connections and threads reduces latency and saves memory.
         */
        val GLOBAL_CONNECTION_POOL = ConnectionPool(6, 15, TimeUnit.SECONDS)
    }

    /**
     * Creates a retrofit service from an arbitrary class (clazz)
     * @param clazz Java interface of the retrofit service
     * @param endPoint REST endpoint url
     * @return retrofit service with defined endpoint
     */

    fun <T> createRetrofitService(clazz: Class<T>, endPoint: String, httpClient: OkHttpClient): T {

        val restAdapter = Retrofit.Builder()
                .baseUrl(endPoint)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build()
        return restAdapter.create(clazz)
    }
}
