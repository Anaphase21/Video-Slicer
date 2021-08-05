package com.anaphase.videoeditor;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.view.MenuItem;
import android.webkit.MimeTypeMap;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.util.ArrayList;

public class ResultsActivity extends BaseFileBrowserActivity {

    ActionBar actionBar;
    MaterialToolbar topToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recyclerViewAdapter = new MediaFilesRecyclerViewAdapter(mediaFiles);
        recyclerView.setAdapter(recyclerViewAdapter);
        topToolbar = (MaterialToolbar)findViewById(R.id.top_toolbar);
        setSupportActionBar(topToolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Select files to share or delete");
        //setActionModeCallback();
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
        ArrayList<String> paths = getIntent().getStringArrayListExtra("paths");
        populateMediaFiles(paths);
    }

    public void startOpenFileActivity(String path){
        if(path != null) {
            Uri uri = FileProvider.getUriForFile(this, "com.anaphase.videoeditor.fileprovider", new File(path));
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            //intent.setData(uri);
            String extension = Util.getExtension(path).toLowerCase();
            //MimeTypeMap.getFileExtensionFromUrl(path);
            intent.setDataAndType(uri, mimeTypeMap.getMimeTypeFromExtension(extension));
            System.out.println("=============="+mimeTypeMap.getMimeTypeFromExtension(extension)+"=============="+extension+"+++Confirm");
            System.out.println(path);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Intent.createChooser(intent, "Open with");
            if(intent.resolveActivity(getPackageManager()) != null){
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        if(menuItem.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}