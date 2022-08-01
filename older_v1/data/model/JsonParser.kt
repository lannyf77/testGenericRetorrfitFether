package com.doubleplay.data.model

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.IOException
import java.io.StringReader

class JsonParser {
    companion object {
        private val TAG = JsonParser::class.java.simpleName
        internal var STRING_LIMIT = 1024 * 1024

        fun<T> parse(jsonString: String, dataClassType: Class<T>)  : T? {
            var ret: T? = null
            var stringReader: StringReader? = null
            var jsonReader: JsonReader? = null
            try {
                if (!TextUtils.isEmpty(jsonString)) {
                    val g = Gson()

                    /**
                     * g.fromJson might cause OOM when string is large
                     * per doc: https://sites.google.com/site/gson/streaming
                     * Because the streams operate on one token at a time, they impose minimal memory overhead.
                     * Most applications should use only the object model API. JSON streaming is useful in just a few situations:
                     * When it is impossible or undesirable to load the entire object model into memory. This is most relevant on mobile platforms where memory is limited.
                     * When it is necessary to read or write a document before it is completely available.
                     *
                     * */

                    if (jsonString.length > STRING_LIMIT) {
                        stringReader = StringReader(jsonString)
                        jsonReader = JsonReader(stringReader)
                        ret = g.fromJson<T>(jsonReader, dataClassType)
                    } else {
                        ret = g.fromJson<T>(jsonString, dataClassType)
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            } catch (oom: OutOfMemoryError) {
                // if it still happen, throw a regular exception
                // to let app side behavior same as other unexpected error without crash the app
                // and report to crashlytics
                val errorMsg = "JsonParser:parse($dataClassType), OutOfMemoryError. " + getUsedMemorySizeText()
                System.gc()
                throw RuntimeException(errorMsg)
            } finally {
                if (jsonReader != null) {
                    try {//request by the api
                        jsonReader.close()
                    } catch (e: IOException) {}
                }
                if (stringReader != null) {
                    stringReader.close()
                }
            }
            return ret
        }

        fun getUsedMemorySizeText(): String {
            var ret = ""

            var freeSize = 0L
            var totalSize = 0L
            var usedSize = -1L
            try {
                val runtime = Runtime.getRuntime()
                freeSize = runtime.freeMemory()
                totalSize = runtime.totalMemory()
                usedSize = (totalSize - freeSize) / 1048576L

                val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
                ret = "runtime.maxMemory:" + maxHeapSizeInMB + "MB/used:" + usedSize + "MB"

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return ret
        }
    }
}