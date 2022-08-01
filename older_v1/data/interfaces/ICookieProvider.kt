package com.doubleplay.data.interfaces

import android.net.Uri

interface ICookieProvider {
    abstract fun getCookies(uri: Uri): String

    abstract fun getAllCookies(): String

    abstract fun getBCookie(): String
}