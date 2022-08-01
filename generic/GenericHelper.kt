package com.oath.doubleplay.muxer.fetcher.generic

import android.os.Parcelable
import com.google.gson.Gson
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

abstract class GenericHelper : Parcelable {

    abstract fun <T> getDataHandler(): IGenericDataHandler<T>

    abstract fun <T> getPojoClass(): Class<T>

    /** Default implementation, Client may provide their own */
    open fun getHttpClient(): OkHttpClient {
        // Client should not use it. provide their own with all necessary interceptors and stuff
        val builder = OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS)
                .connectTimeout(120, TimeUnit.SECONDS)
                .connectionPool(ServiceFactory.GLOBAL_CONNECTION_POOL)

        return builder.build()
    }

    /** Default implementation, Client may provide their own */
    open fun getCustomGson(): Gson {
        return Gson()
    }
}
