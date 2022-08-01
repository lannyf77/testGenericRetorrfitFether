package com.oath.doubleplay.muxer.fetcher.generic

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.oath.doubleplay.muxer.NetworkConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import java.net.SocketTimeoutException

/**
 * fetch command for class type <T>
 *
 * with baseUrl, path, headers, params and body the http request will be sent out via httpClient
 * the provided dataHandler will deserialize the response into object of class type <T> using the gson
 */
class FetchRemoteDataCommand<T>(
    val baseUrl: String,
    val path: String,
    var headers: HashMap<String, String>,
    var params: HashMap<String, String>,
    val body: RequestBody? = null,
    callback: IFetchRemoteDataCommandCallback<IGenericData<T>>,
    var httpClient: OkHttpClient,
    POJOClassType: Class<T>?,
    val gson: Gson?
) {

    private var TAG: String = "FetchRemoteDataCommand"
    private var fetchCompleteCallback: IFetchRemoteDataCommandCallback<IGenericData<T>> = callback

    val mPOJOClazz: Class<T>? = POJOClassType

    suspend fun execute() = withContext(Dispatchers.IO) {
        val service = ServiceFactory().createRetrofitService(IRemoteDataRequest::class.java, baseUrl, httpClient)
        try {
            val response = service.getRemoteData(path, headers, params)
            try {
                val headerList = response.headers()
                val remoteData = GenericData<T>(response, mPOJOClazz, gson)
                val errorBodyString = response.errorBody()?.string() // the string could be very large
                val success = response.isSuccessful
                val contentType = response.body()?.contentType()
                val code = response.code() // http status code
                var message = response.message() // HTTP status message

                if (!success && TextUtils.isEmpty(message)) {
                    message = getErrorMessage(errorBodyString)
                }
                fetchCompleteCallback.onDataFetchComplete(remoteData, success, code, message, errorBodyString, contentType, headerList)
            } finally {
                /**
                 * Closes this stream and releases any system resources associated
                 * with it. If the stream is already closed then invoking this
                 * method has no effect.
                 */
                response.body()?.close()
            }
        } catch (t: Throwable) {
            val statusCode = if ((t is SocketTimeoutException)) NetworkConstants.REQUEST_TIMEOUT else -1
            fetchCompleteCallback.onDataFetchComplete(null, false, statusCode, t.message ?: "", null, null, null)
        }
    }

    suspend fun executeNext() = withContext(Dispatchers.IO) {
        val service = ServiceFactory().createRetrofitService(IRemoteDataRequest::class.java, baseUrl, httpClient)
        try {
            val response = service.getMoreRemoteData(path, headers, params, body!!)
            try {
                val headerList = response.headers()
                val remoteData = GenericData<T>(response, mPOJOClazz, gson)
                val errorBodyString = response.errorBody()?.string()
                val success = response.isSuccessful // okHttp has (code >= 200 && code < 300)
                val contentType = response.body()?.contentType()
                val code = response.code() ?: -1 // http status code
                var message = response.message() ?: "" // HTTP status message

                if (!success && TextUtils.isEmpty(message)) {
                    message = getErrorMessage(errorBodyString)
                }
                fetchCompleteCallback.onDataFetchComplete(remoteData, success, code, message, errorBodyString, contentType, headerList)
            } finally {
                response.body()?.close()
            }
        } catch (t: Throwable) {
            val statusCode = if ((t is SocketTimeoutException)) NetworkConstants.REQUEST_TIMEOUT else -1
            fetchCompleteCallback.onDataFetchComplete(null, false, statusCode, t.message ?: "", null, null, null)
        }
    }

    private fun getErrorMessage(errorBodyString: String?): String {
        var error = ""
        try { // errorBody?.string() could be big
            val errorJsonString = errorBodyString
            if (!TextUtils.isEmpty(errorJsonString)) {
                try {
                    val parsedString = JsonParser().parse(errorJsonString)
                    error = parsedString.asJsonObject["error"]?.toString() ?: ""
                    if (TextUtils.isEmpty(error)) {
                        error = parsedString.asJsonObject["message"]?.toString() ?: ""
                    }
                } catch (ie: Exception) {
                    Log.e("FetchRemoteDataCommand", "+++ unable to parse error $errorJsonString")
                }
            }
        } catch (ex: Throwable) {}
        return error
    }
}
