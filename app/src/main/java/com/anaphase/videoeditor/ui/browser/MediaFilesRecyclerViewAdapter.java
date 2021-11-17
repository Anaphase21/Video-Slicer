package com.anaphase.videoeditor.ui.browser;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.anaphase.videoeditor.R;
import com.anaphase.videoeditor.mediafile.MediaFile;
import com.anaphase.videoeditor.mediafile.MediaFileInformationFetcher;
import com.anaphase.videoeditor.util.Util;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

public class MediaFilesRecyclerViewAdapter extends RecyclerView.Adapter<MediaFilesRecyclerViewAdapter.MediaFileViewHolder> {

    public ArrayList<MediaFile> mediaFiles;
    private boolean multichoiceMode = false;
    private int numSelectedDirs;
    //private int numMediaFilesSelected;
    private BaseFileBrowserActivity baseFileBrowserActivity = null;

    private final int disabledShareIcon = R.drawable.ic_baseline_share_disabled_24;
    private final int enabledShareIcon = R.drawable.ic_baseline_share_24;
    private final int shareItemId = R.id.share_item;

    public static class MediaFileViewHolder extends RecyclerView.ViewHolder{
        public MaterialCardView mediaCardView;
        public MediaFileViewHolder(MaterialCardView mediaCardView){
            super(mediaCardView);
            this.mediaCardView = mediaCardView;
        }
    }

    public MediaFilesRecyclerViewAdapter(ArrayList<MediaFile> mediaFiles){
        this.mediaFiles = mediaFiles;
    }

