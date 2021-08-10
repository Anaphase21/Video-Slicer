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

    private Map<String, ArrayList<MediaFile>> mediaStoreTable;
    private Handler handler;
    private Context context;

    public MediaStoreWorker(Map<String, ArrayList<MediaFile>> mediaStoreTable, Context context, Handler hanldler){
        this.mediaStoreTable = mediaStoreTable;
        this.handler = hanldler;
        this.context = context;
    }

    @Override
    public void run(){
        populateMediaStoreTable();
    }

    private void populateMediaStoreTable(){
        Uri videoCollection;
        Uri audioCollection;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            audioCollection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }else{
            videoCollection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            audioCollection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        String[] mediaStoreColumns = new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DURATION, MediaStore.MediaColumns.DATA};
        Uri[] collections = new Uri[]{videoCollection, audioCollection};
        int len = collections.length;
        for(int i = 0; i < len; ++i){
            try(Cursor cursor = context.getApplicationContext().getContentResolver().query(collections[i], mediaStoreColumns, null, null, null)){
                int idColumn = cursor.getColumnIndexOrThrow(mediaStoreColumns[0]);
                int nameColumn = cursor.getColumnIndexOrThrow(mediaStoreColumns[1]);
                int durationColumn = cursor.getColumnIndexOrThrow(mediaStoreColumns[2]);
                int pathColumn = cursor.getColumnIndex(mediaStoreColumns[3]);
                while(cursor.moveToNext()){
                    String path = "";
                    if(pathColumn != -1) {
                        path = cursor.getString(pathColumn);
                    }
                    //If this path does not exist, then ignore it.
                    if(!(new File(path).exists())){
                        continue;
                    }
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    int duration = cursor.getInt(durationColumn);
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
            }catch(IllegalArgumentException illegalArgumentException){
                illegalArgumentException.printStackTrace();
            }catch (SecurityException securityException){
                securityException.printStackTrace();
            }
        }
        Message message = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("build_complete", "COMPLETE");
        message.setData(bundle);
        handler.sendMessage(message);
    }
}
