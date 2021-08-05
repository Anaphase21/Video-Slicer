package com.anaphase.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.util.ArrayList;

public class MediaFilesRecyclerViewAdapter extends RecyclerView.Adapter<MediaFilesRecyclerViewAdapter.MediaFileViewHolder> {

    public ArrayList<MediaFile> mediaFiles;
    private boolean multichoiceMode = false;

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
        Bitmap thumbnail;
        MediaFile mediaFile = mediaFiles.get(position);
        mediaFile.setPosition(position);
        mediaFile.setCardView(viewHolder.mediaCardView);
        viewHolder.mediaCardView.setChecked(mediaFile.isChecked());
//        mediaFile.setCheckedState(viewHolder.mediaCardView.isChecked());
        Context context = viewHolder.mediaCardView.getContext();

        viewHolder.mediaCardView.setOnLongClickListener((e)->{
            //if((context instanceof ResultsActivity) || (context instanceof FileBrowserActivity)) {
            if(context instanceof BaseFileBrowserActivity){
                if (!multichoiceMode) {
                    mediaFile.toggle();
                    multichoiceMode = true;
                }else{
                    return true;
                }
                //((ResultsActivity)context).showActionMode();
                ((BaseFileBrowserActivity)context).showActionMode();
                return true;
            }
            return false;
        });

        viewHolder.mediaCardView.setOnClickListener((e)->{
            if(context instanceof BaseFileBrowserActivity) {
                if (multichoiceMode) {
                    mediaFile.toggle();
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
                            //System.out.println("================File Clicked==================");
                            //notifyDataSetChanged();
                        }else if(mediaFile.isAudioTrack() || mediaFile.isVideo()){
                            fbActivity.openFileInEditor(mediaFile.getPath());
                        }
                        fbActivity = null;
                    }else if(context instanceof MediaStoreFileBrowser){
                        MediaStoreFileBrowser mSActivity = ((MediaStoreFileBrowser)context);
                        if(mediaFile.isDirectory()) {
                            mSActivity.setCurrentDirectory(mediaFile.getPath());
                            //System.out.println(mediaFile.getFileName());
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
            switch (id){
                case R.id.media_file_duration:
                    if(mediaFile.isDirectory()){
                        int temp = mediaFile.getFileCount();
                        text = "[".concat(String.valueOf(temp)).concat(" ").concat("File");
                        text += temp == 1 ? "]" : "s]";
                    }else {
                        text = Util.toTimeUnits(mediaFile.getFileDuration());
                    }
                    ((TextView)view).setText(text);
                    break;
                case R.id.media_file_name:
                    ((TextView)view).setText(mediaFile.getFileName());
                    break;
                case R.id.media_file_thumbnail:
                    thumbnail = mediaFile.getThumbnail();
                    if(mediaFile.isAudioTrack()){
                        ((ImageView)view).setImageIcon(mediaFile.getFileIcon());
                    }else if((new File(mediaFile.getPath())).isDirectory()){
                        ((ImageView)view).setImageIcon(mediaFile.getFileIcon());
                    }else if(mediaFile.isVideo()){
                        ((ImageView)view).setImageBitmap(thumbnail);
                    }else if(mediaFile.isImage()){
                        ((ImageView)view).setImageBitmap(thumbnail);
                    }
                    break;
                case R.id.media_file_size:
                    TextView textView = (TextView)view;
                    if(!mediaFile.isDirectory()) {
                       textView.setText(Util.convertBytes(mediaFile.getFileSize()));
                    }else{
                        if((textView.getText() != null) || (!textView.getText().toString().isEmpty())){
                            textView.setText("");
                        }
                    }
                    break;
                case R.id.media_file_date:
                    TextView textView1 = (TextView)view;
                    if(!mediaFile.isDirectory()) {
                        textView1.setText(Util.getFormattedDate(mediaFile.getLastModified()));
                    }else{
                        if((textView1.getText() != null) || (!textView1.getText().toString().isEmpty())){
                            textView1.setText("");
                        }
                    }
                    break;
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

}
