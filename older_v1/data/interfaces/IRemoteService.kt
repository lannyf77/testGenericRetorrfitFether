package com.doubleplay.data.interfaces

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * Created by linma9 on 4/3/18.
 */

/**
 * retrofit service for GET
 * TODO: need one for POST
 */
interface IRemoteRepositoryService {

    @GET
    fun getRemoteData(@Url url: String,
                      @HeaderMap headers: Map<String, String>,
                      @QueryMap params: Map<String, String>): Observable<Response<ResponseBody>>

    @GET
    fun getRemoteData_cb(@Url url: String,
                      @HeaderMap headers: Map<String, String>,
                      @QueryMap params: Map<String, String>): Call<ResponseBody>

}