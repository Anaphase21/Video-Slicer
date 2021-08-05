package com.anaphase.videoeditor.ui.browser;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.FileProvider;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.webkit.MimeTypeMap;

import com.anaphase.videoeditor.ui.AlertDialogBox;
import com.anaphase.videoeditor.R;
import com.anaphase.videoeditor.mediafile.MediaFile;
import com.anaphase.videoeditor.mediafile.MediaFileInformationFetcher;
import com.anaphase.videoeditor.util.Settings;
import com.anaphase.videoeditor.util.SortComparators;
import com.anaphase.videoeditor.util.SortTypeEnum;
import com.anaphase.videoeditor.util.Util;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BaseFileBrowserActivity extends AppCompatActivity implements AlertDialogBox.AlertDialogListener {

    private static AppBarLayout appBarLayout;

    protected volatile ArrayList<MediaFile> mediaFiles;
    protected RecyclerView recyclerView;
    protected RecyclerView.Adapter recyclerViewAdapter;
    protected RecyclerView.LayoutManager recyclerViewLayoutManager;
    protected BlockingQueue<Runnable> tasks;
    protected ThreadPoolExecutor threadPoolExecutor;
    protected final int CORE_POOL_SIZE = 3;
    protected final int MAX_POOL_SIZE = Integer.MAX_VALUE;
    protected long KEEP_ALIVE_TIME = 1L;
    protected TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    public Handler handler;
    private Bundle bundle;
    protected Handler mediaStoreWorkerHandler;
    protected Map<String, Integer> directoriesFilesLoadedCount;

    protected ActionMode actionMode;
    private ActionMode.Callback actionModeCallback;

    private ArrayList<String> filesToShare;

    private final int shareItemId = R.id.share_item;
    private final int selectAllItemId = R.id.select_all_item;
    private final int deleteItemId = R.id.delete_item;

    private MaterialToolbar topToolbar;

    private MenuItem sortByNameMenuItem;
    private MenuItem sortByDateModifiedMenuItem;
    private MenuItem sortByFileSizeMenuItem;
    private MenuItem sortByDurationMenuItem;
    protected SortTypeEnum sortTypeEnum;

    private String sortByPreferencesKey;

    private int numFilesFullyLoaded;
    protected long mediaFilesOnlyCount;

    protected String currentDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_file_browser);
        appBarLayout = (AppBarLayout)findViewById(R.id.top_toolbar_layout);
        topToolbar = (MaterialToolbar)findViewById(R.id.top_toolbar);
        setHandler();
        recyclerView = (RecyclerView)findViewById(R.id.media_files_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerViewLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerViewLayoutManager);
        mediaFiles = new ArrayList<>();
        setActionModeCallback();
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true){
            @Override
            public void handleOnBackPressed(){
                if(threadPoolExecutor != null) {
                    threadPoolExecutor.shutdownNow();
                    recyclerViewAdapter.notifyDataSetChanged();
                    recyclerView.removeAllViews();
                    handler.removeCallbacksAndMessages(null);
                    finish();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
        directoriesFilesLoadedCount = new HashMap<>(5);
        sortByPreferencesKey = getString(R.string.sort_by);
        inflateTopToolbarMenu();
    }

    public void populateMediaFiles(ArrayList<String> paths){
        MediaFile mediaFile;
        mediaFiles.clear();
        tasks = new LinkedBlockingQueue<>(paths.size() == 0 ? 1 : paths.size());
        threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TIME_UNIT, tasks);
        String path;
        int size = paths.size();
        recyclerViewAdapter.notifyDataSetChanged();
        for(int i = 0; i < size; ++i) {
            path = paths.get(i);
            mediaFile = new MediaFile();
            mediaFile.setContext(this);
            mediaFile.setPath(path);
            mediaFile.setFileName((new File(path)).getName());
            mediaFiles.add(mediaFile);
            threadPoolExecutor.execute(new MediaFileInformationFetcher(mediaFile, handler, i));
        }
    }

    private void setHandler(){
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message){
                bundle = message.getData();
                int mediaFilePosition = bundle.getInt("loadingComplete", -1);
                if((mediaFilePosition > -1) && (recyclerViewAdapter != null)){
                    recyclerViewAdapter.notifyItemChanged(mediaFilePosition);
                }else{
                    String fileParentDirectory = bundle.getString("FILE_LOADED", null);
                    if(fileParentDirectory != null){
                        increaseFilesLoadedCount(fileParentDirectory);
                        Integer count = directoriesFilesLoadedCount.get(fileParentDirectory);
                        if(BaseFileBrowserActivity.this instanceof FileBrowserActivity){
                            FileBrowserActivity fba = (FileBrowserActivity)BaseFileBrowserActivity.this;
                            if(count != null){
                                if((count == mediaFilesOnlyCount) && (fba.getCurrentDirectory().equals(fileParentDirectory))){
                                    enableMenuItems();
                                }
                            }
                        }else if(BaseFileBrowserActivity.this instanceof MediaStoreFileBrowser){
                            if(count != null){
                                if(count == mediaFiles.size()){
                                    enableMenuItems();
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    private void increaseFilesLoadedCount(String fileParentDirectory){
        Integer count = 0;
        if(directoriesFilesLoadedCount != null) {
            count = directoriesFilesLoadedCount.get(fileParentDirectory);
            if(count != null){
                directoriesFilesLoadedCount.put(fileParentDirectory, ++count);
            }else{
                directoriesFilesLoadedCount.put(fileParentDirectory, 1);
            }
        }
    }

    private void removeDirectoryCount(String fileParentDirectory){
        directoriesFilesLoadedCount.remove(fileParentDirectory);
    }

    private void retrieveSettings(){
        int sortBy = Settings.retrieve(this, this.sortByPreferencesKey);
        switch(sortBy){
            case 0:
            case -1:
                sortTypeEnum = SortTypeEnum.BY_NAME;
                sortByNameMenuItem.setChecked(true);
                break;
            case 1:
                sortTypeEnum = SortTypeEnum.BY_DATE;
                sortByDateModifiedMenuItem.setChecked(true);
                break;
            case 2:
                sortTypeEnum = SortTypeEnum.BY_SIZE;
                sortByFileSizeMenuItem.setChecked(true);
                break;
            case 3:
                sortTypeEnum = SortTypeEnum.BY_DURATION;
                sortByDurationMenuItem.setChecked(true);
                break;
        }
    }

    protected void resetNumFilesFullyLoaded(){
        numFilesFullyLoaded = 0;
    }

    private void setActionModeCallback(){
        actionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.share_contextual_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch(item.getItemId()){
                    case shareItemId:
                        ArrayList<Uri> filesToShare = getFilesToShare();
                        if((filesToShare == null) || (filesToShare.size() == 0)){
                            showInfoMessage("No item selected. Please select an item.");
                            return false;
                        }
                        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, filesToShare);
                        String extension = Util.getExtension(filesToShare.get(0).toString()).toLowerCase();
                        intent.setType(mimeTypeMap.getMimeTypeFromExtension(extension));
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        Intent.createChooser(intent, "Share with");
                        if(intent.resolveActivity(getPackageManager()) != null){
                            startActivity(intent);
                        }
                        break;
                    case selectAllItemId:
                        if(!item.isCheckable()){
                            item.setCheckable(true);
                            item.setChecked(true);
                        }
                        selectUnselectAllItems(item.isChecked());
                        item.setChecked(!item.isChecked());
                        break;
                    case deleteItemId:
                        AlertDialogBox dialog = new AlertDialogBox();
                        Bundle bundle = new Bundle();
                        int numOfSelectedFiles = mediaFiles.stream().filter(e->{return e.isChecked();}).toArray().length;
                        if(numOfSelectedFiles == 0){
                            showInfoMessage("No item selected. Please select an item.");
                            return false;
                        }
                        String title = "Delete Selected File" + (numOfSelectedFiles == 1 ? "?" : "s?");
                        String message = "The selected file" + (numOfSelectedFiles == 1 ? "" : "s") + " will be deleted permanently from your device. Do you want to proceed?";
                        bundle.putString("title", title);
                        bundle.putString("message", message);
                        bundle.putString("positiveButton", "Yes");
                        bundle.putString("negativeButton", "No");
                        dialog.setArguments(bundle);
                        dialog.show(getFragmentManager(), "");
                        break;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                selectUnselectAllItems(false);
                ((MediaFilesRecyclerViewAdapter)recyclerViewAdapter).setMultichoiceMode(false);
                actionMode = null;
            }
        };
    }

    public void showActionMode(){
        actionMode = startSupportActionMode(actionModeCallback);
    }

    public void deleteSelectedItems(){
        MediaFile mediaFile = null;
        int size = mediaFiles.size();
        int idx = 0;
        File file = null;
        while(idx < size){
            mediaFile = mediaFiles.get(idx);
            if(mediaFile.isChecked()) {
                file = new File(mediaFile.getPath());
                mediaFile.toggle();
                if(file.exists()) {
                    if(file.isDirectory()) {
                        recursiveDelete(mediaFile.getPath(), false);
                    }
                    if(!file.isDirectory()) {
                        if(mediaFile.getUri() == null){
                            try {
                                file.delete();
                            }catch (SecurityException securityException){}
                        }else {
                            getContentResolver().delete(mediaFile.getUri(), null, null);
                        }
                    }
                }else{
                    if(!file.isDirectory()) {
                        if(mediaFile.getUri() == null){
                            try{
                                file.delete();
                            }catch(SecurityException securityException){}
                        }else {
                            getContentResolver().delete(mediaFile.getUri(), null, null);
                        }
                    }
                }
                mediaFiles.remove(idx);
                --size;
                recyclerViewAdapter.notifyItemRemoved(idx);
                if(size == 0){
                    break;
                }
                continue;
            }
            ++idx;
        }
        actionMode.finish();
    }

    //Returns true if a file is deleted, and false, otherwise.
    //recursiveDelete will delete directories in "path", but
    //not "path" itself (that will be done by delete method).
    //deleteTopDirectoryOfPath indicates whether recursiveDelete
    //will delete the passed "path" or not.
    private boolean recursiveDelete(String path, boolean deleteTopDirectoryOfPath){
        File[] filesArray;
        if(this instanceof MediaStoreFileBrowser){
            ArrayList<MediaFile> filesToDelete = ((MediaStoreFileBrowser)this).mediaStoreTable.get(path);
            for(MediaFile mediaFile : filesToDelete){
                getContentResolver().delete(mediaFile.getUri(), null, null);
            }
            try{
                File file = new File(path);
                file.delete();
            }catch(SecurityException securityException){}
            return true;
        }
        if(path != null){
           filesArray = (new File(path)).listFiles();
           if(filesArray.length == 0) {
               return false;
           }
           for(File file : filesArray){
               if(file.exists()){
                   if(file.isDirectory()){
                       recursiveDelete(file.getPath(), true);
                   }else{
                       if(this instanceof FileBrowserActivity){
                           Handler handler = ((FileBrowserActivity)this).fileHandler;
                           Bundle bundle = new Bundle();
                           bundle.putString("-fileChange", file.getPath());
                           Message message = handler.obtainMessage();
                           message.setData(bundle);
                           handler.sendMessage(message);
                       }
                   }
               }
           }
           if(deleteTopDirectoryOfPath) {
               File file = new File(path);
               if (file.exists()) {
                   try {
                       file.delete();
                   }catch (SecurityException securityException){}
               }
           }
        }
        return true;
    }

    public ArrayList<Uri> getFilesToShare(){
        ArrayList<Uri> urisToShare = new ArrayList<>();
        Uri uri = null;
        String path = null;
        for(MediaFile mediaFile : mediaFiles){
            if(mediaFile.isChecked()) {
                path = mediaFile.getPath();
                uri = FileProvider.getUriForFile(this, "com.anaphase.videoeditor.fileprovider", new File(path));
                urisToShare.add(uri);
            }
        }
        return urisToShare;
    }

    private void showInfoMessage(String message){
        Snackbar snackbar = Snackbar.make(recyclerView, message, 1300);
        snackbar.show();
    }

    private void selectUnselectAllItems(boolean selectAll){
        int size = recyclerViewAdapter.getItemCount();
        for(int i = 0; i < size; ++i){
            ((MediaFilesRecyclerViewAdapter)recyclerViewAdapter).mediaFiles.get(i).setChecked(selectAll);
        }
    }

    private static class CustomRecyclerView extends RecyclerView{
        public CustomRecyclerView(Context context){
            super(context);
        }

        public CustomRecyclerView(Context context, AttributeSet attrs){
            super(context, attrs);
        }

        public CustomRecyclerView(Context context, AttributeSet attrs, int defStyle){
            super(context, attrs, defStyle);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
            int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
            int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(parentHeight - appBarLayout.getHeight(), MeasureSpec.EXACTLY));
            setMeasuredDimension(parentWidth, parentHeight - appBarLayout.getMeasuredHeight());
        }
    }

    private void inflateTopToolbarMenu(){
        Menu menu = topToolbar.getMenu();
        SubMenu sortTypeSubMenu = menu.addSubMenu("Sort files by");

        sortTypeSubMenu.add("Name");
        sortTypeSubMenu.add("Date");
        sortTypeSubMenu.add("Size");
        sortTypeSubMenu.add("Duration");

        sortTypeSubMenu.setGroupCheckable(0, true, true);

        sortByNameMenuItem = sortTypeSubMenu.getItem(0);
        sortByDateModifiedMenuItem = sortTypeSubMenu.getItem(1);
        sortByFileSizeMenuItem = sortTypeSubMenu.getItem(2);
        sortByDurationMenuItem = sortTypeSubMenu.getItem(3);
        retrieveSettings();

        sortByNameMenuItem.setOnMenuItemClickListener((item->{
            sortByNameMenuItem.setChecked(true);
            if(sortTypeEnum == SortTypeEnum.BY_NAME){
                return true;
            }
            sortTypeEnum = SortTypeEnum.BY_NAME;
            ArrayList<MediaFile> sortedMediaFiles = sortMediaFiles(this.mediaFiles);
            this.mediaFiles.clear();
            this.mediaFiles.addAll(sortedMediaFiles);
            recyclerViewAdapter.notifyDataSetChanged();
            Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));

        sortByDateModifiedMenuItem.setOnMenuItemClickListener((item->{
            sortByDateModifiedMenuItem.setChecked(true);
            if(sortTypeEnum == SortTypeEnum.BY_DATE){
                return true;
            }
            sortTypeEnum = SortTypeEnum.BY_DATE;
            ArrayList<MediaFile> sortedMediaFiles = sortMediaFiles(this.mediaFiles);
            this.mediaFiles.clear();
            this.mediaFiles.addAll(sortedMediaFiles);
            recyclerViewAdapter.notifyDataSetChanged();
            Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));

        sortByFileSizeMenuItem.setOnMenuItemClickListener((item->{
            sortByFileSizeMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_SIZE;
            List<MediaFile> directories = mediaFiles.stream().filter((e)->e.isDirectory()).collect(Collectors.toList());
            ArrayList<MediaFile> sortedMediaFiles = sortMediaFiles(mediaFiles.stream().filter((e)
                    ->!e.isDirectory()).collect(Collectors.toList()));
            mediaFiles.clear();
            mediaFiles.addAll(directories);
            mediaFiles.addAll(sortedMediaFiles);
            recyclerViewAdapter.notifyDataSetChanged();
            Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));

        sortByDurationMenuItem.setOnMenuItemClickListener((item->{
            sortByDurationMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_DURATION;
            List<MediaFile> directories = mediaFiles.stream().filter((e)->e.isDirectory()).collect(Collectors.toList());
            ArrayList<MediaFile> sortedMediaFiles = sortMediaFiles(mediaFiles.stream().filter((e)
                    ->!e.isDirectory()).collect(Collectors.toList()));
            mediaFiles.clear();
            mediaFiles.addAll(directories);
            mediaFiles.addAll(sortedMediaFiles);
            recyclerViewAdapter.notifyDataSetChanged();
            Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));
    }

    protected ArrayList<MediaFile> sortMediaFiles(List<MediaFile> list){
        ArrayList<MediaFile> mediaFiles = new ArrayList<>(list.size());
        mediaFiles.addAll(list.stream().filter((e)
                ->e.isDirectory()).sorted(SortComparators.getMediaFileListComparator(sortTypeEnum)).collect(Collectors.toList()));
        mediaFiles.addAll(list.stream().filter((e)
                ->!e.isDirectory()).sorted(SortComparators.getMediaFileListComparator(sortTypeEnum)).collect(Collectors.toList()));
        return mediaFiles;
    }

    protected ArrayList<String> sortStrings(List<String> strings){
        ArrayList<String> sortedPaths = new ArrayList<>(strings.size());
        sortedPaths.addAll(strings.stream().filter((e)->(new File(e)).isDirectory())
                .sorted(SortComparators.getStringListComparator(sortTypeEnum)).collect(Collectors.toList()));
        sortedPaths.addAll(strings.stream().filter((e)->!(new File(e)).isDirectory())
                .sorted(SortComparators.getStringListComparator(sortTypeEnum)).collect(Collectors.toList()));
        return sortedPaths;
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog){
        deleteSelectedItems();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog){
        return;
    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialog){
        return;
    }

    @Override
    public void onCancel(DialogInterface dialog){
        return;
    }

    protected void enableMenuItems(){
        if(topToolbar != null){
            topToolbar.getMenu().getItem(0).setEnabled(true);
        }
    }

    protected void disableMenuItems(){
        if(topToolbar != null){
            topToolbar.getMenu().getItem(0).setEnabled(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putString("current_path", currentDirectory);
        super.onSaveInstanceState(savedInstanceState);
    }
}