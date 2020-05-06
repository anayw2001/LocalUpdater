package com.statix.updater.history;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ListView;

import com.statix.updater.R;

public class HistoryView extends Activity {

    private Context mContext;
    private HistoryController mHistoryController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.history_title);
        addContentView(new HistoryList(getApplicationContext()),
            new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private class HistoryList extends ListView {
        public HistoryList(Context context) {
            super(context);
            mContext = context;
            mHistoryController = new HistoryController(context);
            mHistoryController.getUpdates();
            setAdapter(mHistoryController);
        }
    }
}
