package com.statix.updater

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemProperties
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.statix.updater.MainViewController.Companion.getInstance
import com.statix.updater.history.HistoryUtils
import com.statix.updater.history.HistoryView
import com.statix.updater.misc.Constants
import com.statix.updater.misc.Utilities
import com.statix.updater.model.ABUpdate
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), MainViewController.StatusListener {

    private lateinit var updateHandler: ABUpdateHandler
    private var abUpdate: ABUpdate? = null
    private lateinit var controller: MainViewController
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        controller = getInstance(applicationContext)

        // set up views
        progress_bar!!.visibility = View.INVISIBLE
        current_version_view!!.text = getString(R.string.current_version, SystemProperties.get(Constants.STATIX_VERSION_PROP))
        history_view.setOnClickListener {
            Log.d(LOG_TAG, "History imagebutton clicked")
            val histIntent = Intent(applicationContext, HistoryView::class.java)
            startActivity(histIntent)
        }

        // set up prefs
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // check for updoots in /sdcard/statix_updates
        abUpdate = Utilities.checkForUpdates(applicationContext)
        update_control.setOnClickListener {
            val buttonText = update_control!!.text.toString()
            val cancel = getString(R.string.cancel_update)
            val check = getString(R.string.check_for_update)
            val apply = getString(R.string.apply_update)
            when (buttonText) {
                cancel -> {
                    updateHandler.cancel()
                    Log.d(LOG_TAG, "Update cancelled")
                    progress_bar.visibility = View.INVISIBLE
                    progressText.visibility = View.INVISIBLE
                    pause_resume.visibility = View.INVISIBLE
                    update_control.text = getString(R.string.reboot_device)
                }
                check -> {
                    abUpdate = Utilities.checkForUpdates(applicationContext)
                    setUpView()
                }
                apply -> {
                    updateHandler.handleUpdate()
                    update_control.text = getString(R.string.cancel_update)
                    pause_resume.visibility = View.VISIBLE
                    pause_resume.text = getString(R.string.pause_update)
                }
                else -> { // reboot
                    showRebootDialog()
                }
            }
        }
        setUpView()
    }

    private fun setUpView() {
        if (abUpdate != null) {
            val updateHandler = ABUpdateHandler.getInstance(abUpdate!!, applicationContext, controller)
            if (updateHandler != null) {
                this.updateHandler = updateHandler
            }
            controller.addUpdateStatusListener(this)
            if (sharedPrefs.getBoolean(Constants.PREF_INSTALLING_SUSPENDED_AB, false)
                    || sharedPrefs.getBoolean(Constants.PREF_INSTALLING_AB, false)
                    || sharedPrefs.getBoolean(Constants.PREF_INSTALLED_AB, false)) {
                updateHandler!!.reconnect()
            }
            // ab perf switch
            perf_mode_switch.visibility = View.VISIBLE
            perf_mode_switch.isChecked = sharedPrefs.getBoolean(Constants.ENABLE_AB_PERF_MODE, false)
            perf_mode_switch.setOnClickListener {
                updateHandler!!.setPerformanceMode(perf_mode_switch.isChecked)
            }
            updateHandler!!.setPerformanceMode(perf_mode_switch.isChecked)
            // apply updoot button
            val updateText = getString(R.string.to_install, abUpdate!!.update.name)
            update_view.text = updateText
            val updateSizeMB = getString(R.string.update_size, (abUpdate!!.update.length() / (1024 * 1024)).toString())
            update_size.text = updateSizeMB
            update_control.text = getString(R.string.apply_update)
            // pause/resume
            pause_resume!!.visibility = View.INVISIBLE
            pause_resume!!.setOnClickListener {
                val updatePaused = sharedPrefs.getBoolean(Constants.PREF_INSTALLING_SUSPENDED_AB, false)
                if (updatePaused) {
                    pause_resume!!.setText(R.string.pause_update)
                    updateHandler.resume()
                    progress_bar!!.visibility = View.VISIBLE
                } else {
                    pause_resume!!.setText(R.string.resume_update)
                    updateHandler.suspend()
                }
            }
            setButtonVisibilities()
        } else {
            update_view.text = getString(R.string.no_update_available)
            update_control.setText(R.string.check_for_update)
            pause_resume.visibility = View.INVISIBLE
            perf_mode_switch.visibility = View.INVISIBLE
        }
    }

    private fun setButtonVisibilities() {
        when {
            sharedPrefs.getBoolean(Constants.PREF_INSTALLING_SUSPENDED_AB, false) -> {
                pause_resume.visibility = View.VISIBLE
                pause_resume.text = getString(R.string.resume_update)
                update_control.text = getString(R.string.cancel_update)
            }
            sharedPrefs.getBoolean(Constants.PREF_INSTALLED_AB, false) -> {
                pause_resume.visibility = View.INVISIBLE
                update_control.text = getString(R.string.reboot_device)
                progressText.text = getString(R.string.update_complete)
            }
            sharedPrefs.getBoolean(Constants.PREF_INSTALLING_AB, false) -> {
                pause_resume.visibility = View.VISIBLE
                pause_resume.text = getString(R.string.pause_update)
                update_control.text = getString(R.string.cancel_update)
                progress_bar.visibility = View.VISIBLE
            }
        }
        perf_mode_switch.isChecked = sharedPrefs.getBoolean(Constants.ENABLE_AB_PERF_MODE, false)
    }

    override fun onUpdateStatusChanged(update: ABUpdate, state: Int) {
        val updateProgress = update.progress
        val f = File(Constants.HISTORY_PATH)
        abUpdate!!.state = update.state
        runOnUiThread {
            when (state) {
                Constants.PREPARING_UPDATE -> {
                    progressText.text = getString(R.string.preparing_update)
                    progress_bar.visibility = View.INVISIBLE
                    update_control.visibility = View.INVISIBLE
                    update.progress = 0
                    progress_bar.visibility = View.INVISIBLE
                    progressText.text = getString(R.string.reboot_try_again)
                    update_control.visibility = View.VISIBLE
                    update_control.text = getString(R.string.reboot_device)
                    pause_resume.visibility = View.INVISIBLE
                    try {
                        HistoryUtils.writeUpdateToJson(f, abUpdate!!)
                    } catch (e: IOException) {
                        Log.e(LOG_TAG, "Unable to write to update history.")
                    } catch (e: JSONException) {
                        Log.e(LOG_TAG, "Unable to write to update history.")
                    }
                    Utilities.cleanInternalDir()
                }
                Constants.UPDATE_FAILED -> {
                    update.progress = 0
                    this.progress_bar.visibility = View.INVISIBLE
                    progressText.text = getString(R.string.reboot_try_again)
                    update_control.visibility = View.VISIBLE
                    update_control.text = getString(R.string.reboot_device)
                    pause_resume.visibility = View.INVISIBLE
                    try {
                        HistoryUtils.writeUpdateToJson(f, abUpdate!!)
                    } catch (e: IOException) {
                        Log.e(LOG_TAG, "Unable to write to update history.")
                    } catch (e: JSONException) {
                        Log.e(LOG_TAG, "Unable to write to update history.")
                    }
                    Utilities.cleanInternalDir()
                }
                Constants.UPDATE_FINALIZING -> {
                    progress_bar.progress = updateProgress
                    progressText.text = getString(R.string.update_finalizing, updateProgress.toString())
                }
                Constants.UPDATE_IN_PROGRESS -> {
                    update_control.visibility = View.VISIBLE
                    pause_resume.visibility = View.VISIBLE
                    pause_resume.text = getString(R.string.pause_update)
                    update_control.text = getString(R.string.cancel_update)
                    progressText.visibility = View.VISIBLE
                    progressText.text = getString(R.string.installing_update, updateProgress.toString())
                    this.progress_bar.visibility = View.VISIBLE
                    this.progress_bar.progress = updateProgress
                }
                Constants.UPDATE_VERIFYING -> {
                    pause_resume.visibility = View.INVISIBLE
                    update_view.text = getString(R.string.verifying_update)
                    Utilities.cleanUpdateDir(applicationContext)
                    Utilities.cleanInternalDir()
                    try {
                        HistoryUtils.writeUpdateToJson(f, abUpdate!!)
                    } catch (e: IOException) {
                        Log.e(LOG_TAG, "Unable to write to update history.")
                    } catch (e: JSONException) {
                        Log.e(LOG_TAG, "Unable to write to update history.")
                    }
                    Utilities.putPref(Constants.PREF_INSTALLED_AB, true, applicationContext)
                    pause_resume.visibility = View.INVISIBLE
                    this.progress_bar.visibility = View.INVISIBLE
                    progressText.text = getString(R.string.update_complete)
                    update_control.text = getString(R.string.reboot_device)
                }
                Constants.UPDATE_SUCCEEDED -> {
                    Utilities.cleanUpdateDir(applicationContext)
                    Utilities.cleanInternalDir()
                    try {
                        HistoryUtils.writeUpdateToJson(f, abUpdate!!)
                    } catch (e: IOException) {
                        Log.e(LOG_TAG, "Unable to write to update history.")
                    } catch (e: JSONException) {
                        Log.e(LOG_TAG, "Unable to write to update history.")
                    }
                    Utilities.putPref(Constants.PREF_INSTALLED_AB, true, applicationContext)
                    pause_resume.visibility = View.INVISIBLE
                    this.progress_bar.visibility = View.INVISIBLE
                    progressText.text = getString(R.string.update_complete)
                    update_control.text = getString(R.string.reboot_device)
                }
            }
        }
    }

    public override fun onStop() {
        controller.removeUpdateStatusListener(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        val update = Utilities.checkForUpdates(applicationContext)
        if (update != null && update != abUpdate) {
            abUpdate = update
        }
        controller.addUpdateStatusListener(this)
        updateHandler.reconnect()
        Log.d(LOG_TAG, "Reconnected to update engine")
        setButtonVisibilities()
    }

    override fun onPause() {
        controller.removeUpdateStatusListener(this)
        updateHandler.unbind()
        Log.d(LOG_TAG, "Unbound callback from update engine")
        super.onPause()
    }

    private fun rebootDevice() {
        val pm = this.getSystemService(Context.POWER_SERVICE) as PowerManager
        Utilities.resetPreferences(applicationContext)
        pm.reboot("Update complete")
    }

    private fun showRebootDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.restart_title)
                .setMessage(R.string.reboot_message)
                .setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int -> rebootDevice() }
                .setNegativeButton(R.string.cancel, null).show()
    }

    companion object {
        private const val LOG_TAG = "Updater"
    }
}