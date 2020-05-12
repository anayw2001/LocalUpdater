package com.statix.updater.model

class HistoryCard(val updateName: String, private val mSuccessful: Boolean) {

    fun updateSucceeded(): Boolean {
        return mSuccessful
    }
}