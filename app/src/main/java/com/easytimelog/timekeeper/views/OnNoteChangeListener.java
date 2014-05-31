package com.easytimelog.timekeeper.views;

import android.content.ContentValues;

public interface OnNoteChangeListener {
    public static final String RESULT = "result";
    public static final int RESULT_ACCEPT = 1;
    public static final int RESULT_CANCEL = 0;

    // notify fragment listener that we are all done
    // if multi-pane, the activity should swap out this fragment for (nothing/the previous records list/the record list for this note?)
    // if single-pane, the activity should go back to the projects list
    public void onNoteChanged(int result, ContentValues values);
}
