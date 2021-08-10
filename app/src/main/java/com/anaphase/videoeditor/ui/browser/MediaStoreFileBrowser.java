package com.anaphase.videoeditor.ui.browser;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.anaphase.videoeditor.ui.editor.EditFileActivity;
import com.anaphase.videoeditor.R;
import com.anaphase.videoeditor.mediafile.MediaFile;
import com.anaphase.videoeditor.mediafile.MediaFileInformationFetcher;
import com.anaphase.videoeditor.mediafile.MediaFileObserver;
import com.anaphase.videoeditor.mediafile.MediaScanner;
import com.anaphase.videoeditor.mediafile.MediaStoreWorker;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

public class MediaStoreFileBrowser extends BaseFileBrowserActivity {

    protected volatile Map<String, ArrayList<MediaFile>> mediaStoreTable;
    private MaterialToolbar materialToolbar;
    private AppBarLayout layout;
    private Paint paint;
    private float scale;
    private int scrollPosition;
    private ArrayList<MediaFile> mediaDirectories;
    private List<MediaFileObserver> mediaFileObservers;
    private MediaScanner mediaScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            String path = savedInstanceState.getString("current_path");
            if(path != null){
                currentDirectory = path;
            }
        }else {
            currentDirectory = "";
        }
        initialiseHandler();
        layout = findViewById(R.id.top_toolbar_layout);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if(animator instanceof SimpleItemAnimator){
            ((SimpleItemAnimator)animator).setSupportsChangeAnimations(false);
        }
        materialToolbar = findViewById(R.id.top_toolbar);
        materialToolbar.setSubtitle(currentDirectory);
        recyclerViewAdapter = new MediaFilesRecyclerViewAdapter(mediaFiles);
        recyclerView.setAdapter(recyclerViewAdapter);
        scale = getResources().getDisplayMetrics().scaledDensity;
        paint = new Paint();
        paint.setTypeface(Typeface.DEFAULT);
        paint.setLetterSpacing(0.0f);
        paint.setTextSize(scale * 15.0f);
        mediaStoreTable = new HashMap<>(30);
        mediaDirectories = new ArrayList<>();
        startMediaStoreTableBuildTask();
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true){
            @Override
            public void handleOnBackPressed(){
                if(threadPoolExecutor != null){
                    threadPoolExecutor.shutdownNow();
                    resetNumFilesFullyLoaded();
                }
                if(!currentDirectory.isEmpty()) {
                    mediaFiles.clear();
                    directoriesFilesLoadedCount.remove(currentDirectory);
                    ArrayList<String> directories = new ArrayList<>(mediaStoreTable.keySet());
                    directories.sort(getFileListComparator());
                    populateMediaFiles(directories);
                    recyclerViewAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(scrollPosition);
                    currentDirectory = "";
                    materialToolbar.setTitle(currentDirectory);
                    materialToolbar.setSubtitle(currentDirectory);
                }else{
                    killMediaFileObservers();
                    finish();
                }
            }
        };
        mediaScanner = new MediaScanner(this);
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private void startMediaStoreTableBuildTask(){
        MediaStoreWorker worker = new MediaStoreWorker(mediaStoreTable, this, mediaStoreWorkerHandler);
        Thread th = new Thread(worker);
        th.start();
    }

    private void initialiseHandler(){
        mediaStoreWorkerHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message){
                Bundle bundle = message.getData();
                String mediaStoreTableBuildComplete = bundle.getString("build_complete", "");
                if(!mediaStoreTableBuildComplete.isEmpty()){
                    ArrayList<String> directories = new ArrayList<>(mediaStoreTable.keySet());
                    directories.sort(getFileListComparator());
                    populateMediaFiles(directories);
                    recyclerViewAdapter.notifyDataSetChanged();
                    initialiseMediaFileObservers();
                }
                String filePathAdded = bundle.getString("+fileChange", "");
                if(!filePathAdded.isEmpty()){
                    File file = new File(filePathAdded);
                    if(file.isDirectory()){
                        mediaStoreTable.put(file.getPath(), new ArrayList<>());
                        if(currentDirectory.isEmpty()){
                            ArrayList<String> directories = new ArrayList<>(mediaStoreTable.keySet());
                            directories.sort(getFileListComparator());
                            MediaFileObserver mediaFileObserver = new MediaFileObserver(file.getPath());
                            mediaFileObserver.setHandler(mediaStoreWorkerHandler);
                            mediaFileObserver.startWatching();
                            populateMediaFiles(directories);
                            recyclerViewAdapter.notifyDataSetChanged();
                            mediaFileObservers.add(mediaFileObserver);
                        }
                    }else{
                        MediaFile mediaFile = new MediaFile();
                        String path = file.getPath();
                        String parent = file.getParent();
                        mediaFile.setFileName(file.getName());
                        mediaFile.setPath(path);
                        mediaFile.setContext(MediaStoreFileBrowser.this);
                        ArrayList<MediaFile> mediaFiles = mediaStoreTable.get(parent);
                        if(mediaFiles == null){
                            mediaFiles = new ArrayList<>();
                        }
                        mediaFiles.add(mediaFile);
                        mediaStoreTable.put(parent, mediaFiles);
                        mediaScanner.enqueueMediaFile(mediaFile);
                        if(currentDirectory.equals(parent)){
                            MediaStoreFileBrowser.this.mediaFiles.add(mediaFile);
                            int position = MediaStoreFileBrowser.this.mediaFiles.size() - 1;
                            recyclerViewAdapter.notifyItemInserted(position);
                            if(threadPoolExecutor != null){
                                threadPoolExecutor.execute(new MediaFileInformationFetcher(mediaFile, handler, position));
                            }
                        }else if(currentDirectory.isEmpty()){
                            recyclerViewAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        };
    }

    public void setCurrentDirectory(String directory){
        currentDirectory = directory;
        materialToolbar.setTitle(currentDirectory);
        materialToolbar.setSubtitle((new File(currentDirectory)).getName());
        disableMenuItems();
    }

    public String getCurrentDirectory(){
        return currentDirectory;
    }

    public void openFileInEditor(String path){
        Intent intent = new Intent(this, EditFileActivity.class);
        intent.putExtra("path", path);
        startActivity(intent);
    }

    @Override
    public void populateMediaFiles(ArrayList<String> paths){
        if(paths == null){
            mediaDirectories.addAll(mediaFiles);
            mediaFiles.clear();
            recyclerViewAdapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(0);
            mediaFiles.addAll(sortMediaFiles(mediaStoreTable.get(currentDirectory)));
            tasks = new LinkedBlockingQueue<>(mediaFiles.size() == 0 ? 1 : mediaFiles.size());
            threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TIME_UNIT, tasks);
            MediaFile mediaFile;
            int len = mediaFiles.size();
            for(int i = 0; i < len; ++i){
                mediaFile = mediaFiles.get(i);
                mediaFile.setContext(this);
                threadPoolExecutor.execute(new MediaFileInformationFetcher(mediaFile, handler, i));
            }
        }else{
            super.populateMediaFiles(paths);
        }
    }

    private Comparator<String> getFileListComparator(){
            Comparator<String> comparator = (str1, str2)->{
                str1 = (new File(str1)).getName();
                str2 = (new File(str2)).getName();
                return str1.toUpperCase().compareTo(str2.toUpperCase());
            };
            return comparator;
    }

    private Comparator<MediaFile> getMediaFileComparator(){
        Comparator<MediaFile> comparator = (mediaFile1,  mediaFile2)->{
            String str1 = mediaFile1.getFileName();
            String str2 = mediaFile2.getFileName();
            return str1.toUpperCase().compareTo(str2.toUpperCase());
        };
        return comparator;
    }

    public int getDirectoryFileCount(String path){
        return mediaStoreTable.get(path).size();
    }

    public void setScrollPosition(int position){
        scrollPosition = position;
    }

    private void initialiseMediaFileObservers(){
        Set<String> directories = mediaStoreTable.keySet();
        mediaFileObservers = new ArrayList<>(directories == null ? 0 : directories.size());
        File[] externalMediaDirs = getExternalCacheDirs();
        if(externalMediaDirs != null) {
            for (File externalMediaDir : externalMediaDirs) {
                while(!(externalMediaDir = externalMediaDir.getParentFile()).getName().equals("Android"));
                externalMediaDir = externalMediaDir.getParentFile();
                if(Environment.getExternalStorageState(externalMediaDir).equals(Environment.MEDIA_MOUNTED)){
                    MediaFileObserver mediaFileObserver = new MediaFileObserver(externalMediaDir.getPath());
                    mediaFileObserver.setHandler(mediaStoreWorkerHandler);
                    mediaFileObserver.startWatching();
                    mediaFileObservers.add(mediaFileObserver);
                }
            }
        }
        for(String directory : directories){
            MediaFileObserver mediaFileObserver = new MediaFileObserver(directory);//new File(directory));
            mediaFileObserver.setHandler(mediaStoreWorkerHandler);
            mediaFileObserver.startWatching();
            mediaFileObservers.add(mediaFileObserver);
        }
    }

    private void killMediaFileObservers(){
        if(mediaFileObservers != null){
            for(MediaFileObserver mediaFileObserver : mediaFileObservers){
                mediaFileObserver.stopWatching();
            }
            mediaFileObservers.clear();
            mediaFileObservers = null;
        }
    }
}