# testGenericRetorrfitFether
files for using generic retrofit fetcher

in v1:
```

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
===
 private fun getRemoteData(observer: Observer<Response<ResponseBody>>) {
        val retrofitServiceFactory = DataFetcherComponentInjector.get().getServiceFactory()
        val service: IRemoteRepositoryService = retrofitServiceFactory.createRetrofitService(IRemoteRepositoryService::class.java, mBaseUrl)

        // for using RxJava observable
//        service.getRemoteData(mPath, mHeaders, mParams)
//                .subscribeOn(Schedulers.newThread())
//                .subscribe(observer)

        //TEST_ML===<
        service.getRemoteData_cb(mPath, mHeaders, mParams).enqueue(object : Callback<ResponseBody>{
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("+++", "+++ getRemoteData_cb() onFailure: $t")
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.e("+++", "+++ onResponse() response: $response")
                //var resp = response.raw()
                val remoteData = RemoteData<T>(response, mPOJOClazz)

                val thread = Thread {
                    val cb = mDataReadyListener?.get()
                    cb?.onRemoteDataReady(remoteData, IHandler.STATE_DATA_READY, "")
                }
                thread.start()
            }
        })

        //==========>

    }
    
    ===========
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
```

in generic:

```
    fun <T> createRetrofitService(clazz: Class<T>, endPoint: String, httpClient: OkHttpClient): T {

        val restAdapter = Retrofit.Builder()
                .baseUrl(endPoint)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build()
        return restAdapter.create(clazz)
    }
    
    ======
    
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
    
    ======
    
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

=======
//the core part:

 val response = service.getRemoteData(path, headers, params)
            try {
                val headerList = response.headers()
                val remoteData = GenericData<T>(response, mPOJOClazz, gson)
    
```
