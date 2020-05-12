package com.statix.updater

import android.content.Context
import android.os.AsyncTask
import android.os.UpdateEngine
import android.os.UpdateEngineCallback
import android.util.Log
import com.statix.updater.misc.Constants
import com.statix.updater.misc.Utilities
import com.statix.updater.model.ABUpdate
import java.io.IOException

internal class ABUpdateHandler private constructor(private val mUpdate: ABUpdate, private val mContext: Context, private val mController: MainViewController) {
    private var mBound = false
    private val mUpdateEngine: UpdateEngine

    @Synchronized
    fun handleUpdate() {
        if (!mBound) {
            mBound = mUpdateEngine.bind(mUpdateEngineCallback)
        }
        AsyncTask.execute {
            try {
                mController.notifyUpdateStatusChanged(mUpdate, Constants.PREPARING_UPDATE)
                Utilities.copyUpdate(mUpdate)
                Log.d(TAG, mUpdate.update().toString())
                val payloadProperties = Utilities.getPayloadProperties(mUpdate.update())
                val offset = Utilities.getZipOffset(mUpdate.updatePath)
                val zipFileUri = "file://" + mUpdate.updatePath
                mUpdate.setState(Constants.UPDATE_IN_PROGRESS)
                mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_IN_PROGRESS)
                Log.d(TAG, "Applying payload")
                Utilities.putPref(Constants.PREF_INSTALLING_AB, true, mContext)
                mUpdateEngine.applyPayload(zipFileUri, offset, 0, payloadProperties)
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Unable to extract update.")
                mUpdate.setState(Constants.UPDATE_FAILED)
                mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FAILED)
            }
        }
    }

    fun reconnect() {
        if (!mBound) {
            mBound = mUpdateEngine.bind(mUpdateEngineCallback)
            Log.d(TAG, "Reconnected to update engine")
        }
    }

    fun suspend() {
        mUpdateEngine.suspend()
        Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, true, mContext)
        Utilities.putPref(Constants.PREF_INSTALLING_AB, false, mContext)
        mUpdate.setState(Constants.UPDATE_PAUSED)
    }

    fun resume() {
        mUpdateEngine.resume()
        Utilities.putPref(Constants.PREF_INSTALLING_AB, true, mContext)
        Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, false, mContext)
        mUpdate.setState(Constants.UPDATE_IN_PROGRESS)
    }

    fun cancel() {
        Utilities.putPref(Constants.PREF_INSTALLED_AB, false, mContext)
        Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, false, mContext)
        Utilities.putPref(Constants.PREF_INSTALLING_AB, false, mContext)
        mUpdateEngine.cancel()
        mUpdate.setState(Constants.UPDATE_STOPPED)
    }

    fun unbind() {
        mBound = !mUpdateEngine.unbind()
        Log.d(TAG, "Unbound callback from update engine")
    }

    private val mUpdateEngineCallback: UpdateEngineCallback = object : UpdateEngineCallback() {
        override fun onStatusUpdate(status: Int, percent: Float) {
            when (status) {
                UpdateEngine.UpdateStatusConstants.REPORTING_ERROR_EVENT -> {
                    mUpdate.setState(Constants.UPDATE_FAILED)
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FAILED)
                    mUpdate.progress = Math.round(percent * 100)
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_IN_PROGRESS)
                }
                UpdateEngine.UpdateStatusConstants.DOWNLOADING -> {
                    mUpdate.progress = Math.round(percent * 100)
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_IN_PROGRESS)
                }
                UpdateEngine.UpdateStatusConstants.FINALIZING -> {
                    mUpdate.progress = Math.round(percent * 100)
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FINALIZING)
                }
                UpdateEngine.UpdateStatusConstants.VERIFYING -> {
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_VERIFYING)
                    mUpdate.setState(Constants.UPDATE_VERIFYING)
                }
                UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT -> {
                    mUpdate.setState(Constants.UPDATE_SUCCEEDED)
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_SUCCEEDED)
                }
                UpdateEngine.UpdateStatusConstants.IDLE -> Utilities.cleanInternalDir()
            }
        }

        override fun onPayloadApplicationComplete(errorCode: Int) {
            if (errorCode != UpdateEngine.ErrorCodeConstants.SUCCESS) {
                mUpdate.progress = 0
                mUpdate.setState(Constants.UPDATE_FAILED)
                Utilities.putPref(Constants.PREF_INSTALLED_AB, false, mContext)
                Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, false, mContext)
                Utilities.putPref(Constants.PREF_INSTALLING_AB, false, mContext)
                mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FAILED)
            }
        }
    }

    fun setPerformanceMode(checked: Boolean) {
        mUpdateEngine.setPerformanceMode(checked)
        Utilities.putPref(Constants.ENABLE_AB_PERF_MODE, checked, mContext)
    }

    companion object {
        private var sInstance: ABUpdateHandler? = null
        private const val TAG = "ABUpdateHandler"

        @Synchronized
        fun getInstance(update: ABUpdate, context: Context,
                        controller: MainViewController): ABUpdateHandler? {
            if (sInstance == null) {
                sInstance = ABUpdateHandler(update, context, controller)
            }
            return sInstance
        }
    }

    init {
        mUpdateEngine = UpdateEngine()
    }
}