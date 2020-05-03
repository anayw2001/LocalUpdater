package com.statix.updater;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.statix.updater.misc.Constants;
import com.statix.updater.misc.Utilities;
import com.statix.updater.model.ABUpdate;

public class MainActivity extends AppCompatActivity implements MainViewController.StatusListener {

    private ABUpdateHandler mUpdateHandler;
    private ABUpdate mUpdate;
    private Button mUpdateControl;
    private Button mPauseResume;
    private MainViewController mController;
    private ProgressBar mUpdateProgress;
    private TextView mCurrentVersionView;
    private TextView mUpdateProgressText;
    private TextView mUpdateView;
    private TextView mUpdateSize;
    private TextView mVersionView;

    private int mAccent;
    private final String TAG = "Updater";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mController = new MainViewController(getApplicationContext());
        // set up views
        mUpdateProgress = (ProgressBar) findViewById(R.id.progress_bar);
        mUpdateProgress.setVisibility(View.INVISIBLE);
        mUpdateView = (TextView) findViewById(R.id.update_view);
        mUpdateControl = (Button) findViewById(R.id.update_control);
        mPauseResume = (Button) findViewById(R.id.pause_resume);
        mCurrentVersionView = (TextView) findViewById(R.id.current_version_view);
        mUpdateProgressText = (TextView) findViewById(R.id.progressText);
        mUpdateSize = (TextView) findViewById(R.id.update_size);
        mVersionView = (TextView) findViewById(R.id.version_view);
        mAccent = Utilities.getSystemAccent(this);
        mUpdateControl.setBackgroundColor(mAccent);
        mCurrentVersionView.setText("Current version: " + SystemProperties.get(Constants.STATIX_VERSION_PROP));
        mVersionView.setText(SystemProperties.get(Constants.STATIX_VERSION_PROP).split("-")[0]);

        // check for updoots in /sdcard/statix_updates
        mUpdate = Utilities.checkForUpdates(getApplicationContext());

        mUpdateControl.setOnClickListener(v -> {
            String buttonText = mUpdateControl.getText().toString();
            String cancel = getString(R.string.cancel_update);
            String check = getString(R.string.check_for_update);
            String apply = getString(R.string.apply_update);
            if (buttonText.equals(cancel)) {
                mUpdateHandler.cancel();
                Log.d(TAG, "Update cancelled");
                mUpdateProgress.setVisibility(View.INVISIBLE);
                mUpdateProgressText.setVisibility(View.INVISIBLE);
                mPauseResume.setVisibility(View.INVISIBLE);
                mUpdateControl.setText(R.string.reboot_device);
            } else if (buttonText.equals(check)) {
                mUpdate = Utilities.checkForUpdates(getApplicationContext());
                setUpView();
            } else if (buttonText.equals(apply)){
                mUpdateHandler.handleUpdate();
                mUpdateControl.setText(R.string.cancel_update);
            } else { // reboot
                showRebootDialog();
            }
        });

        setUpView();
    }

    private void setUpView() {
        if (mUpdate != null) {
            mUpdateHandler = ABUpdateHandler.getInstance(mUpdate.update(), getApplicationContext(), mController);
            mController.addUpdateStatusListener(this);
            // apply updoot button
            String updateText = "Update zip to install: " + mUpdate.update().getName();
            mUpdateView.setText(updateText);
            String updateSizeMB = "Update size: " + mUpdate.update().length()/(1024*1024) + "M";
            mUpdateSize.setText(updateSizeMB);
            mUpdateControl.setText(R.string.apply_update);
            mUpdateProgress.getIndeterminateDrawable().setColorFilter(Utilities.getSystemAccent(this),
                android.graphics.PorterDuff.Mode.SRC_IN);
            // pause/resume
            mPauseResume.setBackgroundColor(Utilities.getSystemAccent(this));
            mPauseResume.setVisibility(View.INVISIBLE);
            mPauseResume.setOnClickListener(v -> {
                Log.d(TAG, "Pause/resume");
                boolean updatePaused = mUpdate.state() == Constants.UPDATE_PAUSED;
                if (updatePaused) {
                    mPauseResume.setText(R.string.resume_update);
                    Log.d(TAG, "Update paused");
                    mUpdateHandler.suspend();
                } else {
                    mPauseResume.setText(R.string.pause_update);
                    Log.d(TAG, "Update resumed");
                    mUpdateHandler.resume();
                }
            });
        } else {
            mUpdateView.setText(R.string.no_update_available);
            mUpdateControl.setText(R.string.check_for_update);
            mPauseResume.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUpdateStatusChanged(ABUpdate update, int state) {
        runOnUiThread(() -> {
            int updateProgress = mUpdate.getProgress();
            switch (state) {
                case Constants.UPDATE_FAILED:
                    mUpdate.setProgress(0);
                    mUpdateProgressText.setText("Update failed. Reboot to try again.");
                    mUpdateControl.setText(R.string.reboot_device);
                    mPauseResume.setVisibility(View.INVISIBLE);
                    break;
                case Constants.UPDATE_FINALIZING:
                    mUpdate.setProgress(100);
                    mUpdateProgressText.setText(R.string.update_finalizing);
                    break;
                case Constants.UPDATE_IN_PROGRESS:
                    mPauseResume.setVisibility(View.VISIBLE);
                    mPauseResume.setText(R.string.pause_update);
                    mUpdateControl.setText(R.string.cancel_update);
                    mUpdateProgressText.setText("Installing... " + Integer.toString(updateProgress) + "%");
                    mUpdateProgress.setVisibility(View.VISIBLE);
                    mUpdateProgress.setProgress(updateProgress);
                    break;
                case Constants.UPDATE_VERIFYING:
                    mPauseResume.setVisibility(View.INVISIBLE);
                    mUpdateView.setText(R.string.verifying_update);
                case Constants.UPDATE_SUCCEEDED:
                    mUpdate.update().delete();
                    mPauseResume.setVisibility(View.INVISIBLE);
                    mUpdateProgress.setVisibility(View.INVISIBLE);
                    mUpdateProgressText.setText(R.string.update_complete);
                    mUpdateControl.setText(R.string.reboot_device);
                    break;
            }
        });
    }

    @Override
    public void onStop() {
        mController.removeUpdateStatusListener(this);
        super.onStop();
    }

    private void rebootDevice() {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        pm.reboot("Update complete");
    }

    private void showRebootDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restart_title)
                .setMessage(R.string.reboot_message)
                .setPositiveButton(R.string.ok, (dialog, id) -> rebootDevice())
                .setNegativeButton(R.string.cancel, null).show();
    }
}
