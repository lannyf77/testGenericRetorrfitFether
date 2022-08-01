package com.doubleplay.data.service

import com.doubleplay.data.di.DataFetcherComponentInjector
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Created by linma9 on 1/23/18.
 */


@Singleton
class ServiceFactory @Inject constructor() {

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

    fun <T> createRetrofitService(clazz: Class<T>, endPoint: String): T {

        var mHttpClient: OkHttpClient? = DataFetcherComponentInjector.get().getOkHttpClient()
        val restAdapter = Retrofit.Builder()
                .baseUrl(endPoint)
                //.addCallAdapterFactory(RxJava2CallAdapterFactory.create())  //for RxJava2, getRemoteData(object : Observer<Response<ResponseBody>>
                .addConverterFactory(GsonConverterFactory.create())  // need for using service.getRemoteData_cb(mPath, mHeaders, mParams).enqueue(object : Callback<ResponseBody>{
                .client(mHttpClient)
                .build()
        return restAdapter.create(clazz)
    }
}
