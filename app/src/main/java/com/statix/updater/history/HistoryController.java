package com.statix.updater.history;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.statix.updater.R;
import com.statix.updater.misc.Constants;
import com.statix.updater.model.HistoryCard;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class HistoryController extends BaseAdapter {

    private ArrayList<HistoryCard> mCards;
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    private static final String TAG = "HistoryController";

    public HistoryController(Context ctx) {
        mContext = ctx;
        mLayoutInflater = LayoutInflater.from(ctx);
        mCards = new ArrayList<>();
    }

    public void getUpdates() {
        File historyFile = new File(Constants.HISTORY_PATH);
        try {
            mCards = HistoryUtils.readFromJson(historyFile);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Unable to find previous updates");
        }
    }

    @Override
    public int getCount() {
        return mCards.size();
    }

    @Override
    public Object getItem(int position) {
        return mCards.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.update_cardview, parent);
        }
        HistoryCard card = mCards.get(position);
        TextView title = convertView.findViewById(R.id.title);
        title.setText(card.getUpdateName());
        ImageView check = convertView.findViewById(R.id.success_view);
        int resid = card.updateSucceeded() ? R.drawable.checkmark : R.drawable.failed;
        Drawable d = ResourcesCompat.getDrawable(mContext.getResources(), resid, null);
        check.setImageDrawable(d);
        return convertView;
    }
}
