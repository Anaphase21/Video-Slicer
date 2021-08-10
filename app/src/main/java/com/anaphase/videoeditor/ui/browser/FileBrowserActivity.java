package com.anaphase.videoeditor.ui.browser;

import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

public class FileBrowserActivity extends BaseFileBrowserActivity {

    private File file = new File("/storage");
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
                ArrayList<MediaFile> currMediaFiles = new ArrayList<>();
                int currPosition = 0;
                directoriesFilesLoadedCount.remove(file.getPath());
                if((!file.getPath().equals("/storage"))) {
                    do {
                        file = file.getParentFile();
                        currentDirectory = file.getPath();
                        if (file.getPath().equals("/storage/emulated")) {
                            file = file.getParentFile();
                            currentDirectory = file.getPath();
                            if(!previousFolders.isEmpty()) {
                                currMediaFiles = previousFolders.pop().getChildren();
                            }else{
                                //file = file.getParentFile();
                                //currentDirectory = file.getPath();
                                setCurrentDirectory(file.getPath());
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
                    Integer count = directoriesFilesLoadedCount.get(file.getPath());
                    if((count != null) && (count == mediaFilesOnlyCount)){
                        enableMenuItems();
                    }
                    recyclerView.scrollToPosition(currPosition);
                    recyclerViewAdapter.notifyDataSetChanged();
                    materialToolbar.setTitle(fitPathToToolbarWidth());
                    materialToolbar.setSubtitle(file.getName());
                    mediaFileObserverManager.setPathRemoved(true);
                    try{
                        mediaFileObserverManagerThread.interrupt();
                    }catch(SecurityException securityException){}
                }else{
                    mediaFileObserverManager.clearObservers();
                    try{
                        mediaFileObserverManagerThread.interrupt();
                    }catch(SecurityException securityException){}
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
                Bundle bundle = message.getData();
                String path = bundle.getString("-fileChange", null);
                path = bundle.getString("+fileChange", null);
                if(path != null){
                    int dirrDepthIndex = path.split("/").length - 1;
                    File fileAdded = new File(path);
                    String directory = fileAdded.getParent();
                    for(FolderNode folderNode : previousFolders){
                        if((folderNode.getDepthPosition() == dirrDepthIndex) || (file.getPath().equals(directory))){
                            folderNode.setPath(folderNode.getPath());
                            MediaFile mediaFile = new MediaFile();
                            mediaFile.setContext(FileBrowserActivity.this);
                            mediaFile.setPath(path);
                            mediaFile.setFileName(fileAdded.getName());
                            if(threadPoolExecutor.isShutdown()) {
                                threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TIME_UNIT, tasks);
                            }
                            //If the file being created or moved to is in the current directory, then add it to the mediaFiles list
                            if(file.getPath().equals(directory)){
                                int position = mediaFiles.size();
                                mediaFiles.add(mediaFile);
                                recyclerViewAdapter.notifyItemInserted(position);
                                threadPoolExecutor.execute(new MediaFileInformationFetcher(mediaFile, handler, position));
                                //Otherwise add it to the appropriate directory.
                            }else {
                                folderNode.addChild(mediaFile);
                                threadPoolExecutor.execute(new MediaFileInformationFetcher(mediaFile, null, -1));
                            }
                            break;
                        }
                    }
                }
            }
        };
    }

    public ArrayList<String> listFiles(){
        String path = file.getPath();
        ArrayList<String> paths = new ArrayList<>();
        if(path.equals("/storage/emulated")){
            file = new File(path+File.separator+"0");
        }
        try {
            File[] files = file.listFiles(getFileTypeFilter());
            List<String> fileList = Arrays.stream(files).map((file)->file.getPath()).collect(Collectors.toList());
            if((sortTypeEnum == SortTypeEnum.BY_DURATION) || (sortTypeEnum == SortTypeEnum.BY_SIZE)){
                paths.addAll(fileList.stream().filter((e)->(new File(e)).isDirectory()).collect(Collectors.toList()));
                paths.addAll(sortStrings(fileList.stream().filter((e)->!(new File(e)).isDirectory()).collect(Collectors.toList())));
            }else {
                paths.addAll(sortStrings(fileList));
            }
        }catch(Exception e){ e.printStackTrace();}
        return paths;
    }

    private FileFilter getFileTypeFilter(){
        FileFilter filter = (file)->{
            if(file != null){
                String name = file.getPath().toLowerCase();
                if(file.getPath().equals("/storage/self")){
                    return false;
                }
                if (file.isDirectory() || Util.isVideoExtension(name) || Util.isAudioExtension(name)) {
                    return true;
                }
            }
            return false;
        };
        return filter;
    }

    public void setCurrentDirectory(String path){
        if(path.equals("/storage/emulated")){
            path += File.separator+"0";
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
        }catch(SecurityException securityException){}
        disableMenuItems();
    }

    protected String getCurrentDirectory(){
        return currentDirectory;
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
        ArrayList<MediaFile> mediaFiles = new ArrayList<>();
        int dirrIndex = file.getPath().split("/").length;
        FolderNode folderNode = new FolderNode(dirrIndex, file.getPath());
        mediaFiles.addAll(this.mediaFiles);
        folderNode.setChildren(mediaFiles);
        previousFolders.push(folderNode);
    }

    protected void setMediaFilesOnlyCount(){
        if(mediaFiles != null){
            mediaFilesOnlyCount = mediaFiles.stream().filter((e)->e.isAudio() || e.isVideo()).count();
        }
    }

    /*public void removePreviousFiles(){
        if(previousFiles.size() > 0) {
            previousFiles.pop();
            previousPositions.pop();
        }
    }**/
}