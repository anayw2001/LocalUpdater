package com.statix.updater.model

import java.io.File

/** Represents an A/B update  */
class ABUpdate(private var mUpdate: File) {
    private var mState = 0
    var progress = 0
    fun update(): File {
        return mUpdate
    }

    fun state(): Int {
        return mState
    }

    fun setState(state: Int) {
        mState = state
    }

    fun setUpdate(update: File) {
        mUpdate = update
    }

    val updatePath: String
        get() = mUpdate.absolutePath

}