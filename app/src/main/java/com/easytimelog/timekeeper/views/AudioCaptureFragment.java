package com.easytimelog.timekeeper.views;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.easytimelog.timekeeper.R;
import com.easytimelog.timekeeper.data.TimeKeeperContract;
import com.easytimelog.timekeeper.util.DatabaseHelper;

import java.io.IOException;

public class AudioCaptureFragment extends Fragment implements View.OnClickListener {
    private OnNoteChangeListener mListener;

    private static final String ARG_OUTPUT_URI = "outputUri";
    private static final String ARG_TIME_RECORD_ID = "time_record_id";
    private static final String ARG_PROJECT_ID = "project_id";
    private String mOutputUri;
    private String mTimeRecordId;
    private String mProjectId;

    private MediaRecorder mMediaRecorder;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param outputUri uri pointing to file on disk to contain audio.
     * @return A new instance of fragment AudioCaptureFragment.
     */
    public static AudioCaptureFragment newInstance(String projectId, String timeRecordId, Uri outputUri) { return newInstance(projectId, timeRecordId, outputUri.toString()); }
    public static AudioCaptureFragment newInstance(String projectId, String timeRecordId, String outputUri) {
        AudioCaptureFragment fragment = new AudioCaptureFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OUTPUT_URI, outputUri);
        args.putString(ARG_TIME_RECORD_ID, timeRecordId);
        args.putString(ARG_PROJECT_ID, projectId);
        fragment.setArguments(args);
        return fragment;
    }
    public AudioCaptureFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mOutputUri = args.getString(ARG_OUTPUT_URI);
            mTimeRecordId = args.getString(ARG_TIME_RECORD_ID);
            mProjectId = args.getString(ARG_PROJECT_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_capture, container, false);

        ImageButton pauseButton = (ImageButton)view.findViewById(R.id.button_audio_note_capture_record);
        pauseButton.setOnClickListener(this);

        startRecording();

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
    public void onClick(View v) {
        stopRecording();
        notifyCaptureListener(true);
    }

    private void notifyCaptureListener(boolean saved) {
        ContentValues noteValues = new ContentValues();
        noteValues.put(TimeKeeperContract.Notes.TIME_RECORD_ID, mTimeRecordId);
        noteValues.put(TimeKeeperContract.TimeRecords.PROJECT_ID, mProjectId);
        if(saved) {
            ContentValues savedValues = saveNote(mTimeRecordId, TimeKeeperContract.Notes.AUDIO_NOTE, mOutputUri, null);
            noteValues.putAll(savedValues);
        }
        mListener.onNoteChanged(saved ? OnNoteChangeListener.RESULT_ACCEPT : OnNoteChangeListener.RESULT_CANCEL, noteValues);
    }

    private ContentValues saveNote(String timeRecordId, String noteType, String link, String scribble) {
        ContentValues values = new ContentValues();
        values.put(TimeKeeperContract.Notes.TIME_RECORD_ID, timeRecordId);
        values.put(TimeKeeperContract.Notes.NOTE_TYPE, noteType);
        if(link     != null) { values.put(TimeKeeperContract.Notes.LINK, link); }
        if(scribble != null) { values.put(TimeKeeperContract.Notes.SCRIBBLE, scribble); }
        DatabaseHelper.addNote(getActivity().getApplicationContext(), values);
        return values;
    }

    private void handleNoMicrophoneFound() {
        notifyCaptureListener(false);
    }

    private void startRecording() {
        // todo - test what happens if you hit the home button (or change to a different app) while recording
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            handleNoMicrophoneFound();
            return;
        }
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        if(mOutputUri.startsWith("file://")) { mOutputUri = mOutputUri.substring(7); }
        Log.d("AudioCaptureFragment - startRecording", "Set Output File: " + mOutputUri);
        mMediaRecorder.setOutputFile(Uri.parse(mOutputUri).toString());
        try {
            mMediaRecorder.prepare();
        }catch (IOException ioe) {
            Log.e("AudioCaptureFragment", "prepare() failed");
        }
        mMediaRecorder.start();
    }

    private void stopRecording() {
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
    }
}
