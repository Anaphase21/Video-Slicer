package com.anaphase.videoeditor.mediafile;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;

import com.anaphase.videoeditor.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class MediaStoreWorker implements Runnable{

    private final Map<String, ArrayList<MediaFile>> mediaStoreTable;
    private final Handler handler;
    private final Context context;

    public MediaStoreWorker(Map<String, ArrayList<MediaFile>> mediaStoreTable, Context context, Handler handler){
        this.mediaStoreTable = mediaStoreTable;
        this.handler = handler;
        this.context = context;
    }

    @Override
    public void run(){
        populateMediaStoreTable();
    }

    private void populateMediaStoreTable(){
        Uri videoCollection;
        Uri audioCollection;
        Uri noMediaCollection;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            audioCollection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }else{
            videoCollection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            audioCollection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        noMediaCollection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
        String[] mediaStoreColumns = new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DURATION, MediaStore.MediaColumns.DATA};
        String mediaTypeColumn = MediaStore.Files.FileColumns.MEDIA_TYPE;
        String noMediaType = MediaStore.Files.FileColumns.MEDIA_TYPE_NONE + "";
        String path = "";
        int fileCount = 0;
        Cursor[] cursors = new Cursor[3];
        cursors[0] = context.getContentResolver().query(videoCollection, mediaStoreColumns, null, null, null);
        cursors[1] = context.getContentResolver().query(audioCollection, mediaStoreColumns, null, null, null);
        cursors[2] = context.getContentResolver().query(noMediaCollection, mediaStoreColumns, mediaTypeColumn + " = ?", new String[]{noMediaType}, null);
        int len = cursors.length;
        int rows = cursors[0].getCount() + cursors[1].getCount() + cursors[2].getCount();
        for(int i = 0; i < len; ++i){
            int idColumn = cursors[i].getColumnIndex(mediaStoreColumns[0]);
            int nameColumn = cursors[i].getColumnIndex(mediaStoreColumns[1]);
            int durationColumn = cursors[i].getColumnIndex(mediaStoreColumns[2]);
            int pathColumn = cursors[i].getColumnIndex(mediaStoreColumns[3]);
            while(cursors[i].moveToNext()){
                ++fileCount;
                Util.sendMessage((fileCount * 360) / rows, "sweep_angle_percent", handler);
                if(pathColumn != -1) {
                    path = cursors[i].getString(pathColumn);
                }
                //If this path does not exist, then ignore it.
                if(!(new File(path).exists())){
                    continue;
                }
                if(!(Util.isVideoExtension(path) || Util.isAudioExtension(path))){
                    continue;
                }
                long id = cursors[i].getLong(idColumn);
                String name = cursors[i].getString(nameColumn);
                int duration = cursors[i].getInt(durationColumn);
                Uri contentUri;
                if(i == 0){
                    contentUri = ContentUris.withAppendedId(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) : MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                }else{
                    contentUri = ContentUris.withAppendedId(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                }
                MediaFile mediaFile = new MediaFile();
                if(name == null){
                    mediaFile.setFileName((new File(path)).getName());
                }else {
                    mediaFile.setFileName(name);
                }
                mediaFile.setFileDuration(duration);
                mediaFile.setPath(path);
                mediaFile.setUri(contentUri);
                File file = new File(path);
                mediaFile.setLastModified(file.lastModified());
                String directory = file.getParent();
                ArrayList<MediaFile> mediaFiles = mediaStoreTable.get(directory);
                if(mediaFiles == null){
                    mediaFiles = new ArrayList<>();
                }
                mediaFiles.add(mediaFile);
                mediaStoreTable.put(directory, mediaFiles);
            }
            cursors[i].close();
        }
        sleep(100L);
        Message message = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("build_complete", "COMPLETE");
        message.setData(bundle);
        handler.sendMessage(message);
    }

    private void sleep(long duration){
        try{
            Thread.sleep(duration);
        }catch (InterruptedException interruptedException){}
    }
}
