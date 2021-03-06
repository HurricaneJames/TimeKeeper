package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;
import com.easytimelog.timekeeper.util.DatabaseHelper;


public class TextNoteFragment extends Fragment implements View.OnClickListener {
    private OnNoteChangeListener mListener;

    private static final String ARG_NOTE_ID = "note_id";
    private static final String ARG_TIME_RECORD_ID = "time_record_id";
    private static final String ARG_PROJECT_ID = "project_id";

    private String mNoteId;
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
            mNoteId = getArguments().getString(ARG_NOTE_ID);
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
    public void onPause() {
        super.onPause();
        InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    @Override
    public void onResume() {
        super.onResume();
        EditText summaryText = (EditText)getActivity().findViewById(R.id.text_note_scribble);
        summaryText.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(summaryText, InputMethodManager.SHOW_IMPLICIT);
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
            if(mNoteId != null) { values.put(TimeKeeperContract.TimeRecords._ID, mNoteId); }

        mListener.onNoteChanged(saveNote ? OnNoteChangeListener.RESULT_ACCEPT : OnNoteChangeListener.RESULT_CANCEL, values);
    }

    private ContentValues saveNote() {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.Notes.TIME_RECORD_ID, mTimeRecordId);
        values.put(TimeKeeperContract.Notes.NOTE_TYPE, TimeKeeperContract.Notes.TEXT_NOTE);
        values.put(TimeKeeperContract.Notes.SCRIBBLE, mScribbleField.getText().toString());
        if(mNoteId != null) {
            DatabaseHelper.updateNote(getActivity().getApplicationContext(), mNoteId, values);
        }else {
            DatabaseHelper.addNote(getActivity().getApplicationContext(), values);
        }
        return values;
    }
}
