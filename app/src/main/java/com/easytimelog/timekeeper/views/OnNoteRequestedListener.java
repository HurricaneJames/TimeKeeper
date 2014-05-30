package com.easytimelog.timekeeper.views;

public interface OnNoteRequestedListener {
    public void onNewNoteRequested(String projectId, String noteType);
    public void onNoteUpdateRequested(String noteId);
}
