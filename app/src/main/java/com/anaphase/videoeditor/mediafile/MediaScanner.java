package com.anaphase.videoeditor.mediafile;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.util.LinkedList;
import java.util.Queue;

public class MediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

    private final Queue<MediaFile> mediaFileQueue;
    private final MediaScannerConnection mediaScannerConnection;

    public MediaScanner(Context context){
        mediaScannerConnection = new MediaScannerConnection(context, this);
        mediaScannerConnection.connect();
        mediaFileQueue = new LinkedList<>();
    }

    public void enqueueMediaFile(MediaFile mediaFile){
        mediaFileQueue.offer(mediaFile);
        if(mediaFileQueue.size() == 1) {
            mediaScannerConnection.scanFile(mediaFileQueue.peek().getPath(), null);
        }
    }

    @Override
    public void onMediaScannerConnected(){
    }

    @Override
    public void onScanCompleted(String path, Uri uri){
        if(!mediaFileQueue.isEmpty()){
            mediaFileQueue.peek().setUri(uri);
            if(uri != null) {
                mediaFileQueue.poll();
            }
            if(!mediaFileQueue.isEmpty()){
                mediaScannerConnection.scanFile(mediaFileQueue.peek().getPath(), null);
            }
        }
    }
}
