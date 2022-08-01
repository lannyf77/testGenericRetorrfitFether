package com.doubleplay.data.model

import android.util.Log
import com.doubleplay.data.interfaces.IRemoteData
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * impl for IRemoteData passed back to the IHandler<IRemoteData<T>>
 * getting the json string from the okhttp3.ResponseBody
 * parse the json with pojoClassType to build the POJO
 */

/**
 * Created by linma9 on 4/3/18.
 */

class RemoteData<T>(response: Response<ResponseBody>, pojoClassType: Class<T>) : IRemoteData<T> {

    private val TAG = RemoteData::class.java.simpleName
    private var mIsSuccess: Boolean = response.isSuccessful
    private var mCode: Int = response.code()
    private val mMessage : String = response.message()

    private val mPOJOClazz: Class<T> = pojoClassType
    private var mPOJOData: T? = null
    private val mJsonString: String = response.body()?.string() ?: ""

    init {
        mPOJOData = try {
            JsonParser.parse(mJsonString, mPOJOClazz)
        } catch (e: Exception) {
            Log.e(TAG, "Parsing exception!", e)
            mIsSuccess = false
            mCode = -1  //TODO: define internal error
            null
        }
    }

    override fun getPOJOClazzType() : Class<T> {
        return mPOJOClazz
    }
    override fun getPOJOData() :T? {
        return mPOJOData
    }

    override fun getJsonString(): String {
        return mJsonString
    }

    override fun getCode() : Int {
        return mCode
    }

    override fun isSuccess() : Boolean {
        return mIsSuccess
    }

    override fun getMessage() : String {
        return mMessage
    }

}