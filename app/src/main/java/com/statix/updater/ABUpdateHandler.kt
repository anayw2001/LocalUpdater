package com.statix.updater

import android.content.Context
import android.os.AsyncTask
import android.os.UpdateEngine
import android.os.UpdateEngineCallback
import android.util.Log
import com.statix.updater.misc.Constants
import com.statix.updater.misc.Utilities
import com.statix.updater.model.ABUpdate
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.roundToInt

internal class ABUpdateHandler private constructor(private val abUpdate: ABUpdate, private val context: Context, private val controller: MainViewController) {
    private var isBound = false
    private val updateEngine: UpdateEngine = UpdateEngine()
    private val coroutineScope = MainScope()

    @Synchronized
    fun handleUpdate() {
        if (!isBound) {
            isBound = updateEngine.bind(updateEngineCallback)
        }

        coroutineScope.launch {
            try {
                controller.notifyUpdateStatusChanged(abUpdate, Constants.PREPARING_UPDATE)
                Utilities.copyUpdate(abUpdate)
                Log.d(TAG, abUpdate.update.toString())
                val payloadProperties = Utilities.getPayloadProperties(abUpdate.update)
                val offset = Utilities.getZipOffset(abUpdate.updatePath)
                val zipFileUri = "file://" + abUpdate.updatePath
                abUpdate.state = Constants.UPDATE_IN_PROGRESS
                controller.notifyUpdateStatusChanged(abUpdate, Constants.UPDATE_IN_PROGRESS)
                Log.d(TAG, "Applying payload")
                Utilities.putPref(Constants.PREF_INSTALLING_AB, true, context)
                updateEngine.applyPayload(zipFileUri, offset, 0, payloadProperties)
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Unable to extract update.")
                abUpdate.state = Constants.UPDATE_FAILED
                controller.notifyUpdateStatusChanged(abUpdate, Constants.UPDATE_FAILED)
            }
        }
    }

    fun reconnect() {
        if (!isBound) {
            isBound = updateEngine.bind(updateEngineCallback)
            Log.d(TAG, "Reconnected to update engine")
        }
    }

    fun suspend() {
        updateEngine.suspend()
        Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, true, context)
        Utilities.putPref(Constants.PREF_INSTALLING_AB, false, context)
        abUpdate.state = Constants.UPDATE_PAUSED
    }

    fun resume() {
        updateEngine.resume()
        Utilities.putPref(Constants.PREF_INSTALLING_AB, true, context)
        Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, false, context)
        abUpdate.state = Constants.UPDATE_IN_PROGRESS
    }

    fun cancel() {
        Utilities.putPref(Constants.PREF_INSTALLED_AB, false, context)
        Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, false, context)
        Utilities.putPref(Constants.PREF_INSTALLING_AB, false, context)
        updateEngine.cancel()
        abUpdate.state = Constants.UPDATE_STOPPED
        coroutineScope.cancel()
    }

    fun unbind() {
        isBound = !updateEngine.unbind()
        Log.d(TAG, "Unbound callback from update engine")
    }

    private val updateEngineCallback: UpdateEngineCallback = object : UpdateEngineCallback() {
        override fun onStatusUpdate(status: Int, percent: Float) {
            when (status) {
                UpdateEngine.UpdateStatusConstants.REPORTING_ERROR_EVENT -> {
                    abUpdate.state = Constants.UPDATE_FAILED
                    controller.notifyUpdateStatusChanged(abUpdate, Constants.UPDATE_FAILED)
                    abUpdate.progress = (percent * 100).roundToInt()
                    controller.notifyUpdateStatusChanged(abUpdate, Constants.UPDATE_IN_PROGRESS)
                }
                UpdateEngine.UpdateStatusConstants.DOWNLOADING -> {
                    abUpdate.progress = (percent * 100).roundToInt()
                    controller.notifyUpdateStatusChanged(abUpdate, Constants.UPDATE_IN_PROGRESS)
                }
                UpdateEngine.UpdateStatusConstants.FINALIZING -> {
                    abUpdate.progress = (percent * 100).roundToInt()
                    controller.notifyUpdateStatusChanged(abUpdate, Constants.UPDATE_FINALIZING)
                }
                UpdateEngine.UpdateStatusConstants.VERIFYING -> {
                    controller.notifyUpdateStatusChanged(abUpdate, Constants.UPDATE_VERIFYING)
                    abUpdate.state = Constants.UPDATE_VERIFYING
                }
                UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT -> {
                    abUpdate.state = Constants.UPDATE_SUCCEEDED
                    controller.notifyUpdateStatusChanged(abUpdate, Constants.UPDATE_SUCCEEDED)
                }
                UpdateEngine.UpdateStatusConstants.IDLE -> Utilities.cleanInternalDir()
            }
        }

        override fun onPayloadApplicationComplete(errorCode: Int) {
            if (errorCode != UpdateEngine.ErrorCodeConstants.SUCCESS) {
                abUpdate.progress = 0
                abUpdate.state = Constants.UPDATE_FAILED
                Utilities.putPref(Constants.PREF_INSTALLED_AB, false, context)
                Utilities.putPref(Constants.PREF_INSTALLING_SUSPENDED_AB, false, context)
                Utilities.putPref(Constants.PREF_INSTALLING_AB, false, context)
                controller.notifyUpdateStatusChanged(abUpdate, Constants.UPDATE_FAILED)
            }
        }
    }

    fun setPerformanceMode(checked: Boolean) {
        updateEngine.setPerformanceMode(checked)
        Utilities.putPref(Constants.ENABLE_AB_PERF_MODE, checked, context)
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

}