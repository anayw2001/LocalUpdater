package com.statix.updater.model

import java.io.File

/** Represents an A/B update  */
class ABUpdate(private var mUpdate: File) {
    var state: Int = 0
        get() = field
        set(value) {
            field = value
        }

    var progress = 0
    fun update(): File {
        return mUpdate
    }

    fun setUpdate(update: File) {
        mUpdate = update
    }

    val updatePath: String
        get() = mUpdate.absolutePath

}