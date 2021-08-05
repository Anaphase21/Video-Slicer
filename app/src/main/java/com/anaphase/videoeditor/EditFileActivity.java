package com.anaphase.videoeditor;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.snackbar.Snackbar;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.util.ArrayList;

import static com.anaphase.videoeditor.EditActionType.CUT;
import static com.anaphase.videoeditor.EditActionType.GRAB_MP3;
import static com.anaphase.videoeditor.EditActionType.GRAB_THUMBNAILS;

public class EditFileActivity extends AppCompatActivity implements AlertDialogBox.AlertDialogListener {

    private MaterialToolbar bottomToolbar;

    private MenuItem cutItem;
    private MenuItem undoItem;
    private MenuItem settingsItem;
    private MenuItem slowCut;
    private MenuItem fastCut;

    private static Handler1 handler;
    private Bundle bundle;
    private VideoView videoView;
    private Timeline timeline;
    private NumberPicker numberPicker;
    private ImageButton confirmIntervalSelection;
    private MaterialCheckBox constantIntervalCheckBox;
    private RadioGroup editTypeRadioButtons;
    private MaterialRadioButton cutFile, grabMp3, grabThumbnail;
    private MaterialButton startTaskButton;
    private AppBarLayout bottomAppBarLayout;
    private MaterialToolbar topToolbar;
    private ActionBar actionBar;
    private LinearLayout selectPartLayout;
    private HorizontalScrollView selectPartScrollLayout;
    private ConstraintLayout editActivityLayout;
    private LinearLayout taskProgressViewLayout;
    private TextView taskProgressView;

    private MediaScanner mediaScanner;

    private Thread timelineTimerThread;
    private TimelineTimer timer;
    private EditActions editActions;
    private View[] views;
    private EditActionType actionType;
    private MediaPlayerState playerState = MediaPlayerState.STOPPED;
    private StartTaskButtonState buttonState = StartTaskButtonState.SELECT;

    private final int undoEnabledIconResId = R.drawable.ic_undo_24px;
    private final int undoDisabledIconResId = R.drawable.ic_undo_disabled_24px;
    private final int cutEnabledIconResId = R.drawable.ic_content_cut_24px;
    private final int cutDisabledIconResId = R.drawable.ic_content_cut_disabled_24px;
    private final int selectPartShape = R.drawable.select_part_shape;
    private final int unselectPartShape = R.drawable.unselect_part_shape;

    private Icon confirmIntervalSelectionEnabledIcon;
    private Icon confirmIntervalSelectionDisabledIcon;

    private float scale;
    private final float STUB_X_POSITION_DP = 66.66667f;
    private final float STUB_Y_POSITION_DP = 6.66667f;
    private float startButtonInitialYPosition;
    private int stubXPosition;
    private int stubYPosition;
    private int currentPosition = 0;

    ActionMode.Callback cancelActionModeCallback;
    ActionMode cancelActionMode;

    private final int SLOW = 0;
    private final int FAST = 1;
    private int cutSpeed = SLOW;

    private Codec videoCodec;
    private Codec audioCodec;

    ArrayList<String> paths;
    private String path;

    private long lastActionTime = 0L;
    private float lastActionXLocation = 0;

    private final float PLAY_BUTTON_AREA_DP = 30.0F;
    private float PLAY_BUTTON_AREA_PX;

