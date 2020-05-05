package com.statix.updater.model;
import com.statix.updater.misc.Constants;

public class HistoryCard {

    private ABUpdate mUpdate;

    public HistoryCard (ABUpdate update) {
        mUpdate = update;
    }

    public String getUpdateName() {
        return mUpdate.update().getName();
    }

    public boolean updateFailed() {
        return mUpdate.state() == Constants.UPDATE_FAILED;
    }
}