    @Override
    @NonNull
    public MediaFilesRecyclerViewAdapter.MediaFileViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        MaterialCardView cardView = (MaterialCardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.media_file_cardview_layout, parent, false);
        cardView.setCheckable(true);
        cardView.setRadius(0.0f);
        return new MediaFileViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(MediaFileViewHolder viewHolder, int position){
        ConstraintLayout constraintLayout = (ConstraintLayout)viewHolder.mediaCardView.getChildAt(1);
        viewHolder.mediaCardView.setPreventCornerOverlap(true);
        int childCount = constraintLayout.getChildCount();
        int count = 0;
        View view;
        int id;
        String text;
        MediaFile mediaFile = mediaFiles.get(position);
        mediaFile.setPosition(position);
        mediaFile.setCardView(viewHolder.mediaCardView);
        viewHolder.mediaCardView.setChecked(mediaFile.isChecked());
        Context context = viewHolder.mediaCardView.getContext();
        if(context instanceof BaseFileBrowserActivity){
            baseFileBrowserActivity = ((BaseFileBrowserActivity)context);
            baseFileBrowserActivity.initialiseThreadPoolExecutor();
        }
        if((mediaFile.isAudio() || mediaFile.isDirectory()) && (mediaFile.getFileIcon() == null)){
            baseFileBrowserActivity.
                    threadPoolExecutor.execute(new MediaFileInformationFetcher(mediaFile, baseFileBrowserActivity.handler, position, true));
        }else if((mediaFile.isVideo() || mediaFile.isImage()) && (mediaFile.getThumbnail() == null)){
            baseFileBrowserActivity.
                    threadPoolExecutor.execute(new MediaFileInformationFetcher(mediaFile, baseFileBrowserActivity.handler, position, true));
        }
        viewHolder.mediaCardView.setOnLongClickListener((e)->{
            if(mediaFile.isDirectory() && (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)){
                return false;
            }
            if(context instanceof BaseFileBrowserActivity){
                if (!multichoiceMode) {
                    mediaFile.toggle();
                    numSelectedDirs = mediaFile.isDirectory() ? 1 : 0;
                    //++numMediaFilesSelected;
                    multichoiceMode = true;
                }else{
                    return true;
                }
                baseFileBrowserActivity.showActionMode();
                if(mediaFile.isDirectory()){
                    MenuItem shareMenuItem = baseFileBrowserActivity.actionMode.getMenu().findItem(shareItemId);
                    shareMenuItem.setEnabled(false);
                    shareMenuItem.setIcon(disabledShareIcon);
                }
                return true;
            }
            return false;
        });

        viewHolder.mediaCardView.setOnClickListener((e)->{
            if(context instanceof BaseFileBrowserActivity) {
                if (multichoiceMode) {
                    mediaFile.toggle();
                    if(mediaFile.isChecked()) {
                        numSelectedDirs += mediaFile.isDirectory() ? 1 : 0;
                        //++numMediaFilesSelected;
                    }else{
                        numSelectedDirs -= mediaFile.isDirectory() ? 1 : 0;
                        //--numMediaFilesSelected;
                    }
                    BaseFileBrowserActivity baseFileBrowserActivity = (BaseFileBrowserActivity)context;
                    MenuItem shareMenuItem = baseFileBrowserActivity.actionMode.getMenu().findItem(shareItemId);
                    if(numSelectedDirs > 0){
                        shareMenuItem.setEnabled(false);
                        shareMenuItem.setIcon(disabledShareIcon);
                    }else{
                        if(!shareMenuItem.isEnabled()){
                            shareMenuItem.setEnabled(true);
                            shareMenuItem.setIcon(enabledShareIcon);
                        }
                    }
                }else {
                    if(context instanceof ResultsActivity) {
                        ((ResultsActivity) context).startOpenFileActivity(mediaFile.getPath());
                    }else if(context instanceof FileBrowserActivity){
                        FileBrowserActivity fbActivity = (FileBrowserActivity)context;
                        if(mediaFile.isDirectory()) {
                            fbActivity.addPreviousFiles(position);
                            fbActivity.setCurrentDirectory(mediaFile.getPath());
                            fbActivity.populateMediaFiles(fbActivity.listFiles());
                            fbActivity.setMediaFilesOnlyCount();
                            fbActivity.recyclerView.scrollToPosition(0);
                        }else if(mediaFile.isAudio() || mediaFile.isVideo()){
                            fbActivity.openFileInEditor(mediaFile.getPath());
                        }
                    }else if(context instanceof MediaStoreFileBrowser){
                        MediaStoreFileBrowser mSActivity = ((MediaStoreFileBrowser)context);
                        if(mediaFile.isDirectory()) {
                            mSActivity.setCurrentDirectory(mediaFile.getPath());
                            mSActivity.populateMediaFiles(null);
                            mSActivity.setScrollPosition(position);
                        }else{
                            mSActivity.openFileInEditor(mediaFile.getPath());
                        }
                    }
                }
            }
        });

        while(count < childCount){
            view = constraintLayout.getChildAt(count);
            id = view.getId();

            if(id == R.id.media_file_duration) {
                if (mediaFile.isDirectory()) {
                    int temp = mediaFile.getFileCount();
                    text = "[".concat(String.valueOf(temp)).concat(" ").concat("File");
                    text += temp == 1 ? "]" : "s]";
                } else if(mediaFile.isImage()){
                    text = "";
                }else{
                    text = Util.toTimeUnits(mediaFile.getFileDuration());
                }
                ((TextView) view).setText(text);
            }else if(id == R.id.media_file_name) {
                ((TextView) view).setText(mediaFile.getFileName() == null ? "" : mediaFile.getFileName());
            }else if(id == R.id.media_file_thumbnail) {
                if (mediaFile.isAudio()) {
                    ((ImageView) view).setImageIcon(mediaFile.getFileIcon());
                } else if (mediaFile.isDirectory()) {
                    ((ImageView) view).setImageIcon(mediaFile.getFileIcon());
                } else if (mediaFile.isVideo()) {
                    ((ImageView) view).setImageBitmap(mediaFile.getThumbnail());
                } else if (mediaFile.isImage()) {
                    ((ImageView) view).setImageBitmap(mediaFile.getThumbnail());
                }
            }else if(id == R.id.media_file_size) {
                TextView textView = (TextView) view;
                if (!mediaFile.isDirectory()) {
                    textView.setText(Util.convertBytes(mediaFile.getFileSize()));
                } else {
                    textView.setText("");
                }
            }else if(id == R.id.media_file_date) {
                TextView textView1 = (TextView) view;
                if (!mediaFile.isDirectory()) {
                    textView1.setText(Util.getFormattedDate(mediaFile.getLastModified()));
                } else {
                    textView1.setText("");
                }
            }
            ++count;
        }
        if(mediaFiles.get(position).isChecked()){
            viewHolder.mediaCardView.setChecked(true);
        }
    }

    @Override
    public int getItemCount(){
        return mediaFiles.size();
    }

    public void setMultichoiceMode(boolean mode){
        multichoiceMode = mode;
    }

    public void setNumSelectedDirs(int numSelectedDirs){
        this.numSelectedDirs = numSelectedDirs;
    }
}
