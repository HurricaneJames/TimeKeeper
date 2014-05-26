package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.CursorTreeAdapter;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;
import com.easytimelog.timekeeper.util.TaskQueue;

public class ProjectDetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface OnTimeRecordSelectedListener { public void onTimeRecordSelected(String id); }
    private static final String ARG_PROJECT_ID = "projectId";
    private int mProjectId;
    public int getShownProjectId() { return mProjectId; }

    private static final int TIME_RECORDS_LOADER = 0;
    private static final int PROJECT_LOADER = 1;
    private boolean mIgnoreProjectUpdates = false;

    private Context context;
    private OnTimeRecordSelectedListener mListener;
    private ExpandableListView mListView;
    private EditText mProjectNameField;
    private CursorTreeAdapter mAdapter;


    public static ProjectDetailsFragment newInstance(int projectId) {
        ProjectDetailsFragment fragment = new ProjectDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PROJECT_ID, projectId);
        fragment.setArguments(args);
        return fragment;
    }
    public ProjectDetailsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProjectId = getArguments().getInt(ARG_PROJECT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_details, container, false);
        this.context = getActivity().getApplicationContext();
        mListView = (ExpandableListView) view.findViewById(android.R.id.list);
        mListView.setGroupIndicator(null);

        mProjectNameField = (EditText)view.findViewById(R.id.projectName);
        ProjectNameTextChangeListener textChangeListener = new ProjectNameTextChangeListener();
        mProjectNameField.addTextChangedListener(textChangeListener);
        mProjectNameField.setOnFocusChangeListener(textChangeListener);

        mAdapter = new TimeRecordCursorAdapter(null, context, false);
        ((ExpandableListView)mListView).setAdapter(mAdapter);

        getLoaderManager().initLoader(TIME_RECORDS_LOADER, null, this);
        getLoaderManager().initLoader(PROJECT_LOADER, null, this);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTimeRecordSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnTimeRecordSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch(id) {
            case TIME_RECORDS_LOADER:
                return new CursorLoader(this.context, TimeKeeperContract.TimeRecords.CONTENT_URI, TimeKeeperContract.TimeRecords.PROJECTION_ALL, TimeKeeperContract.TimeRecords.whereProjectId(mProjectId), null, null);
            case PROJECT_LOADER:
                return new CursorLoader(getActivity().getApplicationContext(),
                        ContentUris.withAppendedId(TimeKeeperContract.Projects.CONTENT_URI, mProjectId),
                        new String[] { TimeKeeperContract.Projects.NAME },
                        null, null, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch(loader.getId()) {
            case TIME_RECORDS_LOADER:
                this.mAdapter.setGroupCursor(data);
                break;
            case PROJECT_LOADER:
                if(!mIgnoreProjectUpdates) {
                    data.moveToFirst();
                    mIgnoreProjectUpdates = true;
                    mProjectNameField.setText(data.getString(data.getColumnIndex(TimeKeeperContract.Projects.NAME)));
                }
                break;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
       switch(loader.getId()) {
           case TIME_RECORDS_LOADER:
               this.mAdapter.setGroupCursor(null);
               break;
           case PROJECT_LOADER:
               mProjectNameField.setText("");
               break;
       }
    }

    private class ProjectNameTextChangeListener implements TextWatcher, View.OnFocusChangeListener {
        private TaskQueue taskQueue = new TaskQueue();

        @Override
        public void afterTextChanged(Editable s) {
            if(taskQueue.isRunning()) {
                final String newName = s.toString();
                Runnable dbUpdateTask = new Runnable() {
                    @Override
                    public void run() {
                        ContentValues values = new ContentValues();
                        values.put(TimeKeeperContract.Projects.NAME, newName);
                        context.getApplicationContext().getContentResolver().update(
                                ContentUris.withAppendedId(TimeKeeperContract.Projects.CONTENT_URI, mProjectId),
                                values, null, null);
                    }
                };
                taskQueue.addTask(dbUpdateTask);
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if(hasFocus) {
                mIgnoreProjectUpdates = true;
                taskQueue.addAfterProcessQueueTask(new Runnable() { public void run() { mIgnoreProjectUpdates = false; } });
                taskQueue.start();
            }else {
                taskQueue.stop();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
    }
}
