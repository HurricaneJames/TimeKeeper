package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;

public class ProjectDetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface OnTimeRecordSelectedListener { public void onTimeRecordSelected(String id); }
    private static final String ARG_PROJECT_ID = "projectId";
    private int mProjectId;
    public int getShownProjectId() { return mProjectId; }

    private Context context;
    private OnTimeRecordSelectedListener mListener;
    private ExpandableListView mListView;
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

        mAdapter = new TimeRecordCursorAdapter(null, context, false);
        ((ExpandableListView)mListView).setAdapter(mAdapter);

        // todo - replace the onitemclick with on group expand/collapse onchildclick listeners
//        mListView.setOnItemClickListener(this);

        getLoaderManager().initLoader(0, null, this);


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
        return new CursorLoader(this.context, TimeKeeperContract.TimeRecords.CONTENT_URI, TimeKeeperContract.TimeRecords.PROJECTION_ALL, TimeKeeperContract.TimeRecords.whereProjectId(mProjectId), null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        this.mAdapter.setGroupCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        this.mAdapter.setGroupCursor(null);
    }

}
