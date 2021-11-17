package com.anaphase.videoeditor.mediafile;

import android.content.Context;
import android.os.Handler;

import com.anaphase.videoeditor.ui.browser.MediaStoreFileBrowser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MediaStoreTable {
    public static Map<String, ArrayList<MediaFile>> mediaStoreTable;
    private static MediaStoreFileBrowser mediaStoreFileBrowser;

    public static void startMediaStoreTableBuildTask(Context context, Handler handler){
        if(context instanceof MediaStoreFileBrowser){
            mediaStoreFileBrowser = (MediaStoreFileBrowser)context;
        }
        mediaStoreTable = new HashMap<>(30);
        MediaStoreWorker mediaStoreWorker = new MediaStoreWorker(mediaStoreTable, context, handler);
        Thread thread = new Thread(mediaStoreWorker);
        thread.start();
    }

    public static void notifyNewDirectoryCreated(String path){
        if(mediaStoreFileBrowser != null) {
            mediaStoreFileBrowser.addMediaFileObserver(path);
        }
    }
}
