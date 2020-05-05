package com.statix.updater.history;

import android.content.Context;
import android.widget.ListView;

public class HistoryView extends ListView {

    private Context mContext;
    private HistoryController mHistoryController;

    public HistoryView(Context context) {
        super(context);
        mContext = context;
        mHistoryController = new HistoryController(context);
        mHistoryController.deserializeUpdates();
        setAdapter(mHistoryController);
    }
}
