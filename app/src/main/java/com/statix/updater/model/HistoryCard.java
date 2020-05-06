package com.statix.updater.model;

public class HistoryCard {

    private String mName;
    private boolean mSuccessful;

    public HistoryCard(String updateName, boolean success) {
        mName = updateName;
        mSuccessful = success;
    }

    public String getUpdateName() {
        return mName;
    }

    public boolean updateSucceeded() {
        return mSuccessful;
    }
}
