package com.doubleplay.data.di.component

import com.doubleplay.data.RemoteDataFetcher
import com.doubleplay.data.di.module.DataFetcherModule
import com.doubleplay.data.service.ServiceFactory

import javax.inject.Singleton

import dagger.Component
import okhttp3.OkHttpClient

/**
 * Created by linma9 on 4/23/18.
 */

@Singleton
@Component(modules = arrayOf(DataFetcherModule::class))
interface DataFetcherComponent {

    fun inject(dataFetcher: RemoteDataFetcher)
    fun getOkHttpClient() : OkHttpClient
    fun getServiceFactory() : ServiceFactory
}
