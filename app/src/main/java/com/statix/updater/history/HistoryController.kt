package com.statix.updater.history

import android.content.res.Resources
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.statix.updater.R
import com.statix.updater.misc.Constants
import com.statix.updater.model.HistoryCard
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.util.*

class HistoryController(res: Resources) : BaseAdapter() {
    private var mCards: ArrayList<HistoryCard?>
    private val mResources: Resources
    val updates: Unit
        get() {
            val historyFile = File(Constants.HISTORY_PATH)
            try {
                mCards = HistoryUtils.readFromJson(historyFile)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to find previous updates")
            } catch (e: JSONException) {
                Log.e(TAG, "Unable to find previous updates")
            }
        }

    override fun getCount(): Int {
        return mCards.size
    }

    override fun getItem(position: Int): HistoryCard? {
        return mCards[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        val card = mCards[position]
        convertView.setBackgroundColor(if (card!!.updateSucceeded()) ResourcesCompat.getColor(mResources, R.color.update_successful, null) else ResourcesCompat.getColor(mResources, R.color.update_unsuccessful, null))
        val title = convertView.findViewById<TextView>(R.id.title)
        title.text = card.updateName
        val placeholder = mResources.getString(if (card.updateSucceeded()) R.string.succeeded else R.string.failed)
        val updateWasSuccessful = convertView.findViewById<TextView>(R.id.update_status)
        updateWasSuccessful.text = mResources.getString(R.string.update_status, placeholder)
        return convertView
    }

    companion object {
        private const val TAG = "HistoryController"
    }

    init {
        mCards = ArrayList()
        mResources = res
    }
}