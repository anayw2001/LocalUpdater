package com.statix.updater

import android.content.Context
import android.os.Handler
import android.os.PowerManager
import com.statix.updater.model.ABUpdate
import java.util.*

class MainViewController private constructor(private val mContext: Context) {
    private val TAG = "MainViewController"
    private val mUiThread: Handler
    private val mBgThread = Handler()
    private val mWakeLock: PowerManager.WakeLock
    private val mListeners: MutableList<StatusListener> = ArrayList()

    interface StatusListener {
        fun onUpdateStatusChanged(update: ABUpdate?, state: Int)
    }

    fun notifyUpdateStatusChanged(update: ABUpdate?, state: Int) {
        mUiThread.post {
            for (listener in mListeners) {
                listener.onUpdateStatusChanged(update, state)
            }
        }
    }

    fun addUpdateStatusListener(listener: StatusListener) {
        mListeners.add(listener)
    }

    fun removeUpdateStatusListener(listener: StatusListener) {
        mListeners.remove(listener)
    }

    companion object {
        private var sInstance: MainViewController? = null

        @JvmStatic
        @Synchronized
        fun getInstance(ctx: Context): MainViewController? {
            if (sInstance == null) {
                sInstance = MainViewController(ctx)
            }
            return sInstance
        }
    }

    init {
        val pm = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        mUiThread = Handler(mContext.mainLooper)
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Updater")
        mWakeLock.setReferenceCounted(false)
    }
}