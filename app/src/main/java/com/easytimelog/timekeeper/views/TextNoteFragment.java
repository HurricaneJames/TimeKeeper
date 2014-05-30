package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;


public class TextNoteFragment extends Fragment implements View.OnClickListener {
    private OnNoteChangeListener mListener;

    private static final String ARG_NOTE_ID = "note_id";
    private static final String ARG_TIME_RECORD_ID = "time_record_id";
    private static final String ARG_PROJECT_ID = "project_id";
    private static final long INVALID_NOTE_ID = -1;

    private long   mNoteId;
    private String mTimeRecordId;
    private String mProjectId;
    private EditText mScribbleField;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param projectId Id of the project currently holding the time record [required].
     * @param timeRecordId Id of time record that holds note [required].
     * @param noteId Id of note to represent [optional - null means new note].
     * @return A new instance of fragment TextNoteFragment.
     */
    public static TextNoteFragment newInstance(String projectId, String timeRecordId, String noteId) {
        TextNoteFragment fragment = new TextNoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectId);
        args.putString(ARG_TIME_RECORD_ID, timeRecordId);
        if(noteId != null) { args.putString(ARG_NOTE_ID, noteId); }
        fragment.setArguments(args);
        return fragment;
    }
    public TextNoteFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProjectId = getArguments().getString(ARG_PROJECT_ID);
            mNoteId = getArguments().getLong(ARG_NOTE_ID, INVALID_NOTE_ID);
            mTimeRecordId = getArguments().getString(ARG_TIME_RECORD_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_text_note, container, false);

        view.findViewById(R.id.saveButton).setOnClickListener(this);
        view.findViewById(R.id.cancelButton).setOnClickListener(this);

        mScribbleField = (EditText)view.findViewById(R.id.text_note_scribble);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnNoteChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnNoteChangeListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        notifyChangeListener(view.getId() == R.id.saveButton);
    }

    private void notifyChangeListener(boolean saveNote) {
        ContentValues values = new ContentValues();
            values.put(TimeKeeperContract.TimeRecords.PROJECT_ID, mProjectId);
            values.put(TimeKeeperContract.Notes.TIME_RECORD_ID, mTimeRecordId);
            if(saveNote) { values.putAll(saveNote()); }
            if(mNoteId != INVALID_NOTE_ID) { values.put(TimeKeeperContract.TimeRecords._ID, mNoteId); }

        mListener.onNoteChanged(saveNote ? OnNoteChangeListener.RESULT_ACCEPT : OnNoteChangeListener.RESULT_CANCEL, values);
    }

    private ContentValues saveNote() { return saveNote(new ContentValues()); }
    private ContentValues saveNote(ContentValues values) {
        AsyncQueryHandler queryHandler = new AsyncQueryHandler(getActivity().getApplicationContext().getContentResolver()) {};
        values.put(TimeKeeperContract.Notes.TIME_RECORD_ID, mTimeRecordId);
        values.put(TimeKeeperContract.Notes.NOTE_TYPE, TimeKeeperContract.Notes.TEXT_NOTE);
        values.put(TimeKeeperContract.Notes.SCRIBBLE, mScribbleField.getText().toString());
        if(mNoteId != INVALID_NOTE_ID) {
            queryHandler.startUpdate(0, null, ContentUris.withAppendedId(TimeKeeperContract.Notes.CONTENT_URI, mNoteId), values, null, null);
        }else {
            queryHandler.startInsert(0, null, TimeKeeperContract.Notes.CONTENT_URI, values);
        }
        return values;
    }
}
