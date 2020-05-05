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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.statix.updater.history.HistoryUtils;
import com.statix.updater.history.HistoryView;
import com.statix.updater.misc.Constants;
import com.statix.updater.misc.Utilities;
import com.statix.updater.model.ABUpdate;

import java.io.File;

public class MainActivity extends AppCompatActivity implements MainViewController.StatusListener {

    private ABUpdateHandler mUpdateHandler;
    private ABUpdate mUpdate;
    private Button mUpdateControl;
    private Button mPauseResume;
    private ImageButton mHistory;
    private MainViewController mController;
    private ProgressBar mUpdateProgress;
    private TextView mCurrentVersionView;
    private TextView mUpdateProgressText;
    private TextView mUpdateView;
    private TextView mUpdateSize;

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
        mHistory = (ImageButton) findViewById(R.id.history_view);
        mCurrentVersionView = (TextView) findViewById(R.id.current_version_view);
        mUpdateProgressText = (TextView) findViewById(R.id.progressText);
        mUpdateSize = (TextView) findViewById(R.id.update_size);
        mAccent = Utilities.getSystemAccent(this);
        mUpdateControl.setBackgroundColor(mAccent);
        mCurrentVersionView.setText(getString(R.string.current_version, SystemProperties.get(Constants.STATIX_VERSION_PROP)));
        mHistory.setOnClickListener(v -> new HistoryView(getApplicationContext()));

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
            String updateText = getString(R.string.to_install, mUpdate.update().getName());
            mUpdateView.setText(updateText);
            String updateSizeMB = getString(R.string.update_size, Long.toString(mUpdate.update().length()/(1024*1024)));
            mUpdateSize.setText(updateSizeMB);
            mUpdateControl.setText(R.string.apply_update);
            mUpdateProgress.getIndeterminateDrawable().setColorFilter(Utilities.getSystemAccent(this),
                android.graphics.PorterDuff.Mode.SRC_IN);
            // pause/resume
            mPauseResume.setBackgroundColor(Utilities.getSystemAccent(this));
            mPauseResume.setVisibility(View.INVISIBLE);
            mPauseResume.setOnClickListener(v -> {
                boolean updatePaused = mUpdate.state() == Constants.UPDATE_PAUSED;
                if (updatePaused) {
                    mPauseResume.setText(R.string.resume_update);
                    mUpdateHandler.suspend();
                } else {
                    mPauseResume.setText(R.string.pause_update);
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
            int updateProgress = update.getProgress();
            File f = new File(Constants.HISTORY_PATH + update.update().getName());
            switch (state) {
                case Constants.UPDATE_FAILED:
                    update.setProgress(0);
                    mUpdateProgress.setVisibility(View.INVISIBLE);
                    mUpdateProgressText.setText("Update failed. Reboot to try again.");
                    mUpdateControl.setText(R.string.reboot_device);
                    mPauseResume.setVisibility(View.INVISIBLE);
                    HistoryUtils.writeObject(f, mUpdate);
                    break;
                case Constants.UPDATE_FINALIZING:
                    update.setState(state);
                    mUpdateProgressText.setText(getString(R.string.update_finalizing, updateProgress));
                    break;
                case Constants.UPDATE_IN_PROGRESS:
                    mPauseResume.setVisibility(View.VISIBLE);
                    mPauseResume.setText(R.string.pause_update);
                    mUpdateControl.setText(R.string.cancel_update);
                    mUpdateProgressText.setText(getString(R.string.installing_update, Integer.toString(updateProgress*100)));
                    mUpdateProgress.setVisibility(View.VISIBLE);
                    mUpdateProgress.setProgress(updateProgress);
                    break;
                case Constants.UPDATE_VERIFYING:
                    mPauseResume.setVisibility(View.INVISIBLE);
                    mUpdateView.setText(R.string.verifying_update);
                case Constants.UPDATE_SUCCEEDED:
                    update.update().delete();
                    HistoryUtils.writeObject(f, mUpdate);
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
