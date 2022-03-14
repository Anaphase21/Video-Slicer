package com.anaphase.videoeditor.ui.browser;

import static com.anaphase.videoeditor.mediafile.MediaStoreTable.mediaStoreTable;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.FileProvider;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.anaphase.videoeditor.mediafile.MediaStoreTable;
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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BaseFileBrowserActivity extends AppCompatActivity implements AlertDialogBox.AlertDialogListener {

    private static AppBarLayout appBarLayout;

    protected volatile ArrayList<MediaFile> mediaFiles;
    protected RecyclerView recyclerView;
    protected RecyclerView.Adapter<MediaFilesRecyclerViewAdapter.MediaFileViewHolder> recyclerViewAdapter;
    protected RecyclerView.LayoutManager recyclerViewLayoutManager;
    protected BlockingQueue<Runnable> tasks;
    protected ThreadPoolExecutor threadPoolExecutor;
    protected final int CORE_POOL_SIZE = 3;
    protected final int MAX_POOL_SIZE = Integer.MAX_VALUE;
    protected long KEEP_ALIVE_TIME = 1L;
    protected TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    public Handler handler;
    private Bundle bundle;
    protected LoadingWheel loadingWheel;
    protected Handler mediaStoreWorkerHandler;

    protected ActionMode actionMode;
    private ActionMode.Callback actionModeCallback;

    private final int shareItemId = R.id.share_item;
    private final int selectAllItemId = R.id.select_all_item;
    private final int deleteItemId = R.id.delete_item;

    private final int disabledShareIcon = R.drawable.ic_baseline_share_disabled_24;

    private MaterialToolbar topToolbar;

    private MenuItem sortByNameMenuItem;
    private MenuItem sortByDateModifiedMenuItem;
    private MenuItem sortByFileSizeMenuItem;
    private MenuItem sortByDurationMenuItem;
    protected SortTypeEnum sortTypeEnum;

    private String sortByPreferencesKey;

    protected long mediaFilesOnlyCount;

    protected String currentDirectory;

    private final int DELETE_REQUEST_CODE = 0xff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_file_browser);
        appBarLayout = findViewById(R.id.top_toolbar_layout);
        topToolbar = findViewById(R.id.top_toolbar);
        loadingWheel = findViewById(R.id.loading_wheel);
        if(!((this instanceof MediaStoreFileBrowser) && (mediaStoreTable == null))){
            loadingWheel.setVisibility(View.GONE);
        }
        setHandler();
        recyclerView = findViewById(R.id.media_files_recycler_view);
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
        //recyclerView.removeAllViews();
        File file;
        for(int i = 0; i < size; ++i) {
            path = paths.get(i);
            mediaFile = new MediaFile();
            mediaFile.setContext(this);
            mediaFile.setPath(path);
            file = new File(path);
            if(Environment.getExternalStorageDirectory().getPath().equals(path)){
                mediaFile.setFileName("Phone Storage");
            }else if(path.split(File.separator).length == 3){
                mediaFile.setFileName("SD Card");
            }else{
                mediaFile.setFileName(file.getName());
            }
            mediaFiles.add(mediaFile);
            if(mediaFile.isDirectory()){
                threadPoolExecutor.execute(new MediaFileInformationFetcher(mediaFile, handler, i, true));
            }else {
                threadPoolExecutor.execute(new MediaFileInformationFetcher(mediaFile, handler, i, false));
            }
        }
    }

    public void initialiseThreadPoolExecutor(){
        if((threadPoolExecutor == null) || (threadPoolExecutor.isShutdown())){
            threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TIME_UNIT, tasks);
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
                }else if(mediaFilePosition == mediaFiles.size() - 1){
                    List<MediaFile> sortedMediaFiles = sortMediaFiles(mediaFiles);
                    mediaFiles.clear();
                    mediaFiles.addAll(sortedMediaFiles);
                    recyclerViewAdapter.notifyDataSetChanged();
                }
            }
        };
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
                        if(filesToShare.stream().filter(e->(new File(e.getPath())).isDirectory()).count() > 0){
                            showInfoMessage("Can't share folders. Please unselect the folders.");
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
                        Bundle bundle = new Bundle();
                        int numOfSelectedFiles = mediaFiles.stream().filter(MediaFile::isChecked).toArray().length;
                        if(numOfSelectedFiles == 0){
                            showInfoMessage("No item selected. Please select an item.");
                            return false;
                        }
                        AlertDialogBox dialog = new AlertDialogBox();
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
        MediaFile mediaFile;
        int size = mediaFiles.size();
        int numFilesToBeRemoved = 0;
        int firstIndex = -1;
        int idx = 0;
        File file = null;
        while(idx < size){
            mediaFile = mediaFiles.get(idx);
            if(mediaFile.isChecked()) {
                file = new File(mediaFile.getPath());
                mediaFile.toggle();
                if(file.exists()) {
                    if(file.isDirectory()) {
                        recursiveDelete(mediaFile.getPath(), true);
                    }else{
                        if(mediaFile.getUri() != null) {
                            getContentResolver().delete(mediaFile.getUri(), null, null);
                        }else{
                            try{
                                file.delete();
                            }catch(SecurityException securityException){}
                        }
                        if(file.exists() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)){
                            try{
                                file.delete();
                            }catch(SecurityException securityException){}
                        }
                    }
                }else{
                    if(!file.isDirectory()) {
                        getContentResolver().delete(mediaFile.getUri(), null, null);
                    }
                }
                mediaFiles.remove(idx);
                ++numFilesToBeRemoved;
                --size;
                if(firstIndex < 0){
                    firstIndex = idx;
                }
                recyclerViewAdapter.notifyItemRemoved(idx);
                if(size == 0){
                    break;
                }
                continue;
            }
            ++idx;
        }
        if(file != null) {
            removeDeletedMediaFilesFromMediaStoreTable(file.getPath());
        }
        Toast.makeText(this, numFilesToBeRemoved+" file" + (numFilesToBeRemoved > 1 ? "s deleted" : " deleted"), Toast.LENGTH_SHORT).show();
        actionMode.finish();
    }

    private void removeDeletedMediaFilesFromMediaStoreTable(String path){
        if(mediaStoreTable != null) {
            File file = new File(path);
            if(file.isDirectory()){
                mediaStoreTable.remove(path);
            }else{
                ArrayList<MediaFile> mediaFiles = mediaStoreTable.get(file.getParent());
                if(mediaFiles != null) {
                    int size = mediaFiles.size();
                    for(int i = 0; i < size; ++i) {
                        file = new File(mediaFiles.get(i).getPath());
                        if(!file.exists()) {
                            mediaFiles.remove(i);
                            --i;
                            --size;
                        }
                    }
                }
                mediaStoreTable.put(file.getParent(), mediaFiles);
            }
        }
    }
    //Returns true if a file is deleted, and false, otherwise.
    //recursiveDelete will delete directories in "path", but
    //not "path" itself (that will be done by delete method).
    //deleteTopDirectoryOfPath indicates whether recursiveDelete
    //will delete the passed "path" or not.
    private boolean recursiveDelete(String path, boolean deleteTopDirectoryOfPath){
        File[] filesArray;
        if(this instanceof MediaStoreFileBrowser){
            ArrayList<MediaFile> filesToDelete = mediaStoreTable.get(path);
            if(filesToDelete != null) {
                for (MediaFile mediaFile : filesToDelete) {
                    getContentResolver().delete(mediaFile.getUri(), null, null);
                }
            }
            mediaStoreTable.remove(path);
            return true;
        }
        if(path != null){
           filesArray = (new File(path)).listFiles((file)->
                   file.isDirectory() || Util.isVideoExtension(file.getPath()) || Util.isAudioExtension(file.getPath()));
           if(filesArray != null && filesArray.length == 0) {
               return false;
           }
           if(filesArray != null) {
               for (File file : filesArray) {
                   if (file.exists()) {
                       if (file.isDirectory()) {
                           if(this instanceof FileBrowserActivity) {
                               recursiveDelete(file.getPath(), true);
                           }
                       } else {
                           if (this instanceof FileBrowserActivity) {
                               file.delete();
                           }
                       }
                   }
               }
           }
           if(deleteTopDirectoryOfPath) {
               File file = new File(path);
               if (file.exists()) {
                   try {
                       file.delete();
                   }catch (SecurityException securityException){
                       securityException.printStackTrace();
                   }
               }
           }
        }
        return true;
    }

    public ArrayList<Uri> getFilesToShare(){
        ArrayList<Uri> urisToShare = new ArrayList<>();
        Uri uri;
        String path;
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
        MediaFilesRecyclerViewAdapter mediaFilesRecyclerViewAdapter = (MediaFilesRecyclerViewAdapter)recyclerViewAdapter;
        MediaFile mediaFile;
        int numSelectedDirs = 0;
        for(int i = 0; i < size; ++i){
            mediaFile = mediaFiles.get(i);
            mediaFile.setChecked(selectAll);
            if(mediaFile.isDirectory() && mediaFile.isChecked()){
                ++numSelectedDirs;
            }
        }
        mediaFilesRecyclerViewAdapter.setNumSelectedDirs(numSelectedDirs);
        if(numSelectedDirs > 0) {
            MenuItem shareMenuItem = actionMode.getMenu().findItem(shareItemId);
            if (shareMenuItem.isEnabled()) {
                shareMenuItem.setEnabled(false);
                shareMenuItem.setIcon(disabledShareIcon);
            }
        }
    }

    public void startOpenFileActivity(String path){
        if(path != null) {
            Uri uri = FileProvider.getUriForFile(this, "com.anaphase.videoeditor.fileprovider", new File(path));
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String extension = Util.getExtension(path).toLowerCase();
            intent.setDataAndType(uri, mimeTypeMap.getMimeTypeFromExtension(extension));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent.createChooser(intent, "Open with");
            if(intent.resolveActivity(getPackageManager()) != null){
                startActivity(intent);
            }
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
            if(sortTypeEnum == SortTypeEnum.BY_SIZE){
                return true;
            }
            sortTypeEnum = SortTypeEnum.BY_SIZE;
            List<MediaFile> directories = mediaFiles.stream().filter(MediaFile::isDirectory).collect(Collectors.toList());
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
            if(sortTypeEnum == SortTypeEnum.BY_DURATION){
                return true;
            }
            sortTypeEnum = SortTypeEnum.BY_DURATION;
            List<MediaFile> directories = mediaFiles.stream().filter(MediaFile::isDirectory).collect(Collectors.toList());
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
        mediaFiles.addAll(list.stream().filter(MediaFile::isDirectory).sorted(SortComparators.getMediaFileListComparator(sortTypeEnum)).collect(Collectors.toList()));
        mediaFiles.addAll(list.stream().filter((e)
                ->!e.isDirectory()).sorted(SortComparators.getMediaFileListComparator(sortTypeEnum)).collect(Collectors.toList()));
        return mediaFiles;
    }

    protected ArrayList<String> sortStrings(List<String> strings){
        ArrayList<String> sortedPaths = new ArrayList<>(strings.size());
        sortedPaths.addAll(strings.stream().filter((e)->(new File(e)).isDirectory())
                .sorted(SortComparators.getStringListComparator
                        (sortTypeEnum == SortTypeEnum.BY_DURATION ? SortTypeEnum.BY_NAME : sortTypeEnum))
                .collect(Collectors.toList()));
        sortedPaths.addAll(strings.stream().filter((e)->!(new File(e)).isDirectory())
                .sorted(SortComparators.getStringListComparator(sortTypeEnum)).collect(Collectors.toList()));
        return sortedPaths;
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q){
            List<Uri> urisToDelete = mediaFiles.stream().filter(MediaFile::isChecked).map(MediaFile::getUri).collect(Collectors.toList());
            PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), urisToDelete);
            try {
                startIntentSenderForResult(pendingIntent.getIntentSender(), DELETE_REQUEST_CODE, null, 0, 0, 0);
            }catch(IntentSender.SendIntentException sendIntentException){
                sendIntentException.printStackTrace();
            }
        }else {
            deleteSelectedItems();
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog){

    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialog){

    }

    @Override
    public void onCancel(DialogInterface dialog){

    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putString("current_path", currentDirectory);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == DELETE_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                int size = mediaFiles.size();
                int numFilesRemoved = 0;
                MediaFile mediaFile = null;
                for(int i = 0; i < size; ++i){
                    mediaFile = mediaFiles.get(i);
                    if(mediaFile.isChecked()){
                        mediaFiles.remove(i);
                        recyclerViewAdapter.notifyItemRemoved(i);
                        --size;
                        --i;
                        ++numFilesRemoved;
                    }
                }
                String toastMessage = numFilesRemoved + " file";
                toastMessage += (numFilesRemoved > 1) ? "s deleted" : " deleted";
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
                if(mediaFile != null) {
                    removeDeletedMediaFilesFromMediaStoreTable(mediaFile.getPath());
                }
                actionMode.finish();
            }
        }
    }
}