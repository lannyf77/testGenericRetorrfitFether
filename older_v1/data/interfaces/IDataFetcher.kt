package com.doubleplay.data.interfaces

import java.util.*

/**
 * Created by linma9 on 4/3/18.
 */

interface IHandler<T> {
    companion object{
        const val STATE_COMPLETE = "IHandler.STATE_COMPLETE_OK"
        const val STATE_DATA_READY = "IHandler.STATE_DATA_READY"
        const val STATE_ERROR = "IHandler.STATE_COMPLETE_ERROR"
    }

    fun onRemoteDataReady(theData : T?, status: String, error: String)
}

interface IRemoteDataFetcher {
    //TODO: header, cookie, token?
    fun <T> fetchRemoteData(scheme:String, authourity: String, path: String,
                        headers: HashMap<String, String>,
                        params: HashMap<String, String>,
                        dataReadyCb: IHandler<IRemoteData<T>>, POJOClassType: Class<T>)

}

interface IRemoteData<T> {

    fun getPOJOClazzType() : Class<T>
    fun getPOJOData() :T?

    /**
     * true if code() is in the range [200..300)
     */
    fun isSuccess() : Boolean
    /**
     * response json
     */
    fun getJsonString() : String
    /**
     * HTTP status code
     */
    fun getCode() : Int

    /**
     * HTTP status message
     */
    fun getMessage() : String
}