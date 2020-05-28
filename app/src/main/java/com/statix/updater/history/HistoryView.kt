package com.statix.updater.history

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ListView
import com.statix.updater.R

class HistoryView : Activity() {
    private lateinit var context: Context
    private lateinit var historyController: HistoryController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.history_title)
        addContentView(HistoryList(applicationContext),
                ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private inner class HistoryList(context: Context) : ListView(context) {
        init {
            this@HistoryView.context = context
            historyController = HistoryController(resources)
            historyController.getUpdates()
            adapter = historyController
        }
    }
}