package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;

import org.joda.time.DateTime;

public class ProjectsFragment extends Fragment implements AbsListView.OnItemClickListener,
                                                          OnNoteRequestedListener,
                                                          LoaderManager.LoaderCallbacks<Cursor>{

    public interface OnProjectSelectedListener { public void onProjectSelected(String projectId); }

    public static final String ARG_HIGHLIGHT_SELECTED_ITEM = "highlighting";

    private static final String SELECTED_PROJECT = "selectedProject";
    private static final int INVALID_SELECTED_INDEX = -1;

    private Context context;
    private OnProjectSelectedListener mProjectSelectionListener;
    private OnNoteRequestedListener mNoteRequestedListener;
    private AbsListView mListView;
    private CursorAdapter mAdapter;
    private Handler timerButtonUpdateHandler;
    private boolean mHighlightItems = true;

    private int mCurrentPosition = INVALID_SELECTED_INDEX;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param highlighting enable/disable highlighting of selected item.
     * @return A new instance of fragment TextNoteFragment.
     */
    public static ProjectsFragment newInstance(boolean highlighting) {
        ProjectsFragment fragment = new ProjectsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HIGHLIGHT_SELECTED_ITEM, highlighting);
        fragment.setArguments(args);

        return fragment;
    }
    public ProjectsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mCurrentPosition = savedInstanceState.getInt(SELECTED_PROJECT, INVALID_SELECTED_INDEX);
        }
        if (getArguments() != null) {
            mHighlightItems = getArguments().getBoolean(ARG_HIGHLIGHT_SELECTED_ITEM, true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projects_list, container, false);
        this.context = getActivity().getApplicationContext();
        mListView = (AbsListView) view.findViewById(android.R.id.list);

        mListView.setOnItemClickListener(this);
        mAdapter = new ProjectCursorAdapter(this, this.context, null, 0);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
        mListView.setChoiceMode(mHighlightItems ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);

        view.findViewById(R.id.projects_list_new_project).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddNewProjectWithRunningTimerTask(context, System.currentTimeMillis()).execute();
            }
        });

        getLoaderManager().initLoader(0, null, this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mProjectSelectionListener = (OnProjectSelectedListener) activity;
            mNoteRequestedListener = (OnNoteRequestedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnProjectSelectedListener and OnNoteSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        timerButtonUpdateHandler = new Handler();
        timerButtonUpdateHandler.postDelayed(new Runnable() {
            public void run() {
                int viewCount = mListView.getChildCount();
                for(int i=0; i<viewCount; i++) {
                    TimerButton timerButton = (TimerButton)mListView.getChildAt(i).findViewById(R.id.project_item_timer);
                    timerButton.updateTime();
                }
                timerButtonUpdateHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerButtonUpdateHandler.removeCallbacksAndMessages(null);
        timerButtonUpdateHandler = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mProjectSelectionListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_PROJECT, mCurrentPosition);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("ProjectsFragment", "Item Click");
        mCurrentPosition = position;
        mListView.setItemChecked(mCurrentPosition, true);

        Cursor positionCursor = (Cursor)mAdapter.getItem(position);
        String projectId = positionCursor.getString(positionCursor.getColumnIndex(TimeKeeperContract.Projects._ID));
        if (null != mProjectSelectionListener) { mProjectSelectionListener.onProjectSelected(projectId); }
    }

    @Override
    public void onNewNoteRequested(String projectId, String noteType) {
        mNoteRequestedListener.onNewNoteRequested(projectId, noteType);
    }

    // project fragment will never receive or need an update event
    @Override
    public void onNoteUpdateRequested(String noteId) { }

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
        if(mCurrentPosition != INVALID_SELECTED_INDEX) {
            mListView.setItemChecked(mCurrentPosition, true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        this.mAdapter.swapCursor(null);
    }

    public class AddNewProjectWithRunningTimerTask extends AsyncTask<Void, Void, Void> {
        private Context mApplicationContext;
        private long mStartTime;
        private DateTime mStartAt;

        public AddNewProjectWithRunningTimerTask(Context context, long startTime) {
            mApplicationContext = context.getApplicationContext();
            mStartTime = startTime;
            mStartAt = new DateTime(startTime);
        }
        @Override
        protected Void doInBackground(Void... params) {
            ContentResolver contentResolver = mApplicationContext.getContentResolver();

            ContentValues projectValues = new ContentValues();
            projectValues.put(TimeKeeperContract.Projects.NAME, getString(R.string.started) + " "  + DateFormatter.getHumanFriendlyDate(mStartAt));
            Uri newProjectUri = contentResolver.insert(TimeKeeperContract.Projects.CONTENT_URI, projectValues);
            int projectId = Integer.parseInt(newProjectUri.getLastPathSegment());

            ContentValues timeRecordValues = new ContentValues();
            timeRecordValues.put(TimeKeeperContract.TimeRecords.PROJECT_ID, projectId);
            timeRecordValues.put(TimeKeeperContract.TimeRecords.START_AT, mStartTime);
            contentResolver.insert(TimeKeeperContract.TimeRecords.CONTENT_URI, timeRecordValues);

            return null;
        }
    }
}
