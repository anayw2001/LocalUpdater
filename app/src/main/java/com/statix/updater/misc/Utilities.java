package com.statix.updater.misc;

import android.content.Context;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.statix.updater.model.ABUpdate;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utilities {

    public static File[] lsFiles(File dir) {
        return dir.getAbsoluteFile().listFiles();
    }

    public static boolean isUpdate(File update) {
        String updateName = update.getName();
        // current build properties
        String currentBuild = SystemProperties.get(Constants.STATIX_VERSION_PROP);
        String[] currentSplit = currentBuild.split("-");
        String device = SystemProperties.get(Constants.DEVICE_PROP);
        String buildPrefix = Constants.ROM + "_" + device;
        double version = Double.parseDouble(currentBuild.substring(1, 4));
        String variant = SystemProperties.get(Constants.STATIX_BUILD_TYPE_PROP);
        // upgrade build properties
        String[] split = updateName.split("-");
        Log.d("Updater", java.util.Arrays.toString(split));
        String upgradePrefix = split[0];
        double upgradeVersion = Double.parseDouble(split[4].substring(1));
        String upgradeVariant = split[5].split("\\.")[0];
        boolean prefixes = buildPrefix.equals(upgradePrefix);
        boolean versionUpgrade = upgradeVersion >= version;
        boolean sameVariant = upgradeVariant.equals(variant);
        return prefixes && versionUpgrade && sameVariant;
    }

    public static File copyUpdate(File source) throws IOException {
        File dest = new File("/data/statix_updates/" + source.getName());
        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            return dest;
        }
    }

    public static ABUpdate checkForUpdates(Context context) {
        File[] updates = lsFiles(context.getExternalFilesDir(null));
        Log.d("Updater", java.util.Arrays.toString(updates));
        if (updates != null) {
            for (File update : updates) {
                if (isUpdate(update)) {
                    return new ABUpdate(update);
                }
            }
        }
        return null;
    }

    public static int getSystemAccent(AppCompatActivity base) {
        TypedValue typedValue = new TypedValue();
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(base,
                android.R.style.Theme_DeviceDefault);
        contextThemeWrapper.getTheme().resolveAttribute(android.R.attr.colorAccent,
                typedValue, true);
        return typedValue.data;
    }

    public static String[] getPayloadProperties(File update) {
        String[] headerKeyValuePairs;
        try {
            ZipFile zipFile = new ZipFile(update);
            ZipEntry payloadPropEntry = zipFile.getEntry("payload_properties.txt");
            try (InputStream is = zipFile.getInputStream(payloadPropEntry);
                 InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader br = new BufferedReader(isr)) {
                 List<String> lines = new ArrayList<>();
                 for (String line; (line = br.readLine()) != null;) {
                     lines.add(line);
                 }
                 headerKeyValuePairs = new String[lines.size()];
                 headerKeyValuePairs = lines.toArray(headerKeyValuePairs);
            }
            zipFile.close();
            return headerKeyValuePairs;
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    public static long getZipOffset(String zipFilePath) throws IOException {
        ZipFile zipFile = new ZipFile(zipFilePath);
        // Each entry has an header of (30 + n + m) bytes
        // 'n' is the length of the file name
        // 'm' is the length of the extra field
        final int FIXED_HEADER_SIZE = 30;
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        long offset = 0;
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            int n = entry.getName().length();
            int m = entry.getExtra() == null ? 0 : entry.getExtra().length;
            int headerSize = FIXED_HEADER_SIZE + n + m;
            offset += headerSize;
            if (entry.getName().equals("payload.bin")) {
                return offset;
            }
            offset += entry.getCompressedSize();
        }
        Log.e("ABUpdater", "payload.bin not found");
        throw new IllegalArgumentException("The given entry was not found");
    }

    public static void cleanUpdateDir(Context context) {
        File[] updateDirPush = lsFiles(context.getExternalFilesDir(null));
        if (updateDirPush != null) {
            for (File f : updateDirPush) {
                f.delete();
            }
        }
    }

    public static void putPref(String preference, boolean newValue, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(preference, newValue)
                .apply();
    }

    public static void resetPreferences(Context context) {
        for (String pref : Constants.PREFS_LIST) {
            putPref(pref, false, context);
        }
    }
}
