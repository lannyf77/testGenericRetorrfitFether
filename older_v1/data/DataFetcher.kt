package com.doubleplay.data

import com.doubleplay.data.command.*
import com.doubleplay.data.di.component.DaggerDataFetcherComponent
import com.doubleplay.data.di.component.DataFetcherComponent
import com.doubleplay.data.di.module.DataFetcherModule
import com.doubleplay.data.interfaces.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

import java.util.HashMap


/**
 * Created by linma9 on 4/23/18.
 */

/**
 *   DataFetcher responsible for pulling the data from remote server
 *   It creates DataFetcherModule with the overrides
 *   After that the fetchRemoteData() does the fetching
 */

class RemoteDataFetcher(httpClient: OkHttpClient?,
                        httpLoggingInterceptor: HttpLoggingInterceptor?,
                        defaultInterceptors: List<Interceptor>?,
                        defaultNetworkInterceptors: List<Interceptor>?,
                        cookieProvider: ICookieProvider?,
                        ua: String) : IRemoteDataFetcher {

    companion object {
        lateinit var instance: RemoteDataFetcher
            private set
    }

    init {
        instance = this
        instance.initDataFetcher(httpClient,
            httpLoggingInterceptor,
            defaultInterceptors,
            defaultNetworkInterceptors,
            cookieProvider,
            ua)
    }

    private var sDataFetcherComponent: DataFetcherComponent? = null
    val dataFetcherComponent: DataFetcherComponent
        get() {
            if (sDataFetcherComponent == null) {
                // should enforce the DataFetcherModule has been init-ed
                throw(Exception("the DataFetcher has not been init-ed, call initDataFetcher() first!"))
            }
            return sDataFetcherComponent!!
        }

    private fun initDataFetcher(httpClient: OkHttpClient?,
                                httpLoggingInterceptor: HttpLoggingInterceptor?,
                                defaultInterceptors: List<Interceptor>?,
                                defaultNetworkInterceptors: List<Interceptor>?,
                                cookieProvider: ICookieProvider?,
                                ua: String) {

        synchronized(this) {
            sDataFetcherComponent = DaggerDataFetcherComponent.builder()
                    .dataFetcherModule(DataFetcherModule(
                            httpClient,
                            httpLoggingInterceptor,
                            defaultInterceptors,
                            defaultNetworkInterceptors,
                            cookieProvider, ua))

                    .build()
        }
    }

    /**
     * taking params for making the remote request
     *
     * dataReadyCb: the callback which will receive IRemoteData<T>
     * POJOClassType: the class type of the T
     */
    override fun <T> fetchRemoteData(scheme: String, authourity: String, path: String,
                                     headers: HashMap<String, String>,
                                     params: HashMap<String, String>,
                                     dataReadyCb: IHandler<IRemoteData<T>>, POJOClassType: Class<T>) {
                FetchRemoteDataCommand(scheme, authourity, path, headers, params, dataReadyCb, POJOClassType)
                .execute()
    }
}

