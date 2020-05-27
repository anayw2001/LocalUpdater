package com.statix.updater.model

import java.io.File

/** Represents an A/B update  */
class ABUpdate(var update: File) {
    var state: Int = 0
    var progress = 0
    val updatePath: String = update.absolutePath
}