    private String cutSpeedPreferencesKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState != null){
            //Restore video position to the last value before the activity
            //lost focus and eventually got destroyed.
            currentPosition = savedInstanceState.getInt("currentPosition");
            cutSpeed = savedInstanceState.getInt("cutSpeed");
            path = savedInstanceState.getString("path");
        }
        super.onCreate(savedInstanceState);
        editActivityLayout = (ConstraintLayout)getLayoutInflater().inflate(R.layout.activity_edit_file, null);
        setContentView(editActivityLayout);
        scale = getResources().getDisplayMetrics().density;
        stubXPosition = (int)(scale * STUB_X_POSITION_DP);
        stubYPosition = (int)(scale * STUB_Y_POSITION_DP);
        PLAY_BUTTON_AREA_PX = scale * PLAY_BUTTON_AREA_DP;
        timeline = (Timeline)findViewById(R.id.timeline);
        bottomAppBarLayout = findViewById(R.id.bottom_toolbar_layout);
        topToolbar = (MaterialToolbar)findViewById(R.id.top_toolbar);
        cutSpeedPreferencesKey = getString(R.string.cut_speed);
        inflateTopToolbarMenu();
        //setSupportActionBar(topToolbar);
        //actionBar = getSupportActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        inflateBottomToolbarMenu();
        selectPartScrollLayout = (HorizontalScrollView)getLayoutInflater().inflate(R.layout.select_part_layout, null);
        videoView = (VideoView)findViewById(R.id.video_canvas);
        numberPicker = (NumberPicker)findViewById(R.id.constant_interval_picker);
        constantIntervalCheckBox = (MaterialCheckBox)findViewById(R.id.constant_interval_checkbox);
        editTypeRadioButtons = findViewById(R.id.edit_type_radio_buttons);
        cutFile = (MaterialRadioButton)findViewById(R.id.cut_file);
        grabMp3 = (MaterialRadioButton)findViewById(R.id.grab_mp3);
        confirmIntervalSelection = (ImageButton)findViewById(R.id.confirm_interval_selection);
        grabThumbnail = (MaterialRadioButton)findViewById(R.id.grab_thumbnail);
        views = new View[]{numberPicker, constantIntervalCheckBox, cutFile, grabMp3, grabThumbnail};
        startTaskButton = (MaterialButton)findViewById(R.id.start_task);
        taskProgressViewLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.task_progress_layout, null);
        initialiseHandler();
        confirmIntervalSelectionEnabledIcon = Icon.createWithResource(this, R.drawable.ic_content_cut_24px);
        confirmIntervalSelectionDisabledIcon = Icon.createWithResource(this, R.drawable.ic_content_cut_disabled_24px);
        setCancelActionModeCallback();
        //mediaScanner = new MediaScanner(this);
    }

    private void initialiseHandler(){
        handler = new Handler1(Looper.getMainLooper());
    }

    private void initialiseVideo(String path){

        videoView.setOnCompletionListener((l)->{
            if(timer == null){
                return;
            }
            timer.setCompleted(true);
            videoView.seekTo(2);
            //timeline.setSlider_x(0.0f);
            currentPosition = 2;
            //timeline.invalidate();
            //videoView.stopPlayback();
            timelineTimerThread = new Thread(timer);
            setPlayerState(MediaPlayerState.RESTARTED);
        });

        videoView.setOnPreparedListener((l)->{
            timer = new TimelineTimer(handler, videoView);
            timelineTimerThread = new Thread(timer);
            if(currentPosition == 0) {
                videoView.seekTo(2);
            }else{
                videoView.seekTo(currentPosition);
            }
            timer.setDuration(videoView.getDuration());
            timeline.setVideoView(videoView);
        });
        videoView.setVideoURI(Uri.fromFile(new File(path)));
        System.out.println("CONTENT URI>>>"+Uri.fromFile(new File(path)));
        //videoView.setVideoPath(path);
        videoView.setOnTouchListener((view, motionEvent)->{
            if(motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN){
                if((motionEvent.getX() > (videoView.getWidth() / 2) - PLAY_BUTTON_AREA_PX) && (motionEvent.getX() < (videoView.getWidth() / 2) + PLAY_BUTTON_AREA_PX)) {
                    switch (playerState) {
                        case STOPPED:
                            videoView.start();
                            timelineTimerThread.start();
                            setPlayerState(MediaPlayerState.PLAYING);
                            break;
                        case PAUSED:
                            videoView.start();
                            timer.setThreadToSleep(false);
                            setPlayerState(MediaPlayerState.PLAYING);
                            break;
                        case PLAYING:
                            if (videoView.canPause()) {
                                videoView.pause();
                            }
                            timer.setThreadToSleep(true);
                            setPlayerState(MediaPlayerState.PAUSED);
                            break;
                        case RESTARTED:
                            videoView.start();
                            setPlayerState(MediaPlayerState.PLAYING);
                            timer.setCompleted(false);
                            timelineTimerThread.start();
                    }
                    return true;
                }
                long currentTime = System.currentTimeMillis();
                if((currentTime - lastActionTime) <= 300L){
                    if((motionEvent.getX() < (videoView.getWidth() / 2) - PLAY_BUTTON_AREA_PX) && (lastActionXLocation < (videoView.getWidth() / 2) - PLAY_BUTTON_AREA_PX)){
                        //seek() backward
                        int currentPlaybackPosition = videoView.getCurrentPosition();
                        int videoDuration = videoView.getDuration();
                        if(currentPlaybackPosition >= 5000) {
                            videoView.seekTo(currentPlaybackPosition - 5000);
                            timeline.setSlider_x(((currentPlaybackPosition - 5000) * 100.0f) / videoDuration);
                        }else{
                            videoView.seekTo(0);
                            timeline.setSlider_x(0.0f);
                        }
                        //timeline.invalidate();
                        return true;
                    }else if((motionEvent.getX() > (videoView.getWidth() / 2) + PLAY_BUTTON_AREA_PX) && (lastActionXLocation > (videoView.getWidth() / 2) + PLAY_BUTTON_AREA_PX)){
                        //seek() forward
                        int currentPlaybackPosition = videoView.getCurrentPosition();
                        int videoDuration = videoView.getDuration();
                        if((videoDuration - currentPlaybackPosition) <= 5000){
                            videoView.seekTo(videoDuration);
                            timeline.setSlider_x((videoDuration * 100.0f) / videoDuration);
                        }else{
                            videoView.seekTo(currentPlaybackPosition + 5000);
                            timeline.setSlider_x(((currentPlaybackPosition + 5000) * 100.0f) / videoDuration);
                        }
                        //timeline.invalidate();
                        return true;
                    }
                }
                lastActionTime = currentTime;
                lastActionXLocation = motionEvent.getX();
            }
            return true;
        });

        videoView.setOnErrorListener((mp, what, extra)->{
            System.out.println("AN ERROR OCCURED. CAUSE: MISSING FILE: "+what+", "+extra);
            if((what == MediaPlayer.MEDIA_ERROR_UNKNOWN) && (!(new File(path)).exists())){
                showErrorDialog("File Error!", "The current file no longer exist. This window will close.");
            }else{
                showErrorDialog("Unknown Error!", "An unknown Error has occured. File is probably corrupted or invalid.");
            }
            return true;
        });
    }

    private void inflateTopToolbarMenu(){
        //topToolbar.inflateMenu(R.menu.top_toolbar_menu);
        Menu menu = topToolbar.getMenu();
        //settingsItem = menu.getItem(0);
        SubMenu subMenu = menu.addSubMenu("Cut Speed");
        subMenu.add("Slow");
        subMenu.add("Fast");
        subMenu.setGroupCheckable(0, true, true);
        slowCut = subMenu.getItem(0);
        fastCut = subMenu.getItem(1);
        retrieveSettings();
        //slowCut.setChecked(true);
        slowCut.setOnMenuItemClickListener((item)->{
            slowCut.setChecked(true);
            cutSpeed = SLOW;
            Settings.save(this, cutSpeedPreferencesKey, SLOW);
            return true;
        });
        fastCut.setOnMenuItemClickListener((item)->{
            fastCut.setChecked(true);
            cutSpeed = FAST;
            Settings.save(this, cutSpeedPreferencesKey, FAST);
            return true;
        });
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            slowCut.setTooltipText("Slow but accurate cuts");
            fastCut.setTooltipText("Fast but inaccurate cuts");
        }
    }

    private void retrieveSettings(){
        int cutSpeed = Settings.retrieve(this, cutSpeedPreferencesKey);
        switch(cutSpeed){
            case SLOW:
            case -1:
                this.cutSpeed = SLOW;
                slowCut.setChecked(true);
                break;
            case FAST:
                this.cutSpeed = FAST;
                fastCut.setChecked(true);
                break;
        }
    }

    private void inflateBottomToolbarMenu(){
        bottomToolbar = (MaterialToolbar)findViewById(R.id.bottom_toolbar);
        bottomToolbar.inflateMenu(R.menu.bottom_toolbar_menu);
        Menu menu = bottomToolbar.getMenu();
        cutItem = menu.getItem(2);
        undoItem = menu.getItem(0);
        undoItem.setEnabled(false);
        //undoItem.setEnabled(false);
        MenuItem stub = menu.getItem(1);
        View stubActionView = stub.getActionView();
        stubActionView.setPadding(stubXPosition, stubActionView.getPaddingTop(), stubYPosition, stubActionView.getPaddingBottom());
        bottomToolbar.setOnMenuItemClickListener((item)->{
            int itemId = item.getItemId();
            final int cutItemId = R.id.cut_item;
            final int undoItemId = R.id.undo_item;
            switch (itemId) {
                case cutItemId:
                    if((timeline.cutPoints.size() > 0) && (timeline.getSlider_x() == timeline.cutPoints.peek())){
                        return true;
                    }
                    if(constantIntervalCheckBox.isChecked()){
                        if(timeline.cutPoints.size() == 0){
                            timeline.setConstantIntervalStartPoint(timeline.getSlider_x());
                            timeline.cutPoints.push(timeline.getSlider_x());
                            undoItem.setEnabled(true);
                            undoItem.setIcon(undoEnabledIconResId);
                            timeline.invalidate();
                            return true;
                        }
                        if(timeline.cutPoints.size() == 1) {
                            timeline.setConstantIntervalEndPoint(timeline.getSlider_x());
                            timeline.cutPoints.push(timeline.getSlider_x());
                            timeline.invalidate();
                            cutItem.setEnabled(false);
                            cutItem.setIcon(cutDisabledIconResId);
                            return true;
                        }
                    }
                    timeline.cutPoints.push(timeline.getSlider_x());
                    if (!undoItem.isEnabled()) {
                        undoItem.setEnabled(true);
                        undoItem.setIcon(undoEnabledIconResId);
                    }
                    timeline.invalidate();
                    break;
                case undoItemId:
                    if(timeline.cutPoints.size() > 0){
                        if(constantIntervalCheckBox.isChecked()){
                            timeline.cutPoints.pop();
                            timeline.invalidate();
                            cutItem.setEnabled(true);
                            cutItem.setIcon(cutEnabledIconResId);
                            if(timeline.cutPoints.size() == 0){
                                undoItem.setEnabled(false);
                                undoItem.setIcon(undoDisabledIconResId);
                            }
                            return true;
                        }
                        timeline.cutPoints.pop();
                        timeline.invalidate();
                        if(timeline.cutPoints.size() == 0){
                            undoItem.setIcon(undoDisabledIconResId);
                            undoItem.setEnabled(false);
                        }
                    }
            }
            return true;
        });
    }

    public void setPlayerState(MediaPlayerState playerState){
        this.playerState = playerState;
    }

    private void release(){
        if((timelineTimerThread != null) && (timelineTimerThread.isAlive())){
            try{
                timer.setCompleted(true);
                timelineTimerThread.interrupt();
            }catch(SecurityException se){
            }
        }
        videoView.stopPlayback();
    }

    @Override
    //Called when Activity loses focus right after onPause() is called
    protected void onStop(){
        //System.out.println("On Stop");
        //currentPosition = videoView.getCurrentPosition();
        release();
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        //System.out.println("On Destroy");
        release();
        super.onDestroy();
    }

    @Override
    //Called when Activity goes to background right after onStop() is called.
    //Activity may be restarted (onRestart()->onStart()->onResume()) or destroyed and
    //restarted again (onDestroyed()->onCreate()->onStart()->onResume()).
    protected void onSaveInstanceState(Bundle savedInstanceState){
        //System.out.println("On Save Instance: "+currentPosition+" "+videoView.getCurrentPosition());
        //Save current video position before this Activity goes to background
        //and possibly get destroyed.
        System.out.println("ABOUT TO BE DESTROYED");
        savedInstanceState.putInt("currentPosition", currentPosition);
        savedInstanceState.putInt("cutSpeed", cutSpeed);
        savedInstanceState.putString("path", path);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle){
        super.onRestoreInstanceState(bundle);
    }

    @Override
    protected void onPause(){
        //System.out.println("On Pause");
        super.onPause();
    }

    @Override
    protected void onStart(){
//        System.out.println("Current Position: "+currentPosition);
        String fileName = "";
        super.onStart();
        Intent intent = getIntent();
        String path = "";
        if(intent.getAction() == Intent.ACTION_EDIT){
            path = intent.getDataString();
            Uri uri = Uri.parse(path);
            String[] mediaColumns = new String[]{MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATA};
            Cursor cursor = getApplicationContext().getContentResolver().query(uri, mediaColumns, null, null, null);
            cursor.moveToNext();
            fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
            this.path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
            path = this.path;
            cursor.close();
        }else {
            path = (String) intent.getCharSequenceExtra("path");
            this.path = path;
            String decoded = Uri.decode(path);
            fileName = decoded.substring(decoded.lastIndexOf('/') + 1, decoded.lastIndexOf('.'));
        }
        if(path == null){
            path = this.path;
        }
        //actionBar.setDisplayHomeAsUpEnabled(false);
        //topToolbar.setTitle(path.substring(path.lastIndexOf('/')+1, path.lastIndexOf('.')));

        System.out.println("=========="+path);
        //actionBar.setTitle(decoded.substring(decoded.lastIndexOf('/')+1, decoded.lastIndexOf('.')));
        topToolbar.setTitle(fileName);
        //topToolbar.setTitle();
        setPlayerState(MediaPlayerState.STOPPED);
        initialiseVideo(path);
        initialiseEditorUICompnents();
    }

    public void setCurrentPosition(int currentPosition){
        this.currentPosition = currentPosition;
    }

    private void initialiseEditorUICompnents(){
        numberPicker.setMaxValue(60);
        numberPicker.setMinValue(1);
        numberPicker.setFormatter((i)->{
            return numberPicker.getValue() == i ? "[ " + i + " ]" : String.valueOf(i);
        });

        numberPicker.setOnValueChangedListener((picker, oldValue, newValue)->{
        });

        if(!constantIntervalCheckBox.isChecked()){
            numberPicker.setEnabled(false);
        }

        if(!constantIntervalCheckBox.isChecked()) {
            confirmIntervalSelection.setEnabled(false);
        }
        confirmIntervalSelection.setOnClickListener((event)->{
            timeline.setRegularCutPoints(numberPicker.getValue() * 1000);
        });

        constantIntervalCheckBox.setOnCheckedChangeListener((buttonView, isChecked)->{
            if(isChecked){
                timeline.clearCutPoints();
                timeline.invalidate();
                undoItem.setEnabled(false);
                undoItem.setIcon(undoDisabledIconResId);
                confirmIntervalSelection.setEnabled(true);
                confirmIntervalSelection.setImageIcon(confirmIntervalSelectionEnabledIcon);
                numberPicker.setEnabled(true);
            }else{
                timeline.clearCutPoints();
                timeline.invalidate();
                cutItem.setEnabled(true);
                cutItem.setIcon(cutEnabledIconResId);
                confirmIntervalSelection.setEnabled(false);
                confirmIntervalSelection.setImageIcon(confirmIntervalSelectionDisabledIcon);
                numberPicker.setEnabled(false);
            }
        });

        cutFile.setChecked(true);
        actionType = CUT;
        cutFile.setOnCheckedChangeListener((buttonView, isChecked)->{
            if(isChecked){
                actionType = CUT;
                startTaskButton.setEnabled(true);
                setButtonState(StartTaskButtonState.SELECT);
            }
        });

        if(Util.isAudioExtension(path)){
            grabMp3.setEnabled(false);
        }
        grabMp3.setOnCheckedChangeListener((buttonView, isChecked)->{
            if(isChecked){
                actionType = EditActionType.GRAB_MP3;
                startTaskButton.setEnabled(true);
                setButtonState(StartTaskButtonState.SELECT);
            }
        });

        if(Util.isAudioExtension(path)){
            grabThumbnail.setEnabled(false);
        }
        grabThumbnail.setOnCheckedChangeListener((buttonView, isChecked)->{
            if(isChecked){
                actionType = EditActionType.GRAB_THUMBNAILS;
                if(editActivityLayout.indexOfChild(selectPartScrollLayout) != -1){
                    editActivityLayout.removeView(selectPartScrollLayout);
                    restoreBottomToolbar();
                }
                startTaskButton.setEnabled(true);
                setButtonState(StartTaskButtonState.START);
            }
        });

        editActions = new EditActions(handler);
        startTaskButton.setOnClickListener((event)->{
            switch(actionType){
                case GRAB_MP3:
                case CUT:
                    switch (buttonState){
                        case START:
                            setButtonState(StartTaskButtonState.CANCEL);
                            removeCancelContextualMenu();
                            editActivityLayout.removeView(selectPartScrollLayout);
                            showTaskProgressView();
                            if(actionType == CUT){
                                if(cutSpeed == FAST){
                                    videoCodec = Codec.COPY;
                                    audioCodec = Codec.COPY;
                                }else{
                                    if(path.toLowerCase().endsWith(".webm")){
                                        videoCodec = Codec.VP8;
                                        audioCodec = Codec.VORBIS;
                                    }else{
                                        videoCodec = Codec.H264;
                                        audioCodec = Codec.AAC;
                                    }
                                }
                                editActions.cut(getSelectedParts(), videoCodec, audioCodec, path, false);
                            }else if(actionType == GRAB_MP3){
                                editActions.cut(getSelectedParts(), null, Codec.MP3, path,  true);
                            }
                            //restoreBottomToolbar();
                            break;
                        case CANCEL:
                            FFmpeg.cancel();
                            removeTaskProgressView();
                            restoreBottomToolbar();
                            if(actionType == GRAB_THUMBNAILS){
                                setButtonState(StartTaskButtonState.START);
                            }else {
                                setButtonState(StartTaskButtonState.SELECT);
                            }
                            break;
                        case SELECT:
                            showCancelContextualMenu();
                            disableViews();
                            if(startButtonInitialYPosition == 0.0f){
                                startButtonInitialYPosition = startTaskButton.getY();
                            }
                            if(timeline.getPaddedCutPoints().size() > 0) {
                                pushDownBottomToolbar();
                                setButtonState(StartTaskButtonState.START);
                            }
                            break;

                    }
                    //pushDownBottomToolbar();
                    break;
                case GRAB_THUMBNAILS:
                    switch (buttonState){
                        case START:
                            setButtonState(StartTaskButtonState.CANCEL);
                            //removeCancelContextualMenu();
                            if(startButtonInitialYPosition == 0.0f){
                                startButtonInitialYPosition = startTaskButton.getY();
                            }
                            pushDownBottomToolbar();
                            disableViews();
                            //editActivityLayout.removeView(selectPartScrollLayout);
                            showTaskProgressView();
                            if(timeline.getCutPoints().size() > 0) {
                                editActions.grabThumbnails(timeline.getCutPoints(), Codec.MJPEG, path, "1");
                            }
                            break;
                        case CANCEL:
                            setButtonState(StartTaskButtonState.START);
                            break;
                    }
                    break;
            }
        });
    }

    private void setButtonState(StartTaskButtonState buttonState){
        this.buttonState = buttonState;
        startTaskButton.setText(buttonState.label);
    }

    private void addSelectPartButtonLayout(){
        if(editActivityLayout.indexOfChild(selectPartScrollLayout) == -1) {
            editActivityLayout.addView(selectPartScrollLayout);
        }
        ConstraintLayout.LayoutParams constraintLayoutParams = (ConstraintLayout.LayoutParams)selectPartScrollLayout.getLayoutParams();
        constraintLayoutParams.topToBottom = R.id.edit_type_radio_buttons;
        constraintLayoutParams.leftToLeft = editActivityLayout.getId();
        selectPartLayout = (LinearLayout) selectPartScrollLayout.getChildAt(0);
        LinearLayout.LayoutParams selectPartLayoutParams;
        if(selectPartLayout.getChildCount() > 0){
            selectPartLayout.removeAllViews();
        }
        SelectPartButton selectPartButton;
        ArrayList<Integer> cutPoints = timeline.getPaddedCutPoints();
        int numParts = cutPoints.size() - 1;
        for(int i = 0; i < numParts; ++i) {
            selectPartButton = new SelectPartButton(this);
            Pair<Integer, Integer> cutPointsPair = Pair.create(cutPoints.get(i), cutPoints.get(i + 1));
            selectPartButton.setCutPoints(cutPointsPair);
            selectPartButton.setId(i);
            setSelectPartButtonListeners(selectPartButton);
            selectPartButton.setChecked(true);
            selectPartButton.setGravity(Gravity.CENTER);
            selectPartButton.setText(String.valueOf(i+1));
            selectPartLayout.addView(selectPartButton);
            selectPartLayoutParams = (LinearLayout.LayoutParams)selectPartButton.getLayoutParams();
            selectPartLayoutParams.leftMargin = (int)(13 * scale);
            ViewGroup.LayoutParams selectPartButtonLayoutParams = selectPartButton.getLayoutParams();
            selectPartButtonLayoutParams.height = ((int) (scale * 30));
            selectPartButtonLayoutParams.width = ((int) (scale * 30));
        }
    }

    private void setSelectPartButtonListeners(SelectPartButton selectPartButton){
        selectPartButton.setOnClickListener((view)->{
            SelectPartButton selectPartButton1 = (SelectPartButton)view;
            selectPartButton1.setChecked(selectPartButton1.isChecked());
        });

        selectPartButton.setOnCheckedChangeListener((buttonView, isChecked)->{
            if(isChecked){
                buttonView.setBackgroundResource(selectPartShape);
            }else{
                buttonView.setBackgroundResource(unselectPartShape);
            }
        });
    }

    private void pushDownBottomToolbar(){
        Thread th = new Thread(()->{
            float y1_pos = startTaskButton.getY();
            float y2_pos = bottomAppBarLayout.getY();
            Bundle bundle = new Bundle();
            float h = scale * 50;
            float inc = 5;
            if(y1_pos > startButtonInitialYPosition){
                return;
            }
            while(inc < h ) {
                y1_pos += 5;
                y2_pos += 5;
                inc += 5;
                bundle.putFloatArray("dragPos", new float[]{y1_pos, y2_pos});
                Message message = handler.obtainMessage();
                message.setData(bundle);
                handler.sendMessage(message);
                try {
                    Thread.sleep(20);
                }catch(InterruptedException interruptedException){}
            }
            Message message = handler.obtainMessage();
            bundle.putFloatArray("dragPos", new float[]{0.0f, 0.0f});
            message.setData(bundle);
            handler.sendMessage(message);
        });
        th.start();
    }

    private ArrayList<Pair<Integer, Integer>> getSelectedParts(){
        int numChildren = selectPartLayout.getChildCount();
        ArrayList<SelectPartButton> selectPartButtons = new ArrayList<>();
        for(int i = 0; i < numChildren; ++i){
            View view = selectPartLayout.getChildAt(i);
            if(view instanceof SelectPartButton){
                selectPartButtons.add((SelectPartButton)view);
            }
        }
        Object[] selected = selectPartButtons.stream().filter((e)->e.isChecked()).map((e)->e.getCutPoints()).toArray();
        ArrayList<Pair<Integer, Integer>> selectedParts = new ArrayList<>();
        for(Object pair : selected){
            selectedParts.add((Pair<Integer, Integer>)pair);
        }
        return selectedParts;
    }

    private void restoreBottomToolbar(){
        Thread th = new Thread(()->{
            float y1_pos = startTaskButton.getY();
            float y2_pos = bottomAppBarLayout.getY();
            Bundle bundle = new Bundle();
            float h = scale * 50;
            float inc = 5;
            while((inc < h) && (y1_pos > startButtonInitialYPosition)) {
                y1_pos -= 5;
                y2_pos -= 5;
                inc += 5;
                bundle.putFloatArray("dragPos", new float[]{y1_pos, y2_pos});
                Message message = handler.obtainMessage();
                message.setData(bundle);
                handler.sendMessage(message);
                try {
                    Thread.sleep(20);
                }catch(InterruptedException interruptedException){}
            }
        });
        th.start();
    }

    private void showTaskProgressView(){
        if(editActivityLayout.indexOfChild(taskProgressViewLayout) == -1) {
            editActivityLayout.addView(taskProgressViewLayout);
        }
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams)taskProgressViewLayout.getLayoutParams();
        layoutParams.topToBottom = editTypeRadioButtons.getId();
        layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        layoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        taskProgressView = (TextView)taskProgressViewLayout.getChildAt(0);
        editActivityLayout.removeView(taskProgressViewLayout);
        editActivityLayout.addView(taskProgressViewLayout);
        taskProgressView.setGravity(Gravity.CENTER);
        taskProgressView.setText("0%");
    }

    private void removeTaskProgressView(){
        editActivityLayout.removeView(taskProgressViewLayout);
    }

    private void showErrorSnackBar(String errorMessage){
        Snackbar errorSnackbar = Snackbar.make(editActivityLayout, errorMessage, 3500);
        errorSnackbar.show();
    }

    private void showCancelledToast(String cancelMessage){
        Toast cancelToast = Toast.makeText(this, cancelMessage, Toast.LENGTH_LONG);
        cancelToast.show();
    }

    private void showTaskCompleteToast(String message){
        Toast taskCompleteToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        taskCompleteToast.show();
    }

    private void disableViews(){
        for(View view : views){
            view.setEnabled(false);
        }
    }

    private void enableViews(){
        for(View view : views){
            view.setEnabled(true);
        }
    }

    private void setCancelActionModeCallback(){
        cancelActionModeCallback = new ActionMode.Callback(){
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu){
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.cancel_contextual_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu){
                return false;
            }

            @Override

            public boolean onActionItemClicked(ActionMode mode, MenuItem item){
                final int cancelItemId = R.id.cancel_operation;
                switch (item.getItemId()){
                    case cancelItemId:
                        editActivityLayout.removeView(selectPartScrollLayout);
                        restoreBottomToolbar();
                        enableViews();
                        setButtonState(StartTaskButtonState.SELECT);
                        mode.finish();
                        break;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode){
                editActivityLayout.removeView(selectPartScrollLayout);
                if(buttonState == StartTaskButtonState.START) {
                    restoreBottomToolbar();
                    enableViews();
                    setButtonState(StartTaskButtonState.SELECT);
                }
                cancelActionMode = null;
            }
        };
    }

    private void showCancelContextualMenu(){
        cancelActionMode = startSupportActionMode(cancelActionModeCallback);
    }

    private void removeCancelContextualMenu(){
        if(cancelActionMode != null){
            cancelActionMode.finish();
        }
    }

    private void startResultsActivity(ArrayList<String> paths){
        Intent intent = new Intent(this, ResultsActivity.class);
        intent.putStringArrayListExtra("paths", paths);
        startActivity(intent);
    }

    private class Handler1 extends Handler{
        public Handler1(Looper looper){
            super(looper);
        }
        @Override
        public void handleMessage(Message message){
            bundle = message.getData();
            String fileAdded = bundle.getString("fileAdded", "");
            if(!fileAdded.isEmpty()){
                mediaScanner.enqueueMediaFile((new MediaFile(fileAdded)));
            }
            String progress = bundle.getString("taskProgress", "");
            if(!progress.isEmpty() && taskProgressView != null){
                taskProgressView.setText(progress);
            }
            int complete = bundle.getInt("complete", -1);
            if(complete == 0){
                showTaskCompleteToast("Task Completed.");
                try{
                    Thread.sleep(250L);
                }catch (InterruptedException interruptedException){}
                removeTaskProgressView();
                restoreBottomToolbar();
                setButtonState(StartTaskButtonState.SELECT);
                enableViews();
                paths = bundle.getStringArrayList("paths");
                //System.out.println("-------------TASK COMPLETED--------------");
                startResultsActivity(paths);
                //setCompleted(false, true);
                return;
            }
            float[] y_pos = bundle.getFloatArray("dragPos");
            if(y_pos != null){
                if(y_pos[0] == 0.0f){
                    if(!grabThumbnail.isChecked()) {
                        addSelectPartButtonLayout();
                    }
                    return;
                }
                startTaskButton.setY(y_pos[0]);
                bottomAppBarLayout.setY(y_pos[1]);
            }
            float percentagePosition = bundle.getFloat("currentPosition", -1.0f);
            if(percentagePosition >= 0){
                timeline.setSlider_x(percentagePosition);
                currentPosition = videoView.getCurrentPosition();
            }
            String errorMessage = bundle.getString("errorMessage", "");
            if(!errorMessage.isEmpty()){
                removeTaskProgressView();
                restoreBottomToolbar();
                setButtonState(StartTaskButtonState.SELECT);
                enableViews();
                showErrorSnackBar(errorMessage);
            }
            String cancelMessage = bundle.getString("cancelled", "");
            if(!cancelMessage.isEmpty()){
                enableViews();
                showCancelledToast(cancelMessage);
            }
        }
    }

    private void showErrorDialog(String title, String message){
        AlertDialogBox dialogBox = new AlertDialogBox();
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("message", message);
        bundle.putString("positiveButton", "Ok");
        dialogBox.setArguments(bundle);
        dialogBox.show(getFragmentManager(), "File Missing");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog){
        finish();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog){
    }

    @Override
    public void onDialogNeutralClick(DialogFragment dialog){
    }

    @Override
    public void onCancel(DialogInterface dialog){
        finish();
    }
}