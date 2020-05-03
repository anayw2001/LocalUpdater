package com.statix.updater.misc;

public class Constants {

    // A/B constants
    public static final int BUFFER_SIZE = 4096;

    // Update constants
    public static String ROM = "statix";
    public static String DEVICE_PROP = "ro.product.device";
    public static String STATIX_VERSION_PROP = "ro.statix.version";
    public static String STATIX_BUILD_TYPE_PROP = "ro.statix.buildtype";

    // Status constants
    public static final int UPDATE_FINALIZING = 0;
    public static final int UPDATE_STOPPED = 1;
    public static final int UPDATE_PAUSED = 2;
    public static final int UPDATE_FAILED = 3;
    public static final int UPDATE_SUCCEEDED = 4;
    public static final int UPDATE_IN_PROGRESS = 5;
    public static final int UPDATE_VERIFYING = 6;

}
