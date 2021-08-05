package com.anaphase.videoeditor.ui;

import android.app.Dialog;
import android.app.DialogFragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.anaphase.videoeditor.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AlertDialogBox extends DialogFragment {
    private AlertDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity(), R.style.VE_AlertDialogTheme);
        Bundle bundle = getArguments();
        String title = bundle.getString("title");
        String message = bundle.getString("message");
        String positiveButton = bundle.getString("positiveButton", null);
        String negativeButton = bundle.getString("negativeButton", null);
        String neutralButton = bundle.getString("neutralButton", null);
        builder.setMessage(message);
        builder.setTitle(title);
        if(positiveButton != null) {
            builder.setPositiveButton(positiveButton, (dialog, id) -> {
                listener.onDialogPositiveClick(AlertDialogBox.this);
            });
        }
        if(negativeButton != null) {
            builder.setNegativeButton(negativeButton, (dialog, id) -> {
                listener.onDialogNegativeClick(AlertDialogBox.this);
            });
        }
        if(neutralButton != null){
            builder.setNeutralButton(neutralButton, (dialog, id)->{
                listener.onDialogNeutralClick(AlertDialogBox.this);
            });
        }
        return builder.create();
    }

    public interface AlertDialogListener{
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
        public void onDialogNeutralClick(DialogFragment dialog);
        public void onCancel(DialogInterface dialog);
    }
    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        try{
            listener = (AlertDialogListener)context;
        }catch(ClassCastException classCastException){
            throw new ClassCastException(context.getClass().getName() + " must implement AlertDialogListener");
        }
    }

    @Override
    public void onCancel(DialogInterface dialog){
        listener.onCancel(dialog);
    }
}