package com.anaphase.videoeditor.ui.browser;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import androidx.activity.OnBackPressedCallback;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.anaphase.videoeditor.ui.editor.EditFileActivity;
import com.anaphase.videoeditor.R;
import com.anaphase.videoeditor.mediafile.MediaFile;
import com.anaphase.videoeditor.mediafile.MediaFileObserver;
import com.anaphase.videoeditor.mediafile.MediaFileObserverManager;
import com.anaphase.videoeditor.util.SortTypeEnum;
import com.anaphase.videoeditor.util.Util;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class FileBrowserActivity extends BaseFileBrowserActivity {

    private final String storageRoot = File.separator + "storage";
    private File file = new File(storageRoot);
    private Stack<Integer> previousPositions;
    private volatile Stack<FolderNode> previousFolders;
    private MaterialToolbar materialToolbar;
    private AppBarLayout layout;
    private int toolbarWidth;
    private Paint paint;
    private float scale;

    protected Handler fileHandler;
    private MediaFileObserverManager mediaFileObserverManager;
    private Thread mediaFileObserverManagerThread;
    private OnBackPressedCallback onBackPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState != null){
            String path = savedInstanceState.getString("current_path");
            if(path != null){
                file = new File(path);
                currentDirectory = path;
            }
        }
        super.onCreate(savedInstanceState);
        layout = findViewById(R.id.top_toolbar_layout);
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if(animator instanceof SimpleItemAnimator){
            ((SimpleItemAnimator)animator).setSupportsChangeAnimations(false);
        }
        materialToolbar = findViewById(R.id.top_toolbar);
        materialToolbar.setSubtitle(file.getPath());
        recyclerViewAdapter = new MediaFilesRecyclerViewAdapter(mediaFiles);
        //recyclerViewAdapter.setHasStableIds(true);
        recyclerView.setAdapter(recyclerViewAdapter);
        previousFolders = new Stack<>();
        previousPositions = new Stack<>();
        scale = getResources().getDisplayMetrics().scaledDensity;
        paint = new Paint();
        paint.setTypeface(Typeface.DEFAULT);
        paint.setLetterSpacing(0.0f);
        paint.setTextSize(scale * 15.0f);
        //Call back for handling onBackPressed events dispatched by OnBackPressedDispatcher
        onBackPressedCallback = new OnBackPressedCallback(true){
            @Override
            public void handleOnBackPressed(){
                if(threadPoolExecutor != null){
                    threadPoolExecutor.shutdownNow();
                }
                ArrayList<MediaFile> currMediaFiles;
                int currPosition = 0;
                if((!file.getPath().equals(File.separator + "storage"))) {
                    do {
                        file = file.getParentFile();
                        assert file != null;
                        currentDirectory = file.getPath();
                        if (file.getPath().equals(File.separator + "storage" + File.separator + "emulated")) {
                            file = file.getParentFile();
                            currentDirectory = file.getPath();
                            if(!previousFolders.isEmpty()) {
                                currMediaFiles = previousFolders.pop().getChildren();
                            }else{
                                setCurrentDirectory(file == null ? "" : file.getPath());
                                initialiseBrowser();
                                return;
                            }
                            break;
                        }
                        if(previousFolders.isEmpty()){
                            setCurrentDirectory(file.getPath());
                            initialiseBrowser();
                            return;
                        }
                        currMediaFiles = previousFolders.pop().getChildren();
                        currPosition = previousPositions.pop();
                    }while(!file.exists());
                    mediaFiles.clear();
                    mediaFiles.addAll(checkForFileChanges(currMediaFiles));
                    currentDirectory = file.getPath();
                    setMediaFilesOnlyCount();
                    recyclerView.scrollToPosition(currPosition);
                    recyclerViewAdapter.notifyDataSetChanged();
                    materialToolbar.setTitle(fitPathToToolbarWidth());
                    materialToolbar.setSubtitle(file.getName());
                    mediaFileObserverManager.setPathRemoved(true);
                    try{
                        mediaFileObserverManagerThread.interrupt();
                    }catch(SecurityException securityException){
                        securityException.printStackTrace();
                    }
                }else{
                    mediaFileObserverManager.clearObservers();
                    try{
                        mediaFileObserverManagerThread.interrupt();
                        mediaFileObserverManager = null;
                    }catch(SecurityException securityException){
                        mediaFileObserverManager = null;
                        securityException.printStackTrace();
                    }
                    finish();
                }
            }
        };
        initialiseBrowser();
    }

    private void initialiseBrowser(){
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
        initialiseHandler();
        initialiseMediaFileObserverManager();
        populateMediaFiles(listFiles());
    }

    private ArrayList<MediaFile> checkForFileChanges(ArrayList<MediaFile> mediaFiles){
        if(mediaFiles != null){
            int idx = 0;
            int size = mediaFiles.size();
            MediaFile mediaFile;
            while (idx < size){
                mediaFile = mediaFiles.get(idx);
                if(!(new File(mediaFile.getPath())).exists()){
                    mediaFiles.remove(idx);
                    --size;
                }else{
                    ++idx;
                }
            }
        }
        return mediaFiles;
    }

    private void initialiseMediaFileObserverManager(){
        MediaFileObserver mediaFileObserver = new MediaFileObserver(file.getPath());
        mediaFileObserver.setHandler(fileHandler);
        mediaFileObserverManager = new MediaFileObserverManager(mediaFileObserver);
        mediaFileObserverManagerThread = new Thread(mediaFileObserverManager);
        mediaFileObserverManagerThread.start();
    }

    private void initialiseHandler(){
        fileHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message){
            }
        };
    }

    public ArrayList<String> listFiles(){
        String path = file.getPath();
        ArrayList<String> paths = new ArrayList<>();
        if(path.equals(storageRoot)){
            String storageVolume = Environment.getExternalStorageDirectory().getPath();
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                paths.add(storageVolume);
            }
            String[] directories = file.list();
            assert directories != null;
            for(String storageDirectory : directories){
                storageDirectory = storageRoot + File.separator + storageDirectory;
                try{
                    if (Environment.isExternalStorageRemovable(new File(storageDirectory))) {
                        if (Environment.getExternalStorageState(new File(storageDirectory)).equals(Environment.MEDIA_MOUNTED)) {
                            paths.add(storageDirectory);
                        }
                    }
                }catch(IllegalArgumentException illegalArgumentException){}
            }
            return paths;
        }
        try {
            File[] files = file.listFiles(getFileTypeFilter());
            assert files != null;
            List<String> fileList = Arrays.stream(files).map(File::getPath).collect(Collectors.toList());
            if((sortTypeEnum == SortTypeEnum.BY_DURATION) || (sortTypeEnum == SortTypeEnum.BY_SIZE)){
                paths.addAll(fileList.stream().filter((e)->(new File(e)).isDirectory()).collect(Collectors.toList()));
                paths.addAll(fileList.stream().filter((e)->!(new File(e)).isDirectory()).collect(Collectors.toList()));
            }else {
                paths.addAll(sortStrings(fileList));
            }
        }catch(Exception e){ e.printStackTrace();}
        return paths;
    }

    private FileFilter getFileTypeFilter(){
        return (file1)->
                file1.isDirectory() || Util.isVideoExtension(file1.getPath()) || Util.isAudioExtension(file1.getPath());
    }

    public void setCurrentDirectory(String path){
        if(path.equals(File.separator + "storage" + File.separator + "emulated")){
            path += File.separator + "0";
        }
        currentDirectory = path;
        file = new File(path);
        materialToolbar.setTitle(fitPathToToolbarWidth());
        materialToolbar.setSubtitle(file.getName());
        MediaFileObserver mediaFileObserver = new MediaFileObserver(path);
        mediaFileObserver.setHandler(fileHandler);
        mediaFileObserverManager.setMediaFileObserver(mediaFileObserver);
        mediaFileObserverManager.setPathAdded(true);
        try{
            mediaFileObserverManagerThread.interrupt();
        }catch(SecurityException securityException){
            securityException.printStackTrace();
        }
    }

    private String fitPathToToolbarWidth(){
        String path = file.getPath();
        if(toolbarWidth == 0){
            int insets = materialToolbar.getContentInsetStart() + materialToolbar.getContentInsetEnd();
            int margins = materialToolbar.getTitleMarginStart() + materialToolbar.getTitleMarginEnd();
            toolbarWidth = materialToolbar.getWidth() - (insets + margins);
        }
        int length = path.length();
        for(int i = 0; i < length; ++i){
            if(paint.measureText(path, i, length) < toolbarWidth){
                if(i == 0){
                    break;
                }
                return "...".concat(path.substring(i+4, length));
            }
        }
        return path;
    }
    public void openFileInEditor(String path){
        Intent intent = new Intent(this, EditFileActivity.class);
        intent.putExtra("path", path);
        startActivity(intent);
    }

    public void addPreviousFiles(int position){
        previousPositions.push(position);
        int dirIndex = file.getPath().split(File.separator).length;
        FolderNode folderNode = new FolderNode(dirIndex, file.getPath());
        ArrayList<MediaFile> mediaFiles = new ArrayList<>(this.mediaFiles);
        folderNode.setChildren(mediaFiles);
        previousFolders.push(folderNode);
    }

    protected void setMediaFilesOnlyCount(){
        if(mediaFiles != null){
            mediaFilesOnlyCount = mediaFiles.stream().filter((e)->e.isAudio() || e.isVideo()).count();
        }
    }
}