package com.doubleplay.data.command

import android.util.Log
import com.doubleplay.data.di.DataFetcherComponentInjector
import com.doubleplay.data.interfaces.IHandler
import com.doubleplay.data.interfaces.IRemoteData
import com.doubleplay.data.interfaces.IRemoteRepositoryService
import com.doubleplay.data.model.RemoteData
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.lang.ref.WeakReference
import java.util.HashMap

/**
 * Created by linma9 on 4/3/18.
 */

/**
 * FetchRemoteDataCommand
 * taking params for making the remote request
 *
 * dataReadyCb: the callback which will receive IRemoteData<T>
 * POJOClassType: the class type of the T
 */
class FetchRemoteDataCommand<T>(scheme: String, authourity: String, path: String,
                             headers: HashMap<String, String>,
                             params: HashMap<String, String>,
                             dataReadyCb: IHandler<IRemoteData<T>>, POJOClassType: Class<T>) : CommandBase() {


    private val mBaseUrl = "$scheme://$authourity/"
    private val mHeaders = headers
    private val mPath = path
    private val mParams = params

    private var disposeable : Disposable? = null
    private var mDataReadyListener : WeakReference<IHandler<IRemoteData<T>>>? = null

    val mPOJOClazz: Class<T>

    /**
     * TODO: remove the following comment
     *
     * the (this.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>
     *     approach does not work
     *     and better approach is passing a resolved class type instead of trying to get it at runtime
     *
     * https://stackoverflow.com/questions/3437897/how-to-get-a-class-instance-of-generics-type-t
     *
     * A popular solution to this is to pass the Class of the type parameter into the constructor of the generic type, e.g.

        class Foo<T> {
            final Class<T> typeParameterClass;

            public Foo(Class<T> typeParameterClass) {
            this.typeParameterClass = typeParameterClass;
            }

            public void bar() {
            // you can access the typeParameterClass here and do whatever you like
            }
        }
     *
     * A standard approach/workaround/solution is to add a class object to the constructor(s), like:

        public class Foo<T> {

            private Class<T> type;
                public Foo(Class<T> type) {
                this.type = type;
            }

            public Class<T> getType() {
                return type;
            }

            public T newInstance() {
                return type.newInstance();
            }
        }
     */

    init {
        mDataReadyListener = WeakReference<IHandler<IRemoteData<T>>>(dataReadyCb)
        mPOJOClazz = POJOClassType
    }

    override fun dispose() {
        super.dispose()
        mDataReadyListener = null
        disposeable?.dispose()
        disposeable = null
    }

    override fun execute() {
        getRemoteData(object : Observer<Response<ResponseBody>> {
            override fun onNext(response: Response<ResponseBody>) {
                val remoteData = RemoteData<T>(response, mPOJOClazz)
                val cb = mDataReadyListener?.get()
                cb?.onRemoteDataReady(remoteData, IHandler.STATE_DATA_READY, "")
            }

            override fun onComplete() {
                val cb = mDataReadyListener?.get()
                cb?.onRemoteDataReady(null, IHandler.STATE_COMPLETE, "")
                dispose()
            }

            override fun onError(e: Throwable) {
                val cb = mDataReadyListener?.get()
                cb?.onRemoteDataReady(null, IHandler.STATE_ERROR, e.toString())
                dispose()
            }

            override fun onSubscribe(d: Disposable) {
                disposeable = d
            }
        })
    }

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
}