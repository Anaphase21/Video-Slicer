package com.anaphase.videoeditor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.DialogFragment;

import com.anaphase.videoeditor.ui.AlertDialogBox;
import com.anaphase.videoeditor.ui.browser.FileBrowserActivity;
import com.anaphase.videoeditor.ui.browser.MediaStoreFileBrowser;
import com.anaphase.videoeditor.util.SortTypeEnum;
import com.anaphase.videoeditor.util.Util;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import android.Manifest;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

public class MainActivity extends AppCompatActivity implements AlertDialogBox.AlertDialogListener {

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
    private String permissionRationalMessage;

    private final int PERMISSION_REQUEST_CODE = 256;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        topToolbar = findViewById(R.id.main_activity_top_toolbar);
        topToolbar.setTitle("Video Slicer");
        sortByPreferencesKey = getString(R.string.sort_by);
        browserStylePreferencesKey = getString(R.string.browser_type);
        permissionRationalMessage = getString(R.string.read_media_permission_rational_message);
        inflateTopToolbarMenu();
        MaterialButton chooseFile = findViewById(R.id.choose_file);
        chooseFile.setOnClickListener((e)->
            requestStoragePermission()
        );
    }

    private void requestPermission(){
        String readPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        String writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String[] permissions;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q){
            permissions = new String[]{readPermission};
        }else{
            permissions = new String[]{readPermission, writePermission};
        }
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private void openFileBrowser(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q){
            startActivity(new Intent(this, MediaStoreFileBrowser.class));
            return;
        }
        switch(browserStyleEnum){
            case FOLDERS:
                startActivity(new Intent(this, MediaStoreFileBrowser.class));
                break;
            case FILE_BROWSER:
                startActivity(new Intent(this, FileBrowserActivity.class));
                break;
        }
    }

    private void requestStoragePermission(){
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            openFileBrowser();
            Util.createAppDirectory();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            //showInContextUI(...);
            Bundle bundle = new Bundle();
            bundle.putString("title", "Permission required.");
            bundle.putString("message", permissionRationalMessage);
            bundle.putString("positiveButton", "Continue");
            bundle.putString("negativeButton", "Cancel");
            AlertDialogBox permissionDialog = new AlertDialogBox();
            permissionDialog.setArguments(bundle);
            permissionDialog.show(getFragmentManager(), "");
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                openFileBrowser();
                Util.createAppDirectory();
            }
        }
    }

    /*private void requestAllFilesAccess(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        startActivity(intent);
    }**/

    private void inflateTopToolbarMenu(){
        Menu menu = topToolbar.getMenu();
        SubMenu browserStyleSubMenu;
        //Only show browser type submenu for Android 10 and below
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
            browserStyleSubMenu = menu.addSubMenu("Browser style");
            browserStyleSubMenu.add("Folders");
            browserStyleSubMenu.add("File browser");
            browserStyleSubMenu.setGroupCheckable(0, true, true);
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
        }

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

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            int browserStyle = com.anaphase.videoeditor.util.Settings.retrieve(this, browserStylePreferencesKey);
            if (browserStyle == BrowserStyle.FILE_BROWSER.browserStyle) {
                browserStyleEnum = BrowserStyle.FILE_BROWSER;
                fileBrowserMenuItem.setChecked(true);
            } else if ((browserStyle == -1) || (browserStyle == BrowserStyle.FOLDERS.browserStyle)) {
                browserStyleEnum = BrowserStyle.FOLDERS;
                allFoldersMenuItem.setChecked(true);
            }
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

    @Override
    public void onDialogPositiveClick(DialogFragment dialogFragment){
        requestPermission();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialogFragment){

    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialogFragment){

    }

    @Override
    public void onCancel(DialogInterface dialogInterface){

    }
}