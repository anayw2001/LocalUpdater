package com.statix.updater.misc

import android.R
import android.content.Context
import android.os.FileUtils
import android.os.SystemProperties
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.statix.updater.model.ABUpdate
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.ArrayList
import java.util.zip.ZipFile

object Utilities {
    fun lsFiles(dir: File?): Array<File>? {
        return dir!!.absoluteFile.listFiles()
    }

    fun isUpdate(update: File): Boolean {
        val updateName = update.name
        // current build properties
        val currentBuild = SystemProperties.get(Constants.STATIX_VERSION_PROP)
        val device = SystemProperties.get(Constants.DEVICE_PROP)
        val buildPrefix = Constants.ROM + "_" + device
        val version = currentBuild.substring(1, 4).toDouble()
        val variant = SystemProperties.get(Constants.STATIX_BUILD_TYPE_PROP)
        // upgrade build properties
        val split = updateName.split("-").toTypedArray()
        val upgradePrefix = split[0]
        val upgradeVersion = split[4].substring(1).toDouble()
        val upgradeVariant = split[5].split("\\.").toTypedArray()[0]
        val prefixes = buildPrefix == upgradePrefix
        val versionUpgrade = upgradeVersion >= version
        val sameVariant = upgradeVariant == variant
        return prefixes && versionUpgrade && sameVariant
    }

    fun copyUpdate(source: ABUpdate) {
        val src = source.update()
        var name = src.name
        val pos = name.lastIndexOf(".")
        if (pos > 0) {
            name = name.substring(0, pos)
        }
        try {
            val dest = createNewFileWithPermissions(File(Constants.UPDATE_INTERNAL_DIR), name)
            val `is`: InputStream = FileInputStream(src)
            val os: OutputStream = FileOutputStream(dest)
            FileUtils.copy(`is`, os)
            source.setUpdate(dest)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Utilities", "Unable to copy update")
        }
    }

    fun checkForUpdates(context: Context): ABUpdate? {
        val updates = lsFiles(context.getExternalFilesDir(null))
        if (updates != null) {
            for (update in updates) {
                if (isUpdate(update)) {
                    return ABUpdate(update)
                }
            }
        }
        return null
    }

    fun getSystemAccent(base: AppCompatActivity?): Int {
        val typedValue = TypedValue()
        val contextThemeWrapper = ContextThemeWrapper(base,
                R.style.Theme_DeviceDefault)
        contextThemeWrapper.theme.resolveAttribute(R.attr.colorAccent,
                typedValue, true)
        return typedValue.data
    }

    fun getPayloadProperties(update: File?): Array<String?>? {
        var headerKeyValuePairs: Array<String?>? = null
        return try {
            val zipFile = ZipFile(update)
            val payloadPropEntry = zipFile.getEntry("payload_properties.txt")
            zipFile.getInputStream(payloadPropEntry).use { `is` ->
                InputStreamReader(`is`).use { isr ->
                    BufferedReader(isr).use { br ->
                        val lines: MutableList<String> = ArrayList()
                        var line: String
                        while (br.readLine().also { line = it } != null) {
                            lines.add(line)
                        }
                        headerKeyValuePairs = arrayOfNulls(lines.size)
                        headerKeyValuePairs = lines.toTypedArray()
                    }
                }
            }
            zipFile.close()
            headerKeyValuePairs
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    @Throws(IOException::class)
    fun getZipOffset(zipFilePath: String?): Long {
        val zipFile = ZipFile(zipFilePath)
        // Each entry has an header of (30 + n + m) bytes
        // 'n' is the length of the file name
        // 'm' is the length of the extra field
        val FIXED_HEADER_SIZE = 30
        val zipEntries = zipFile.entries()
        var offset: Long = 0
        while (zipEntries.hasMoreElements()) {
            val entry = zipEntries.nextElement()
            val n = entry.name.length
            val m = if (entry.extra == null) 0 else entry.extra.size
            val headerSize = FIXED_HEADER_SIZE + n + m
            offset += headerSize.toLong()
            if (entry.name == "payload.bin") {
                return offset
            }
            offset += entry.compressedSize
        }
        Log.e("ABUpdater", "payload.bin not found")
        throw IllegalArgumentException("The given entry was not found")
    }

    fun cleanUpdateDir(context: Context) {
        val updateDirPush = lsFiles(context.getExternalFilesDir(null))
        if (updateDirPush != null) {
            for (f in updateDirPush) {
                f.delete()
            }
        }
    }

    fun cleanInternalDir() {
        val updateDirInternal = lsFiles(File(Constants.UPDATE_INTERNAL_DIR))
        if (updateDirInternal != null) {
            for (f in updateDirInternal) {
                if (f.name != Constants.HISTORY_FILE) {
                    f.delete()
                }
            }
        }
    }

    fun putPref(preference: String?, newValue: Boolean, context: Context?) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(preference, newValue)
                .apply()
    }

    fun resetPreferences(context: Context?) {
        for (pref in Constants.PREFS_LIST) {
            putPref(pref, false, context)
        }
    }

    @Throws(IOException::class)
    private fun createNewFileWithPermissions(destination: File, name: String): File {
        val update = File.createTempFile(name, ".zip", destination)
        FileUtils.setPermissions( /* path= */
                update,  /* mode= */
                FileUtils.S_IRWXU or FileUtils.S_IRGRP or FileUtils.S_IROTH,  /* uid= */
                -1,  /* gid= */-1)
        return update
    }
}