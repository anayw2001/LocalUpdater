package com.statix.updater.history

import android.content.res.Resources
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.res.ResourcesCompat
import com.statix.updater.R
import com.statix.updater.misc.Constants
import com.statix.updater.model.HistoryCard
import kotlinx.android.synthetic.main.update_cardview.view.*
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.util.*

class HistoryController(res: Resources) : BaseAdapter() {
    private var cards: ArrayList<HistoryCard?>
    private val resources: Resources

    fun getUpdates() {
        val historyFile = File(Constants.HISTORY_PATH)
        try {
            cards = HistoryUtils.readFromJson(historyFile)
        } catch (e: IOException) {
            Log.e(TAG, "Unable to find previous updates")
        } catch (e: JSONException) {
            Log.e(TAG, "Unable to find previous updates")
        }
    }

    override fun getCount(): Int {
        return cards.size
    }

    override fun getItem(position: Int): HistoryCard? {
        return cards[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        val card = cards[position]
        convertView.setBackgroundColor(if (card!!.successful) ResourcesCompat.getColor(resources, R.color.update_successful, null) else ResourcesCompat.getColor(resources, R.color.update_unsuccessful, null))
        val title = convertView.title
        title.text = card.updateName
        val placeholder = resources.getString(if (card.successful) R.string.succeeded else R.string.failed)
        convertView.update_status.text = resources.getString(R.string.update_status, placeholder)
        return convertView
    }

    companion object {
        private const val TAG = "HistoryController"
    }

    init {
        cards = ArrayList()
        resources = res
    }
}