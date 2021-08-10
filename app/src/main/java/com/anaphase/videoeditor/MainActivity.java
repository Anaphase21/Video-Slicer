package com.anaphase.videoeditor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.anaphase.videoeditor.ui.browser.FileBrowserActivity;
import com.anaphase.videoeditor.ui.browser.MediaStoreFileBrowser;
import com.anaphase.videoeditor.util.SortTypeEnum;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Bundle;

import android.provider.Settings;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

public class MainActivity extends AppCompatActivity {

    private MaterialButton chooseFile;
    private MaterialToolbar topToolbar;

    private MenuItem allFoldersMenuItem;
    private MenuItem fileBrowserMenuItem;
    private MenuItem sortByNameMenuItem;
    private MenuItem sortByDateModifiedMenuItem;
    private MenuItem sortByFileSizeMenuItem;
    private MenuItem sortByDurationMenuItem;

    private BrowserStyle browserStyleEnum;
    private SortTypeEnum sortTypeEnum;

    private String sortByPreferencesKey;
    private String browserStylePreferencesKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        topToolbar = findViewById(R.id.main_activity_top_toolbar);
        sortByPreferencesKey = getString(R.string.sort_by);
        browserStylePreferencesKey = getString(R.string.browser_type);
        inflateTopToolbarMenu();
        chooseFile = findViewById(R.id.choose_file);
        requestStoragePermission();
        chooseFile.setOnClickListener((e)->{
            switch(browserStyleEnum){
                case FOLDERS:
                    startActivity(new Intent(this, MediaStoreFileBrowser.class));
                    break;
                case FILE_BROWSER:
                    startActivity(new Intent(this, FileBrowserActivity.class));
                    break;
            }
        });
    }

    private void requestStoragePermission(){
        if (!(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==PackageManager.PERMISSION_GRANTED)) {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }

    /*private void requestAllFilesAccess(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        startActivity(intent);
    }**/

    private void inflateTopToolbarMenu(){
        Menu menu = topToolbar.getMenu();
        SubMenu browserStyleSubMenu = menu.addSubMenu("Browser style");
        SubMenu sortTypeSubMenu = menu.addSubMenu("Sort files by");
        browserStyleSubMenu.add("Folders");
        browserStyleSubMenu.add("File browser");

        sortTypeSubMenu.add("Name");
        sortTypeSubMenu.add("Date");
        sortTypeSubMenu.add("Size");
        sortTypeSubMenu.add("Duration");

        browserStyleSubMenu.setGroupCheckable(0, true, true);
        sortTypeSubMenu.setGroupCheckable(0, true, true);

        allFoldersMenuItem = browserStyleSubMenu.getItem(0);
        fileBrowserMenuItem = browserStyleSubMenu.getItem(1);

        allFoldersMenuItem.setOnMenuItemClickListener((item->{
            allFoldersMenuItem.setChecked(true);
            browserStyleEnum = BrowserStyle.FOLDERS;
            com.anaphase.videoeditor.util.Settings.save(this, browserStylePreferencesKey, browserStyleEnum.browserStyle);
            return true;
        }));

        fileBrowserMenuItem.setOnMenuItemClickListener((item->{
            fileBrowserMenuItem.setChecked(true);
            browserStyleEnum = BrowserStyle.FILE_BROWSER;
            com.anaphase.videoeditor.util.Settings.save(this, browserStylePreferencesKey, browserStyleEnum.browserStyle);
            return true;
        }));

        sortByNameMenuItem = sortTypeSubMenu.getItem(0);
        sortByDateModifiedMenuItem = sortTypeSubMenu.getItem(1);
        sortByFileSizeMenuItem = sortTypeSubMenu.getItem(2);
        sortByDurationMenuItem = sortTypeSubMenu.getItem(3);

        retrieveSettings();

        sortByNameMenuItem.setOnMenuItemClickListener((item->{
            sortByNameMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_NAME;
            com.anaphase.videoeditor.util.Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));

        sortByDateModifiedMenuItem.setOnMenuItemClickListener((item->{
            sortByDateModifiedMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_DATE;
            com.anaphase.videoeditor.util.Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));

        sortByFileSizeMenuItem.setOnMenuItemClickListener((ite->{
            sortByFileSizeMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_SIZE;
            com.anaphase.videoeditor.util.Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));

        sortByDurationMenuItem.setOnMenuItemClickListener((item->{
            sortByDurationMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_DURATION;
            com.anaphase.videoeditor.util.Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));
    }

    private void retrieveSettings(){
        int sortBy = com.anaphase.videoeditor.util.Settings.retrieve(this, this.sortByPreferencesKey);
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

        int browserStyle = com.anaphase.videoeditor.util.Settings.retrieve(this, browserStylePreferencesKey);
            if(browserStyle == BrowserStyle.FILE_BROWSER.browserStyle){
                browserStyleEnum = BrowserStyle.FILE_BROWSER;
                fileBrowserMenuItem.setChecked(true);
            }else if((browserStyle == -1) || (browserStyle == BrowserStyle.FOLDERS.browserStyle)){
                browserStyleEnum = BrowserStyle.FOLDERS;
                allFoldersMenuItem.setChecked(true);
            }
    }

    public enum BrowserStyle{
        FOLDERS(0),
        FILE_BROWSER(1);

        int browserStyle;

        BrowserStyle(int browserStyle){
            this.browserStyle = browserStyle;
        }
    }
}