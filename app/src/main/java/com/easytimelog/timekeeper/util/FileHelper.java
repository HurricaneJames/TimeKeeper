package com.easytimelog.timekeeper.util;

import android.content.Context;
import android.net.Uri;
import android.text.format.Time;

import java.io.File;

public class FileHelper {
    public static final int CAMERA_TYPE = 0;
    public static final int AUDIO_TYPE = 1;

    public static String getFileExtension(int mediaType) {
        switch(mediaType) {
            case CAMERA_TYPE:
                return ".jpg";
            case AUDIO_TYPE:
                return ".m4a";
            default:
                return "";
        }
    }

    public static Uri getOutputFileUri(Context context, String baseName, int mediaType) { return getOutputFileUri(context, baseName, mediaType, null); }
    public static Uri getOutputFileUri(Context context, String baseName, int mediaType, String optionalEnvironmentDirectory) {
        Time time = new Time(Time.TIMEZONE_UTC);
        time.setToNow();
        String timeStampedFilename = baseName + '_' + time.format("%Y-%m-%dT%H%M%SUTC") + getFileExtension(mediaType);
        File path = context.getExternalFilesDir(optionalEnvironmentDirectory);
        File file = new File(path, timeStampedFilename);
        return Uri.fromFile(file);
    }
}
