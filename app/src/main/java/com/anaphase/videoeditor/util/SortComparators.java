package com.anaphase.videoeditor.util;

import android.media.MediaMetadataRetriever;

import com.anaphase.videoeditor.mediafile.MediaFile;

import java.io.File;
import java.util.Comparator;

public class SortComparators {

    public SortComparators(){

    }

    public static Comparator<String> getStringListComparator(SortTypeEnum sortTypeEnum){
        Comparator<String> comparator = null;
        switch(sortTypeEnum){
            case BY_NAME:
                comparator = (path1, path2)->
                    ((new File(path1)).getName().toUpperCase().compareTo((new File(path2)).getName().toUpperCase()))
                ;
                break;
            case BY_DATE:
                comparator = (path1, path2)->{
                    int compare = 0;
                    try{
                        compare = (Long.compare((new File(path1)).lastModified(), (new File(path2)).lastModified()));
                    }catch (SecurityException securityException){
                        securityException.printStackTrace();
                    }
                    return compare;
                };
                break;
            case BY_SIZE:
                comparator = (path1, path2)->{
                    int compare = 0;
                    try{
                        compare = Long.compare((new File(path1)).length(), (new File(path2)).length());
                    }catch(SecurityException securityException){
                        securityException.printStackTrace();
                    }
                    return compare;
                };
                break;
            case BY_DURATION:
                comparator = (path1, path2)->{

                    MediaMetadataRetriever mediaMetadataRetriever1 = new MediaMetadataRetriever();
                    MediaMetadataRetriever mediaMetadataRetriever2 = new MediaMetadataRetriever();
                    int duration1, duration2;
                    try{
                        mediaMetadataRetriever1.setDataSource(path1);
                        mediaMetadataRetriever2.setDataSource(path2);
                    }catch(IllegalArgumentException illegalArgumentException){
                        illegalArgumentException.printStackTrace();
                    }
                    duration1 = Integer.parseInt(mediaMetadataRetriever1.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    duration2 = Integer.parseInt(mediaMetadataRetriever2.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    return Integer.compare(duration1, duration2);
                };
        }
        return comparator;
    }

    public static Comparator<MediaFile> getMediaFileListComparator(SortTypeEnum sortTypeEnum){
        Comparator<MediaFile> comparator = null;
        switch(sortTypeEnum){
            case BY_NAME:
                comparator = (mediaFile1, mediaFile2)->
                    (mediaFile1.getFileName().toUpperCase().compareTo(mediaFile2.getFileName().toUpperCase()));
                break;
            case BY_DATE:
                comparator = (mediaFile1, mediaFile2)->{
                    int compare;
                        compare = Long.compare(mediaFile1.getLastModified(), mediaFile2.getLastModified());
                    return compare;
                };
                break;
            case BY_SIZE:
                comparator = (mediaFile1, mediaFile2)->{
                    int compare;
                        compare = Long.compare(mediaFile1.getFileSize(), mediaFile2.getFileSize());
                    return compare;
                };
                break;
            case BY_DURATION:
                comparator = (mediaFile1, mediaFile2)->
                    (Integer.compare(mediaFile1.getFileDuration(),
                            mediaFile2.getFileDuration()));
        }
        return comparator;
    }
}
