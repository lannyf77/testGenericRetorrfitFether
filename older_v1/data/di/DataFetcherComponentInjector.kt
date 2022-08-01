package com.doubleplay.data.di

import com.doubleplay.data.RemoteDataFetcher
import com.doubleplay.data.di.component.DataFetcherComponent

/**
 * Created by linma9 on 25/04/2018
 */
class DataFetcherComponentInjector {
    companion object {
        fun get() : DataFetcherComponent =
                RemoteDataFetcher.instance.dataFetcherComponent
    }
}