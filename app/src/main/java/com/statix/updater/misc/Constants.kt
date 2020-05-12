package com.statix.updater.misc

object Constants {
    // Data constants
    const val UPDATE_INTERNAL_DIR = "/data/statix_updates/"

    // Update constants
    @JvmField
    var ROM = "statix"
    @JvmField
    var DEVICE_PROP = "ro.product.device"
    @JvmField
    var STATIX_VERSION_PROP = "ro.statix.version"
    @JvmField
    var STATIX_BUILD_TYPE_PROP = "ro.statix.buildtype"

    // Status constants
    const val UPDATE_FINALIZING = 0
    const val UPDATE_STOPPED = 1
    const val UPDATE_PAUSED = 2
    const val UPDATE_FAILED = 3
    const val UPDATE_SUCCEEDED = 4
    const val UPDATE_IN_PROGRESS = 5
    const val UPDATE_VERIFYING = 6
    const val PREPARING_UPDATE = 7

    // Preference Constants
    const val PREF_INSTALLING_SUSPENDED_AB = "installation_suspended_ab"
    const val PREF_INSTALLING_AB = "installing_ab"
    const val PREF_INSTALLED_AB = "installed_ab"
    const val ENABLE_AB_PERF_MODE = "ab_perf_mode"
    @JvmField
    val PREFS_LIST = arrayOf(PREF_INSTALLING_SUSPENDED_AB, PREF_INSTALLED_AB, PREF_INSTALLING_AB)

    // History constants
    const val HISTORY_FILE = "history.json"
    const val HISTORY_PATH = UPDATE_INTERNAL_DIR + HISTORY_FILE
}