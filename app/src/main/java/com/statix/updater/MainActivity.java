package com.statix.updater;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
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
    private TextView mUpdateProgressText;
    private TextView mUpdateView;
    private TextView mUpdateSize;

    private final String TAG = "Updater";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mController = new MainViewController(getApplicationContext());
        // set up views
        mUpdateProgress = findViewById(R.id.progress_bar);
        mUpdateProgress.setVisibility(View.INVISIBLE);
        mUpdateView = findViewById(R.id.update_view);
        mUpdateControl = findViewById(R.id.update_control);
        mPauseResume = findViewById(R.id.pause_resume);
        mUpdateProgressText = findViewById(R.id.progressText);
        mUpdateSize = findViewById(R.id.update_size);
        mUpdateControl.setBackgroundColor(Utilities.getSystemAccent(this));

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
            mUpdateView.setText(mUpdate.update().getName());
            String updateSizeMB = mUpdate.update().length()/(1024*1024) + "M";
            mUpdateSize.setText(updateSizeMB);
            mUpdateControl.setBackgroundColor(Utilities.getSystemAccent(this));
            mUpdateControl.setText(R.string.apply_update);
            // pause/resume
            mPauseResume.setBackgroundColor(Utilities.getSystemAccent(this));
            mPauseResume.setVisibility(View.INVISIBLE);
            mPauseResume.setOnClickListener(v -> {
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
            mUpdateProgress.setVisibility(View.VISIBLE);
            mUpdateProgress.setIndeterminate(true);
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
                    mUpdateProgressText.setText("0");
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
                    mUpdateProgress.setProgress(updateProgress);
                    break;
                case Constants.UPDATE_VERIFYING:
                    mPauseResume.setVisibility(View.INVISIBLE);
                    mUpdateView.setText(R.string.verifying_update);
                case Constants.UPDATE_SUCCEEDED:
                    mPauseResume.setVisibility(View.INVISIBLE);
                    mUpdateProgress.setVisibility(View.INVISIBLE);
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
        new AlertDialog.Builder(getApplicationContext())
                .setTitle(R.string.restart_title)
                .setPositiveButton(R.string.ok, (dialog, id) -> rebootDevice())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
