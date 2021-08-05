package com.anaphase.videoeditor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

import java.io.File;

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
        topToolbar = (MaterialToolbar)findViewById(R.id.main_activity_top_toolbar);
        sortByPreferencesKey = getString(R.string.sort_by);
        browserStylePreferencesKey = getString(R.string.browser_type);
        inflateTopToolbarMenu();
        chooseFile = (MaterialButton)findViewById(R.id.choose_file);
        //browserStyleEnum = BrowserStyle.FOLDERS;
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
        requestPermission();
        File[] files = getExternalMediaDirs();
        for(File file : files) {
            System.out.println("EXTERNAL DIRECTORY>>" + (file == null ? "Null" : file.getPath()));
        }
        //requestAllFilesAccess();
        //FFprobe.execute("\"/storage/emulated/0/Download/Mulan/Ambitions.S01E08.WEBRip.x264-TBS[ettv]/Merlin.S05E13.The.Diamond.of.the.Day.Part.2.720p.BluRay.x264-Pahe.in6.mkv\"");
/*        StorageManager storageManager = (StorageManager)getSystemService(STORAGE_SERVICE);
        List<StorageVolume> volumeList = storageManager.getStorageVolumes();
        for(StorageVolume volume : volumeList) {
            Log.d("volume", volume.toString()+"    "+volume.getUuid());
        }*/
        //initialiseToolbarMenu();
    }

    private void requestPermission(){
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            System.out.println("PERMISSION GRANTED");
            // You can use the API that requires the permission.
            //} else if (shouldShowRequestPermissionRationale(...)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            //showInContextUI(...);
        } else {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, 1);
            System.out.println("Permission not granted");
        }
    }

    private void requestAllFilesAccess(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        startActivity(intent);
    }

    private void inflateTopToolbarMenu(){
        //topToolbar.inflateMenu(R.menu.top_toolbar_menu);
        Menu menu = topToolbar.getMenu();
        //settingsItem = menu.getItem(0);
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
        //allFoldersMenuItem.setChecked(true);

        allFoldersMenuItem.setOnMenuItemClickListener((item->{
            allFoldersMenuItem.setChecked(true);
            browserStyleEnum = BrowserStyle.FOLDERS;
            com.anaphase.videoeditor.Settings.save(this, browserStylePreferencesKey, browserStyleEnum.browserStyle);
            return true;
        }));

        fileBrowserMenuItem.setOnMenuItemClickListener((item->{
            fileBrowserMenuItem.setChecked(true);
            browserStyleEnum = BrowserStyle.FILE_BROWSER;
            com.anaphase.videoeditor.Settings.save(this, browserStylePreferencesKey, browserStyleEnum.browserStyle);
            return true;
        }));

        sortByNameMenuItem = sortTypeSubMenu.getItem(0);
        sortByDateModifiedMenuItem = sortTypeSubMenu.getItem(1);
        sortByFileSizeMenuItem = sortTypeSubMenu.getItem(2);
        sortByDurationMenuItem = sortTypeSubMenu.getItem(3);
        //sortByNameMenuItem.setChecked(true);

        retrieveSettings();

        sortByNameMenuItem.setOnMenuItemClickListener((item->{
            sortByNameMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_NAME;
            com.anaphase.videoeditor.Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));

        sortByDateModifiedMenuItem.setOnMenuItemClickListener((item->{
            sortByDateModifiedMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_DATE;
            com.anaphase.videoeditor.Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));

        sortByFileSizeMenuItem.setOnMenuItemClickListener((ite->{
            sortByFileSizeMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_SIZE;
            com.anaphase.videoeditor.Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));

        sortByDurationMenuItem.setOnMenuItemClickListener((item->{
            sortByDurationMenuItem.setChecked(true);
            sortTypeEnum = SortTypeEnum.BY_DURATION;
            com.anaphase.videoeditor.Settings.save(this, sortByPreferencesKey, sortTypeEnum.sortType);
            return true;
        }));
    }

    private void retrieveSettings(){
        int sortBy = com.anaphase.videoeditor.Settings.retrieve(this, this.sortByPreferencesKey);
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

        int browserStyle = com.anaphase.videoeditor.Settings.retrieve(this, browserStylePreferencesKey);
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