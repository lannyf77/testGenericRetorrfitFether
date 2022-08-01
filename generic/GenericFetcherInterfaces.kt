package com.oath.doubleplay.muxer.fetcher.generic

import android.util.Log
import com.google.gson.Gson
import com.oath.doubleplay.muxer.interfaces.IData
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import java.util.HashMap

/**
 * interface for the remote data request
 */
interface IRemoteDataRequest {

    @GET
    suspend fun getRemoteData(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>
    ): Response<ResponseBody>

    @POST
    suspend fun getMoreRemoteData(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @QueryMap params: Map<String, String>?,
        @Body body: RequestBody
    ): Response<ResponseBody>
}

/**
 * interface for data handler
 */
interface IGenericDataHandler<T> {

    fun onRemoteDataReady(remoteData: T?, success: Boolean, statusCode: Int, statusMessage: String, errorResponse: String?)
    fun getRemoteData(): List<IData>
    fun moreDataAvailable(): Boolean
    fun applyPagination(parameterMap: HashMap<String, String>)
}

/**
 * interface IFetchRemoteDataCommandCallback<T>
 *
 * for FetchRemoteDataCommand's fetching complete
 *
 * remoteData: deserilized data of class type <T>
 * statusCode: HTTP status code.
 * statusMessage: HTTP status message or null if unknown.
 * errorResponse: ResponseBody.string() (see okhttp3 ResponseBody)
 * contentType: HTTP content type (NCP parsing needs it)
 * headers: HTTP request header (NCP parsing needs it back)
 */
interface IFetchRemoteDataCommandCallback<T> {
    suspend fun onDataFetchComplete(
        remoteData: T?,
        success: Boolean,
        statusCode: Int,
        statusMessage: String,
        errorResponse: String?,
        contentType: MediaType?,
        headers: Headers?
    )
}

/**
 * interface for fetching remote data
 */
interface IGenericDataRequest {
    suspend fun <T> fetchRemoteData(
        baseUrl: String,
        path: String,
        headers: HashMap<String, String>,
        params: HashMap<String, String>,
        callback: IFetchRemoteDataCommandCallback<IGenericData<T>>,
        POJOClassType: Class<T>?
    )

    suspend fun <T> fetchRemoteDataWithPostBody(
        baseUrl: String,
        path: String,
        headers: HashMap<String, String>,
        params: HashMap<String, String>,
        body: RequestBody,
        callback: IFetchRemoteDataCommandCallback<IGenericData<T>>,
        POJOClassType: Class<T>?
    )
}

/**
 * interface for generic data wrap
 */
interface IGenericData<T> {

    fun getPOJOClazzType(): Class<T>?
    fun getPOJOData(): T?

    /**
     * true if code() is in the range [200..300)
     */
    fun isSuccess(): Boolean

    /**
     * response json
     */
    fun getJsonString(): String

    /**
     * HTTP status code
     */
    fun getCode(): Int

    /**
     * HTTP status message
     */
    fun getMessage(): String
}

/**
 * generic data wrap utils to parse the Response<ResponseBody> into
 * deserialized object of class type <T>
 *
 * response: a retrofit Response for okhttp ResponseBody
 * pojoClassType: the class type for deserializing to the object
 * json: configured Gson, optional, if null the Gson() will be used
 */
class GenericData<T>(response: Response<ResponseBody>, pojoClassType: Class<T>?, gson: Gson?) : IGenericData<T> {

    private val TAG = GenericData::class.java.simpleName
    private var mIsSuccess: Boolean = response.isSuccessful
    private var mCode: Int = response.code()
    private val mMessage: String = response.message()

    private val mPOJOClazz: Class<T>? = pojoClassType
    private var mPOJOData: T? = null
    private val mJsonString: String = response.body()?.string() ?: ""

    init {
        mPOJOData = try {
            if (mPOJOClazz != null) CustomJsonParser.parse(mJsonString, mPOJOClazz, gson) else null
        } catch (e: Exception) {
            Log.e(TAG, "Parsing exception!", e)
            mIsSuccess = false
            mCode = -1
            null
        }
    }

    override fun getPOJOClazzType(): Class<T>? {
        return mPOJOClazz
    }

    override fun getPOJOData(): T? {
        return mPOJOData
    }

    override fun getJsonString(): String {
        return mJsonString
    }

    override fun getCode(): Int {
        return mCode
    }

    override fun isSuccess(): Boolean {
        return mIsSuccess
    }

    override fun getMessage(): String {
        return mMessage
    }
}
