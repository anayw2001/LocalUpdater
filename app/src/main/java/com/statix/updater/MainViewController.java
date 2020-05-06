package com.statix.updater;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.statix.updater.model.ABUpdate;

import java.util.ArrayList;
import java.util.List;

public class MainViewController {
    private final String TAG = "MainViewController";

    private final Context mContext;
    private Handler mUiThread;
    private Handler mBgThread = new Handler();
    private final LocalBroadcastManager mBroadcastManager;

    private static MainViewController sInstance = null;

    private final PowerManager.WakeLock mWakeLock;

    private List<StatusListener> mListeners = new ArrayList<>();

    public static synchronized MainViewController getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new MainViewController(ctx);
        }
        return sInstance;
    }

    private MainViewController(Context ctx) {
        mBroadcastManager = LocalBroadcastManager.getInstance(ctx);
        mContext = ctx;
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        mUiThread = new Handler(ctx.getMainLooper());
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Updater");
        mWakeLock.setReferenceCounted(false);
    }

    public interface StatusListener {
        void onUpdateStatusChanged(ABUpdate update, int state);
    }

    public void notifyUpdateStatusChanged(final ABUpdate update, final int state) {
        mUiThread.post(() -> {
            for (StatusListener listener : mListeners) {
                listener.onUpdateStatusChanged(update, state);
            }
        });
    }

    public void addUpdateStatusListener(StatusListener listener) {
        mListeners.add(listener);
    }

    public void removeUpdateStatusListener(StatusListener listener) {
        mListeners.remove(listener);
    }
}
