package com.statix.updater.model

class HistoryCard(val updateName: String, private val mSuccessful: Boolean) : Comparable<Any?> {

    fun updateSucceeded(): Boolean {
        return mSuccessful
    }

    override fun compareTo(other: Any?): Int {
        val otherCard = other as HistoryCard?
        return updateName.compareTo(otherCard!!.updateName)
    }

}