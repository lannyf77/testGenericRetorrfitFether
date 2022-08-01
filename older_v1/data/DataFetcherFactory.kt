package com.doubleplay.data

import com.doubleplay.data.interfaces.ICookieProvider
import com.doubleplay.data.interfaces.IRemoteDataFetcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Created by linma9 on 4/3/18.
 */

/**
 * the entry point for using the Core module
 */
class DataFetcherFactory {
    companion object {
        private var remoteDataRepository: IRemoteDataFetcher? = null

        /**
         * provide RemoteDataFetcher from Core module
         * it takes the overrides for the elements used by the Core module to make the remote request
         */
        fun getRemoteDataFetcher(httpClient: OkHttpClient?,
                                 httpLoggingInterceptor: HttpLoggingInterceptor?,
                                 defaultInterceptors: List<Interceptor>?,
                                 defaultNetworkInterceptors: List<Interceptor>?,
                                 cookieProvider: ICookieProvider?,
                                 userAgent: String): IRemoteDataFetcher
             {
                if (remoteDataRepository == null) {
                    remoteDataRepository = RemoteDataFetcher(
                            httpClient,
                            httpLoggingInterceptor,
                            defaultInterceptors,
                            defaultNetworkInterceptors,
                            cookieProvider,
                            userAgent)
                }
                return remoteDataRepository!!
            }
    }
}