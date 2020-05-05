package com.statix.updater;

import android.content.Context;
import android.os.AsyncTask;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import com.statix.updater.misc.Constants;
import com.statix.updater.misc.Utilities;
import com.statix.updater.model.ABUpdate;

class ABUpdateHandler {

    private boolean mBound;
    private ABUpdate mUpdate;
    private Context mContext;
    private MainViewController mController;
    private UpdateEngine mUpdateEngine;

    private static ABUpdateHandler sInstance = null;

    private static final String TAG = "ABUpdateHandler";

    private ABUpdateHandler(File update, Context ctx, MainViewController controller) {
        mContext = ctx;
        mController = controller;
        mUpdate = new ABUpdate(update);
        mUpdateEngine = new UpdateEngine();
    }

    public static synchronized ABUpdateHandler getInstance(File update, Context context,
                                                           MainViewController controller) {
        if (sInstance == null) {
            sInstance = new ABUpdateHandler(update, context, controller);
        }
        return sInstance;
    }

    void handleUpdate() {
        if (!mBound) {
            mBound = mUpdateEngine.bind(mUpdateEngineCallback);
        }
        try {
            String[] payloadProperties = Utilities.getPayloadProperties(mUpdate.update());
            Log.d(TAG, java.util.Arrays.toString(payloadProperties));
            long offset = Utilities.getZipOffset(mUpdate.getUpdatePath());
            String zipFileUri = "file://" + mUpdate.getUpdatePath();
            mUpdate.setState(Constants.UPDATE_IN_PROGRESS);
            mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_IN_PROGRESS);
            Log.d(TAG, "Applying payload");
            mUpdateEngine.applyPayload(zipFileUri, offset, 0, payloadProperties);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Unable to extract update.");
            mUpdate.setState(Constants.UPDATE_FAILED);
            mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FAILED);
        }
    }

    boolean isBound() {
        return mBound;
    }

    public void reconnect() {
        if (!mBound) {
            mBound = mUpdateEngine.bind(mUpdateEngineCallback);
        }
    }

    public void suspend() {
        mUpdateEngine.suspend();
        mUpdate.setState(Constants.UPDATE_PAUSED);
    }

    public void resume() {
        mUpdateEngine.resume();
        mUpdate.setState(Constants.UPDATE_IN_PROGRESS);
    }

    public void cancel() {
        mUpdateEngine.cancel();
        mUpdate.setState(Constants.UPDATE_STOPPED);
    }

    private final UpdateEngineCallback mUpdateEngineCallback = new UpdateEngineCallback() {
        @Override
        public void onStatusUpdate(int status, float percent) {
            switch (status) {
                case UpdateEngine.UpdateStatusConstants.REPORTING_ERROR_EVENT:
                    mUpdate.setState(Constants.UPDATE_FAILED);
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FAILED);
                case UpdateEngine.UpdateStatusConstants.DOWNLOADING:
                    mUpdate.setProgress(Math.round(percent * 100));
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_IN_PROGRESS);
                break;
                case UpdateEngine.UpdateStatusConstants.FINALIZING: {
                    mUpdate.setProgress(Math.round(percent * 100));
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FINALIZING);
                }
                break;
                case UpdateEngine.UpdateStatusConstants.VERIFYING:
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_VERIFYING);
                    mUpdate.setState(Constants.UPDATE_VERIFYING);
                break;
                case UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT: {
                    mUpdate.setState(Constants.UPDATE_SUCCEEDED);
                    mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_SUCCEEDED);
                }
                break;
                case UpdateEngine.UpdateStatusConstants.IDLE:
            }
        }

        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            if (errorCode != UpdateEngine.ErrorCodeConstants.SUCCESS) {
                mUpdate.setProgress(0);
                mUpdate.setState(Constants.UPDATE_FAILED);
                mController.notifyUpdateStatusChanged(mUpdate, Constants.UPDATE_FAILED);
            }
        }
    };
}
