package com.statix.updater.history

import android.util.Log
import com.statix.updater.misc.Constants
import com.statix.updater.model.ABUpdate
import com.statix.updater.model.HistoryCard
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.util.*

object HistoryUtils {
    @Synchronized
    @Throws(IOException::class, JSONException::class)
    fun writeUpdateToJson(historyFile: File, update: ABUpdate) {
        val updateSuccessful = update.state == Constants.UPDATE_SUCCEEDED
        val updateName = update.update.name
        val cards = readFromJson(historyFile)
        cards.add(HistoryCard(updateName, updateSuccessful))
        cards.sortBy { it!!.updateName }
        val cardMap = HashMap<String, Boolean>()

        // convert cards to a map
        for (card in cards) {
            cardMap[card!!.updateName] = card.successful
        }

        val toWrite = JSONObject(cardMap as Map<String, Boolean>)
        val write = toWrite.toString()
        Log.d("HistoryUtils", write)

        val fileWriter = FileWriter(historyFile)
        val bufferedWriter = BufferedWriter(fileWriter)
        bufferedWriter.write(write)
        bufferedWriter.close()
    }

    @Synchronized
    @Throws(IOException::class, JSONException::class)
    fun readFromJson(historyFile: File): ArrayList<HistoryCard?> {
        return if (historyFile.exists()) {
            val fr = FileReader(historyFile)
            val bufferedReader = BufferedReader(fr)
            val stringBuilder = StringBuilder()
            var line = bufferedReader.readLine()
            while (line != null) {
                stringBuilder.append(line).append("\n")
                line = bufferedReader.readLine()
            }
            bufferedReader.close()
            val updates = stringBuilder.toString()
            if (updates.isEmpty()) {
                return ArrayList()
            }
            val historyPairs = JSONObject(updates)
            val updateNames = historyPairs.names()!!
            val ret = ArrayList<HistoryCard?>()
            for (i in 0 until updateNames.length()) {
                val success = historyPairs.getBoolean(updateNames.getString(i))
                ret.add(HistoryCard(updateNames.getString(i), success))
            }
            ret.sortBy { it!!.updateName }
            ret.reverse()
            ret
        } else {
            historyFile.createNewFile()
            ArrayList()
        }
    }
}