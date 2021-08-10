package com.anaphase.videoeditor.mediafile;

import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Size;

import com.anaphase.videoeditor.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MediaFileInformationFetcher implements Runnable{

    private MediaFile mediaFile;
    private Handler handler;
    private float DP_SCALE;
    private int mediaFileIndex;

    public MediaFileInformationFetcher(MediaFile mediaFile, Handler handler, int mediaFileIndex){
        this.mediaFile = mediaFile;
        this.handler = handler;
        this.mediaFileIndex = mediaFileIndex;
        DP_SCALE = mediaFile.getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    public void run(){
        int width;
        int height;
        Bitmap thumbnail = null;
        if(mediaFile.isAudio()){
            mediaFile.setFileIcon(Icon.createWithResource(mediaFile.getContext(), R.drawable.ic_baseline_audiotrack_24));
        }else if(mediaFile.isDirectory()){
            mediaFile.setFileIcon(Icon.createWithResource(mediaFile.getContext(), R.drawable.ic_baseline_folder_24));
        }else{
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if(mediaFile.isImage()){
                    try {
                        thumbnail = MediaStore.Images.Media.getBitmap(mediaFile.getContext().getContentResolver(), Uri.fromFile(new File(mediaFile.getPath())));
                        width = thumbnail.getWidth();
                        height = thumbnail.getHeight();
                        thumbnail = Bitmap.createScaledBitmap(thumbnail, (int)Math.ceil((float)width / height) * (int)(DP_SCALE * 40.0f), (int)(DP_SCALE * 40.0f), false);
                    }catch(FileNotFoundException fileNotFoundException){
                        fileNotFoundException.printStackTrace();}catch (IOException ioe){
                        ioe.printStackTrace();
                    }
                }else if(mediaFile.isVideo()){
                    thumbnail = ThumbnailUtils.createVideoThumbnail(mediaFile.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
                }
            } else {
                if (mediaFile.isImage()) {
                    try {
                        thumbnail = ThumbnailUtils.createImageThumbnail(new File(mediaFile.getPath()), new Size((int)(DP_SCALE * 40.0f), (int)(DP_SCALE * 40.0f)), null);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                } else if (mediaFile.isVideo()) {
                    try {
                        thumbnail = ThumbnailUtils.createVideoThumbnail(new File(mediaFile.getPath()), new Size((int)(DP_SCALE * 40.0f), (int)(DP_SCALE * 40.0f)), null);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
        mediaFile.setThumbnail(thumbnail);
        if(!mediaFile.isDirectory()) {
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            try {
                metadataRetriever.setDataSource(mediaFile.getPath());
            } catch (Exception exception) {
            }
            if (mediaFile.getFileDuration() == -1) {
                String duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (duration != null) {
                    mediaFile.setFileDuration(Integer.parseInt(duration));
                }else{
                    mediaFile.setFileDuration(0);
                }
            }
            Uri uri;
            if(mediaFile.getUri() == null) {
                if (mediaFile.isVideo()) {
                    uri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) :
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else {
                    uri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) :
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String[] projection = new String[]{MediaStore.MediaColumns._ID};
                String selection = MediaStore.MediaColumns.DATA;
                Cursor cursor = mediaFile.getContext().getApplicationContext().getContentResolver().query(uri, projection, selection + " = ?", new String[]{mediaFile.getPath()}, null);
                int idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                if (cursor.moveToFirst()) {
                    long id = cursor.getLong(idCol);
                    Uri contentUri = ContentUris.withAppendedId(uri, id);
                    mediaFile.setUri(contentUri);
                }
                cursor.close();
            }
        }
        if(handler != null) {
            sendMessage(mediaFileIndex, "loadingComplete");
        }

        long fileSize = 0;
        long dateLastModified = 0;
        File file = new File(mediaFile.getPath());
        try {
            fileSize = file.length();
            dateLastModified = file.lastModified();
        }catch(SecurityException securityException){
            securityException.printStackTrace();
        }
        mediaFile.setFileSize(fileSize);
        mediaFile.setLastModified(dateLastModified);
    }

    private void sendMessage(int data, String key){
        Bundle bundle = new Bundle();
        bundle.putInt(key, data);
        Message msg = handler.obtainMessage();
        msg.setData(bundle);
        handler.sendMessage(msg);
    }
}