package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;

public class ProjectsFragment extends Fragment implements AbsListView.OnItemClickListener,
                                                          LoaderManager.LoaderCallbacks<Cursor> {

    public interface OnProjectSelectedListener { public void onProjectSelected(String id); }
    private Context context;
    private OnProjectSelectedListener mListener;
    private AbsListView mListView;
    private CursorAdapter mAdapter;

    public ProjectsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projects, container, false);
        this.context = container.getContext();
        mListView = (AbsListView) view.findViewById(android.R.id.list);

        mListView.setOnItemClickListener(this);
        mAdapter = new ProjectCursorAdapter(this.context, null, 0);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);

        getLoaderManager().initLoader(0, null, this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnProjectSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnProjectSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) { mListener.onProjectSelected(""); }
    }


    private static final int PROJECTS_LOADER = 0;
    private static final int TIME_RECORDS_LOADER = 1;
    private static final int NOTES_LOADER = 2;
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this.context, TimeKeeperContract.Projects.CONTENT_URI, TimeKeeperContract.Projects.PROJECTION_ALL, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        this.mAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        this.mAdapter.swapCursor(null);
    }

}