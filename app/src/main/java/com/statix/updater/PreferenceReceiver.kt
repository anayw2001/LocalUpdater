package com.statix.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.statix.updater.misc.Utilities

class PreferenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Utilities.resetPreferences(context)
        Utilities.cleanInternalDir()
    }
}