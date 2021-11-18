package com.anaphase.videoeditor.ui.browser;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import android.os.Bundle;

import android.view.MenuItem;

import com.anaphase.videoeditor.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class ResultsActivity extends BaseFileBrowserActivity {

    ActionBar actionBar;
    MaterialToolbar topToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recyclerViewAdapter = new MediaFilesRecyclerViewAdapter(mediaFiles);
        recyclerView.setAdapter(recyclerViewAdapter);
        topToolbar = findViewById(R.id.top_toolbar);
        setSupportActionBar(topToolbar);
        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Select files to share or delete");
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

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        if(menuItem.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }
}