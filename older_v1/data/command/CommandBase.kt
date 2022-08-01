package com.doubleplay.data.command

import com.doubleplay.data.interfaces.ICommand
import java.util.*

/**
 * Created by linma9 on 4/3/18.
 */

abstract class CommandBase : ICommand {

    companion object {
        private val mRequestHashMap : HashMap<UUID, ICommand> = HashMap<UUID, ICommand>()
        fun getAllRequest() : HashMap<UUID, ICommand>{
            return mRequestHashMap
        }
    }

    protected var mRequestId: UUID = UUID.randomUUID()
    var mDisposed = false
    protected open fun dispose() {
        if (!mDisposed) {
            mDisposed = true
            mRequestHashMap.remove(mRequestId)
        }
    }
}