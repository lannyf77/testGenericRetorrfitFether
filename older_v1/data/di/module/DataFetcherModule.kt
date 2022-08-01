package com.doubleplay.data.di.module

import android.text.TextUtils

import com.doubleplay.data.interfaces.ICookieProvider
import com.doubleplay.data.service.CookiesInterceptor
import com.doubleplay.data.service.ServiceFactory
import com.doubleplay.data.service.UserAgentInterceptor
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.yahoo.doubleplay_data.BuildConfig
import java.util.Collections

import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by linma9 on 4/23/18.
 */

@Module
class DataFetcherModule(
        httpClient: OkHttpClient?,
        httpLoggingInterceptor: HttpLoggingInterceptor?,
        defaultInterceptors: List<Interceptor>?,
        defaultNetworkInterceptors: List<Interceptor>?,
        private val mCookieProvider: ICookieProvider?, ua: String) {

    private val mHttpClient: OkHttpClient? = httpClient
    private val mHttpLoggingInterceptor: HttpLoggingInterceptor
    private val mDefaultInterceptors: List<Interceptor>
    private val mDefaultNetworkInterceptors: List<Interceptor>
    private var mUserAgent: String = DEFAULT_USER_AGENT

    init {
        if (httpLoggingInterceptor == null) {
            mHttpLoggingInterceptor = if (BuildConfig.DEBUG) {
                // for debug log raw data
                HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message ->
                    Timber.i("timber.i(DataFetcherModule::HttpLoggingInterceptor::Logger()):   $message \ntimber.i: --- thread: ${Thread.currentThread().getId()} ------")
                })
            } else {
                HttpLoggingInterceptor()
            }
            mHttpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            mHttpLoggingInterceptor = httpLoggingInterceptor
        }

        mDefaultInterceptors = if (defaultInterceptors != null) {
            Collections.unmodifiableList(defaultInterceptors)
        } else {
            emptyList()
        }
        mDefaultNetworkInterceptors = if (defaultNetworkInterceptors != null) {
            Collections.unmodifiableList(defaultNetworkInterceptors)
        } else {
            emptyList()
        }

        if (!TextUtils.isEmpty(ua)) {
            mUserAgent = ua
        } else {
            try {
                mUserAgent = System.getProperty(KEY_SYSTEM_HTTP_AGENT)
            } catch (systemException: SecurityException) {}
        }
    }

    @Singleton
    @Provides
    internal fun provideCookiesInterceptor(): CookiesInterceptor {
        return CookiesInterceptor(mCookieProvider)
    }

    @Singleton
    @Provides
    internal fun provideUserAgentInterceptor(): UserAgentInterceptor {
        return UserAgentInterceptor(mUserAgent)
    }

    /**
     * using the passed in mHttpClient otherwise build one with default config
     */
    @Singleton
    @Provides
    internal fun provideOkHttpClient(
                                     cookiesInterceptor: CookiesInterceptor,
                                     userAgentInterceptor: UserAgentInterceptor
    ): OkHttpClient {

        if (mHttpClient != null) {
            return mHttpClient
        } else {
            val builder = OkHttpClient.Builder()
                    .readTimeout(120, TimeUnit.SECONDS)
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .connectionPool(ServiceFactory.GLOBAL_CONNECTION_POOL)

            for (interceptor in mDefaultInterceptors) {
                builder.addInterceptor(interceptor)
            }

            for (networkInterceptor in mDefaultNetworkInterceptors) {
                builder.addNetworkInterceptor(networkInterceptor)
            }

            builder.addInterceptor(userAgentInterceptor)
                    .addNetworkInterceptor(cookiesInterceptor)
                    .addInterceptor(mHttpLoggingInterceptor)

            if (BuildConfig.DEBUG) {
                builder.addNetworkInterceptor(StethoInterceptor())
            }

            return builder.build()
        }
    }

    companion object {
        private val DEFAULT_USER_AGENT = "Mozilla/x.x (Linux; Android x.x.x; Yahoo Newsroom)"
        private val KEY_SYSTEM_HTTP_AGENT = "http.agent"
    }
}
