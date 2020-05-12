package com.statix.updater

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemProperties
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.statix.updater.MainViewController.Companion.getInstance
import com.statix.updater.history.HistoryUtils
import com.statix.updater.history.HistoryView
import com.statix.updater.misc.Constants
import com.statix.updater.misc.Utilities
import com.statix.updater.model.ABUpdate
import org.json.JSONException
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), MainViewController.StatusListener {
    private var mUpdateHandler: ABUpdateHandler? = null
    private var mUpdate: ABUpdate? = null
    private var mUpdateControl: Button? = null
    private var mPauseResume: Button? = null
    private var mHistory: ImageButton? = null
    private var mController: MainViewController? = null
    private var mUpdateProgress: ProgressBar? = null
    private var mSharedPrefs: SharedPreferences? = null
    private var mABPerfMode: Switch? = null
    private var mCurrentVersionView: TextView? = null
    private var mUpdateProgressText: TextView? = null
    private var mUpdateView: TextView? = null
    private var mUpdateSize: TextView? = null
    private var mAccent = 0
    private val TAG = "Updater"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mController = getInstance(applicationContext)
        // set up views
        mUpdateProgress = findViewById<View>(R.id.progress_bar) as ProgressBar
        mUpdateProgress!!.visibility = View.INVISIBLE
        mUpdateView = findViewById<View>(R.id.update_view) as TextView
        mUpdateControl = findViewById<View>(R.id.update_control) as Button
        mPauseResume = findViewById<View>(R.id.pause_resume) as Button
        mHistory = findViewById<View>(R.id.history_view) as ImageButton
        mABPerfMode = findViewById<View>(R.id.perf_mode_switch) as Switch
        mCurrentVersionView = findViewById<View>(R.id.current_version_view) as TextView
        mUpdateProgressText = findViewById<View>(R.id.progressText) as TextView
        mUpdateSize = findViewById<View>(R.id.update_size) as TextView
        mAccent = Utilities.getSystemAccent(this)
        mUpdateControl!!.setBackgroundColor(mAccent)
        mCurrentVersionView!!.text = getString(R.string.current_version, SystemProperties.get(Constants.STATIX_VERSION_PROP))
        mHistory!!.setOnClickListener { v: View? ->
            Log.d(TAG, "History imagebutton clicked")
            val histIntent = Intent(applicationContext, HistoryView::class.java)
            startActivity(histIntent)
        }

        // set up prefs
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        // check for updoots in /sdcard/statix_updates
        mUpdate = Utilities.checkForUpdates(applicationContext)
        mUpdateControl!!.setOnClickListener { v: View? ->
            val buttonText = mUpdateControl!!.text.toString()
            val cancel = getString(R.string.cancel_update)
            val check = getString(R.string.check_for_update)
            val apply = getString(R.string.apply_update)
            if (buttonText == cancel) {
                mUpdateHandler!!.cancel()
                Log.d(TAG, "Update cancelled")
                mUpdateProgress!!.visibility = View.INVISIBLE
                mUpdateProgressText!!.visibility = View.INVISIBLE
                mPauseResume!!.visibility = View.INVISIBLE
                mUpdateControl!!.setText(R.string.reboot_device)
            } else if (buttonText == check) {
                mUpdate = Utilities.checkForUpdates(applicationContext)
                setUpView()
            } else if (buttonText == apply) {
                mUpdateHandler!!.handleUpdate()
                mUpdateControl!!.setText(R.string.cancel_update)
                mPauseResume!!.visibility = View.VISIBLE
                mPauseResume!!.setText(R.string.pause_update)
            } else { // reboot
                showRebootDialog()
            }
        }
        setUpView()
    }

    private fun setUpView() {
        if (mUpdate != null) {
            val updateHandler = ABUpdateHandler.getInstance(mUpdate, applicationContext, mController)
            mUpdateHandler = updateHandler
            mController!!.addUpdateStatusListener(this)
            if (mSharedPrefs!!.getBoolean(Constants.PREF_INSTALLING_SUSPENDED_AB, false)
                    || mSharedPrefs!!.getBoolean(Constants.PREF_INSTALLING_AB, false)
                    || mSharedPrefs!!.getBoolean(Constants.PREF_INSTALLED_AB, false)) {
                updateHandler.reconnect()
            }
            // ab perf switch
            mABPerfMode!!.visibility = View.VISIBLE
            mABPerfMode!!.isChecked = mSharedPrefs!!.getBoolean(Constants.ENABLE_AB_PERF_MODE, false)
            mABPerfMode!!.setOnClickListener { v: View? ->
                updateHandler.setPerformanceMode(mABPerfMode!!.isChecked)
            }
            updateHandler.setPerformanceMode(mABPerfMode!!.isChecked)
            // apply updoot button
            val updateText = getString(R.string.to_install, mUpdate!!.update().name)
            mUpdateView!!.text = updateText
            val updateSizeMB = getString(R.string.update_size, java.lang.Long.toString(mUpdate!!.update().length() / (1024 * 1024)))
            mUpdateSize!!.text = updateSizeMB
            mUpdateControl!!.setText(R.string.apply_update)
            mUpdateProgress!!.progressDrawable.setColorFilter(Utilities.getSystemAccent(this),
                    PorterDuff.Mode.SRC_IN)
            // pause/resume
            mPauseResume!!.setBackgroundColor(Utilities.getSystemAccent(this))
            mPauseResume!!.visibility = View.INVISIBLE
            mPauseResume!!.setOnClickListener { v: View? ->
                val updatePaused = mSharedPrefs!!.getBoolean(Constants.PREF_INSTALLING_SUSPENDED_AB, false)
                if (updatePaused) {
                    mPauseResume!!.setText(R.string.pause_update)
                    updateHandler.resume()
                    mUpdateProgress!!.visibility = View.VISIBLE
                } else {
                    mPauseResume!!.setText(R.string.resume_update)
                    updateHandler.suspend()
                }
            }
            setButtonVisibilities()
        } else {
            mUpdateView!!.setText(R.string.no_update_available)
            mUpdateControl!!.setText(R.string.check_for_update)
            mPauseResume!!.visibility = View.INVISIBLE
            mABPerfMode!!.visibility = View.INVISIBLE
        }
    }

    private fun setButtonVisibilities() {
        if (mSharedPrefs!!.getBoolean(Constants.PREF_INSTALLING_SUSPENDED_AB, false)) {
            mPauseResume!!.visibility = View.VISIBLE
            mPauseResume!!.setText(R.string.resume_update)
            mUpdateControl!!.setText(R.string.cancel_update)
        } else if (mSharedPrefs!!.getBoolean(Constants.PREF_INSTALLED_AB, false)) {
            mPauseResume!!.visibility = View.INVISIBLE
            mUpdateControl!!.setText(R.string.reboot_device)
            mUpdateProgressText!!.setText(R.string.update_complete)
        } else if (mSharedPrefs!!.getBoolean(Constants.PREF_INSTALLING_AB, false)) {
            mPauseResume!!.visibility = View.VISIBLE
            mPauseResume!!.setText(R.string.pause_update)
            mUpdateControl!!.setText(R.string.cancel_update)
            mUpdateProgress!!.visibility = View.VISIBLE
        }
        mABPerfMode!!.isChecked = mSharedPrefs!!.getBoolean(Constants.ENABLE_AB_PERF_MODE, false)
    }

    override fun onUpdateStatusChanged(update: ABUpdate?, state: Int) {
        val updateProgress = update!!.progress
        val f = File(Constants.HISTORY_PATH)
        mUpdate!!.setState(state)
        runOnUiThread {
            when (state) {
                Constants.PREPARING_UPDATE -> {
                    mUpdateProgressText!!.setText(R.string.preparing_update)
                    mUpdateProgress!!.visibility = View.INVISIBLE
                    mUpdateControl!!.visibility = View.INVISIBLE
                    update.progress = 0
                    mUpdateProgress!!.visibility = View.INVISIBLE
                    mUpdateProgressText!!.setText(R.string.reboot_try_again)
                    mUpdateControl!!.visibility = View.VISIBLE
                    mUpdateControl!!.setText(R.string.reboot_device)
                    mPauseResume!!.visibility = View.INVISIBLE
                    try {
                        HistoryUtils.writeUpdateToJson(f, mUpdate)
                    } catch (e: IOException) {
                        Log.e(TAG, "Unable to write to update history.")
                    } catch (e: JSONException) {
                        Log.e(TAG, "Unable to write to update history.")
                    }
                    Utilities.cleanInternalDir()
                }
                Constants.UPDATE_FAILED -> {
                    update.progress = 0
                    mUpdateProgress!!.visibility = View.INVISIBLE
                    mUpdateProgressText!!.setText(R.string.reboot_try_again)
                    mUpdateControl!!.visibility = View.VISIBLE
                    mUpdateControl!!.setText(R.string.reboot_device)
                    mPauseResume!!.visibility = View.INVISIBLE
                    try {
                        HistoryUtils.writeUpdateToJson(f, mUpdate)
                    } catch (e: IOException) {
                        Log.e(TAG, "Unable to write to update history.")
                    } catch (e: JSONException) {
                        Log.e(TAG, "Unable to write to update history.")
                    }
                    Utilities.cleanInternalDir()
                }
                Constants.UPDATE_FINALIZING -> {
                    mUpdateProgress!!.progress = updateProgress
                    mUpdateProgressText!!.text = getString(R.string.update_finalizing, Integer.toString(updateProgress))
                }
                Constants.UPDATE_IN_PROGRESS -> {
                    mUpdateControl!!.visibility = View.VISIBLE
                    mPauseResume!!.visibility = View.VISIBLE
                    mPauseResume!!.setText(R.string.pause_update)
                    mUpdateControl!!.setText(R.string.cancel_update)
                    mUpdateProgressText!!.visibility = View.VISIBLE
                    mUpdateProgressText!!.text = getString(R.string.installing_update, Integer.toString(updateProgress))
                    mUpdateProgress!!.visibility = View.VISIBLE
                    mUpdateProgress!!.progress = updateProgress
                }
                Constants.UPDATE_VERIFYING -> {
                    mPauseResume!!.visibility = View.INVISIBLE
                    mUpdateView!!.setText(R.string.verifying_update)
                    Utilities.cleanUpdateDir(applicationContext)
                    Utilities.cleanInternalDir()
                    try {
                        HistoryUtils.writeUpdateToJson(f, mUpdate)
                    } catch (e: IOException) {
                        Log.e(TAG, "Unable to write to update history.")
                    } catch (e: JSONException) {
                        Log.e(TAG, "Unable to write to update history.")
                    }
                    Utilities.putPref(Constants.PREF_INSTALLED_AB, true, applicationContext)
                    mPauseResume!!.visibility = View.INVISIBLE
                    mUpdateProgress!!.visibility = View.INVISIBLE
                    mUpdateProgressText!!.setText(R.string.update_complete)
                    mUpdateControl!!.setText(R.string.reboot_device)
                }
                Constants.UPDATE_SUCCEEDED -> {
                    Utilities.cleanUpdateDir(applicationContext)
                    Utilities.cleanInternalDir()
                    try {
                        HistoryUtils.writeUpdateToJson(f, mUpdate)
                    } catch (e: IOException) {
                        Log.e(TAG, "Unable to write to update history.")
                    } catch (e: JSONException) {
                        Log.e(TAG, "Unable to write to update history.")
                    }
                    Utilities.putPref(Constants.PREF_INSTALLED_AB, true, applicationContext)
                    mPauseResume!!.visibility = View.INVISIBLE
                    mUpdateProgress!!.visibility = View.INVISIBLE
                    mUpdateProgressText!!.setText(R.string.update_complete)
                    mUpdateControl!!.setText(R.string.reboot_device)
                }
            }
        }
    }

    public override fun onStop() {
        mController!!.removeUpdateStatusListener(this)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        val update = Utilities.checkForUpdates(applicationContext)
        if (update != null && update != mUpdate) {
            mUpdate = update
        }
        if (mController != null) {
            mController!!.addUpdateStatusListener(this)
        }
        if (mUpdateHandler != null) {
            mUpdateHandler!!.reconnect()
            Log.d(TAG, "Reconnected to update engine")
        }
        setButtonVisibilities()
    }

    override fun onPause() {
        if (mController != null) {
            mController!!.removeUpdateStatusListener(this)
        }
        if (mUpdateHandler != null) {
            mUpdateHandler!!.unbind()
            Log.d(TAG, "Unbound callback from update engine")
        }
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
                .setPositiveButton(R.string.ok) { dialog: DialogInterface?, id: Int -> rebootDevice() }
                .setNegativeButton(R.string.cancel, null).show()
    }
}