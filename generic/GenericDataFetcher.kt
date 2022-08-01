package com.oath.doubleplay.muxer.fetcher.generic

import com.oath.doubleplay.muxer.BaseHandler
import com.oath.doubleplay.muxer.NetworkConstants
import com.oath.doubleplay.muxer.interfaces.*
import com.oath.doubleplay.muxer.tracking.NetworkTrackingConstants
import com.oath.doubleplay.muxer.tracking.NetworkTrackingUtils
import kotlinx.coroutines.coroutineScope
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.OkHttpClient
import java.util.*

open class GenericDataFetcher(
    private val dataFetcherConfiguration: DataFetcherConfiguration,
    private val customObjectsProvider: GenericHelper?
) : IDataFetcher, IFetchRemoteDataCommandCallback<IGenericData<Any>> {

    private val classTag: String = "GenericDataFetcher"
    protected var responseHandler: BaseHandler? = null
    private lateinit var streamConfig: IStreamConfig
    protected var streamResponse: List<IData>? = null
    protected var fetcherHash: Int = 0
    protected var description: String = "GenericDataFetcher"
    protected var dataRequest: IGenericDataRequest? = null
    private var startTime: Long = 0L
    protected val dataProcessingHandler = customObjectsProvider?.getDataHandler<IGenericData<Any>>()
    private var firstRequestDone: Boolean = false
    private var isFirstRequest: Boolean = true
    private val mUUID: UUID = UUID.randomUUID()
    private val customGson = customObjectsProvider?.getCustomGson()
    private val pojoClass: Class<Any>? = customObjectsProvider?.getPojoClass()
    private val httpClient: OkHttpClient? = customObjectsProvider?.getHttpClient()

    init {
        if (customObjectsProvider == null || httpClient == null) {
            throw RuntimeException("Generic fetcher misconfiguration Exception. Custom objects are not provided")
        }
        generateHash()
        dataRequest = createDataRequest()
        if (dataFetcherConfiguration.streamEndPoint == null)
            throw RuntimeException("Custom Fetcher not configured, missing endpoint")
        setConfig(dataFetcherConfiguration.streamConfig)
        // dataProcessingHandler?.setDataReadyCallback(this)
    }

    protected fun getFetcherHashDesc(queryMap: HashMap<String, String>): String {

        var fetcherType: String = classTag
        for (paramValue in queryMap.values) {
            fetcherType += "_" + paramValue
        }

        return fetcherType
    }

    private fun generateHash() {

        description = getFetcherHashDesc(dataFetcherConfiguration.streamEndPoint?.queryMap as HashMap<String, String>)

        fetcherHash = description.hashCode()
    }

    private fun createDataRequest(): IGenericDataRequest {
        return GenericDataRequest(httpClient!!, gson = customGson)
    }

    override suspend fun getCachedData(): Boolean {
        return false
    }

    override suspend fun getFreshData() {
        coroutineScope {
            startTime = System.currentTimeMillis()

            if (dataProcessingHandler == null || pojoClass == null) {
                throw RuntimeException("Generic Fetcher misconfiguration, missinh data handler")
            }

            val endPoint = dataFetcherConfiguration.streamEndPoint
            if (endPoint == null || endPoint.baseUrl == null || endPoint.path == null)
                throw RuntimeException("Custom Fetcher misconfigured, missing path and base URL")

            var params = HashMap<String, String>()
            params.putAll(endPoint.queryMap!!)

            // TODO - sort of too specific to declare in interface, need to decide
            // if (dataProcessingHandler is BaseLegacyHandler) {
            //     (dataProcessingHandler as BaseLegacyHandler).addBucketInfo(params)
            // }

            // Some fetchers may have different endpoint for getting "more" pages. Some use the same endpoint
            // in this case morePath shold not be set
            if (isFirstRequest || endPoint.morePath == null) {
                dataRequest?.fetchRemoteData(endPoint.baseUrl, endPoint.path,
                        headers = endPoint.headerMap as HashMap<String, String>,
                        params = endPoint.queryMap as HashMap<String, String>,
                        callback = this@GenericDataFetcher, POJOClassType = pojoClass)
            } else {
                // Different path for getMore

                // add pagination parameters
                dataProcessingHandler.applyPagination(params)

                dataRequest?.fetchRemoteData(endPoint.baseUrl, endPoint.morePath,
                        headers = endPoint.headerMap as HashMap<String, String>,
                        params = params,
                        callback = this@GenericDataFetcher, POJOClassType = pojoClass)
            }
        }
    }

    override fun getUUID(): UUID {
        return mUUID
    }

    override fun setConfig(config: IStreamConfig) {
        streamConfig = config
    }

    override fun getConfig(): IStreamConfig {
        return streamConfig
    }

    override fun setHandler(handler: BaseHandler) {
        responseHandler = handler
    }

    override fun removeHandler() {
        responseHandler = null
    }

    override fun getHandler(): BaseHandler? {
        return responseHandler
    }

    override fun getData(): List<IData> {
        return streamResponse ?: emptyList()
    }

    override fun clearData() {
        streamResponse = emptyList()
    }

    override fun isAdsFetcher(): Boolean {
        return false
    }

    override fun isSMAdsFetcher(): Boolean {
        return false
    }

    override fun reset() {
        clearData()
    }

    override fun getHash(): Int {
        return getUUID().hashCode()
    }

    override fun hasMore(): Boolean {
        return if (dataProcessingHandler != null) dataProcessingHandler.moreDataAvailable() else false
    }

    override fun isNext(): Boolean {
        return false
    }

    override suspend fun onDataFetchComplete(
        remoteData: IGenericData<Any>?,
        success: Boolean,
        statusCode: Int,
        statusMessage: String,
        errorResponse: String?,
        contentType: MediaType?,
        headers: Headers?
    ) {

        // let client ahndle first
        dataProcessingHandler?.onRemoteDataReady(remoteData, true, statusCode, statusMessage, errorResponse)
        // call onRequestDone afterwards
        onRequestDone(success, statusCode, statusMessage)
    }

    private fun onRequestDone(isSuccessful: Boolean, statusCode: Int?, statusMessage: String?) {
        val duration = System.currentTimeMillis() - startTime

//        Log.w("+++", "+++ GenericDataFetcher::onRequestDone(), isSuccessful: $isSuccessful, errorCode: $statusCode, $statusMessage
//        dataProcessingHandler?.getRemoteData().size: ${dataProcessingHandler?.getRemoteData()?.size}")

        if (isSuccessful) {

            NetworkTrackingUtils.logRequestTime(NetworkTrackingConstants.ACTION_LOAD_INITIAL,
                    reqId = getUUID().toString(), assetType = NetworkTrackingConstants.ASSET_STREAM,
                    count = streamResponse?.size ?: 0,
                    retryCount = 0,
                    duration = duration)

            firstRequestDone = true
            isFirstRequest = false
            streamResponse = dataProcessingHandler?.getRemoteData()
            responseHandler?.onComplete(DataFetchState(isSuccessful, (statusCode
                    ?: NetworkConstants.REQUEST_SUCCESS), (statusMessage ?: "")))
        } else {

            NetworkTrackingUtils.logRequestFailure(action = (if (firstRequestDone) NetworkTrackingConstants.ACTION_LOAD_NEXT else NetworkTrackingConstants.ACTION_LOAD_INITIAL), reqId = getUUID().toString(),
                    retryCount = 0, isTimeout = (statusCode == 504), errorCode = statusCode?.toString(), errorDesc = statusMessage, errorAssetType = NetworkTrackingConstants.ASSET_STREAM, errorType = NetworkTrackingConstants.ERROR_STREAM)
            responseHandler?.onComplete(DataFetchState(false, (statusCode
                    ?: -1), statusMessage))
        }
    }
}
