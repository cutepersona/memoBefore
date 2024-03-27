package com.peng.power.memo.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;

import com.peng.power.memo.R;

public class SystemCheckDialog extends Dialog implements View.OnClickListener , DialogInterface.OnShowListener , DialogInterface.OnDismissListener , DialogInterface.OnCancelListener {
    private static final String TAG = "SystemCheckDialog";

    private Context context;
    private SystemCheckDialog.Listener listener;
    private TextView settingBtn;
    private TextView exitBtn;
    private TextView bodyText;

    public interface Listener {
        void onTimeSetting();
        void onExit();

        void onShow();
        void onDismiss();
        void onCancel();
    }

    public SystemCheckDialog(@NonNull Context context , SystemCheckDialog.Listener listener)
    {
        super(context , android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_check_time);

        Log.d(TAG , "Noah onCreate!!");
        settingBtn = (TextView) findViewById(R.id.date_setting);
        exitBtn = (TextView) findViewById(R.id.date_exit);

        bodyText = (TextView) findViewById(R.id.body_text);
        settingBtn.setOnClickListener(this);
        exitBtn.setOnClickListener(this);
    }

    public void setText(String Msg)
    {
        bodyText.setText(Msg);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        Log.d(TAG, "Noah setOnShowListener");
        this.listener.onShow();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.d(TAG, "Noah setOnDismissListener");
        this.listener.onDismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Log.d(TAG, "Noah setOnCancelListener");
        this.listener.onCancel();
    }

    @Override
    public void onClick(View v)
    {
        Log.d(TAG, "Noah onClick id: " + v.getId());
        switch (v.getId())
        {
            case R.id.date_setting:
                this.listener.onTimeSetting();
                break;

            case R.id.date_exit:
                this.listener.onExit();
                break;
        }
    }

}
