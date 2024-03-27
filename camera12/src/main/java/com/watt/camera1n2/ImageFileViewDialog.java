package com.watt.camera1n2;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.watt.camera1n2.Control.ImageControll;
import com.watt.camera1n2.Control.zoomListener;

public class ImageFileViewDialog extends Dialog implements View.OnClickListener , DialogInterface.OnShowListener , DialogInterface.OnDismissListener , DialogInterface.OnCancelListener , zoomListener {

    private static final String TAG = "ImageFileViewDialog";

    private ImageFileViewDialog.Listener listener;
    private Bitmap bmp;
    private String btnText;
    final int imageZoomOut = 1;
    final int reload = 2;
    Bitmap resize_bmp;

    private ImageControll previewImage;


    public interface Listener {
        void onClicked(int clickedID);
        void onShow();
        void onDismiss();
        void onCancel();
    }

    public ImageFileViewDialog(@NonNull Context context , Bitmap bitmap , String btnText , ImageFileViewDialog.Listener listener) {
        super(context , R.style.DialogCustomTheme);
        this.listener = listener;
        this.bmp = bitmap;
        this.btnText = btnText;

        resize_bmp = resizeBitmapImage(bitmap , 4000);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_image_file_save);

        previewImage = (ImageControll)findViewById(R.id.iv_save_img);

        if (getDeviceTypeGlass()) {
            previewImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        TextView infoSaveBtn = (TextView) findViewById(R.id.info_save_btn);
        TextView infoRefreshBtn = (TextView) findViewById(R.id.info_refresh_btn);
        TextView infoCancelBtn = (TextView) findViewById(R.id.info_cancel_btn);

        infoSaveBtn.setOnClickListener(this);
        infoRefreshBtn.setOnClickListener(this);
        infoCancelBtn.setOnClickListener(this);


        previewImage.setImage(this.bmp);
        previewImage.showImage(previewImage);
        previewImage.zoomListener(this);

//        previewImage.setImageBitmap(this.bmp);
        infoSaveBtn.setText(this.btnText);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.info_save_btn) {
            listener.onClicked(0);
            dismiss();
        } else if (id == R.id.info_cancel_btn) {
            listener.onClicked(1);
            dismiss();
        }else if (id == R.id.info_refresh_btn) {
            listener.onClicked(2);
            dismiss();
        }
    }

    @Override
    public void onClose() {

    }

    @Override
    public void onZoomOut() {
//        ScreenControl(imageZoomOut);
    }

//    public void ScreenControl(int control){
//
//        switch(control){
//            case imageZoomOut :
//                this.listener.onDismiss();
//                this.listener.onShow(resize_bmp);
//                Log.d(TAG, "daengjun zoom out");
//                break;
//
//            case reload :
//                previewImage.setImage(this.bmp);
//                previewImage.setVisibility(View.GONE);
//                setFullScreen(false);
//                previewImage.showImage(previewImage);
//                setFullScreen(true);
//                Log.d(TAG, "daengjun Image Reload");
//                break;
//        }
//    }

    public void setFullScreen(boolean visible){
        if(visible){
            previewImage.getChildAt(0).setVisibility(View.VISIBLE);
            previewImage.getChildAt(1).setVisibility(View.VISIBLE);
        }
        else{
            previewImage.getChildAt(0).setVisibility(View.GONE);
            previewImage.getChildAt(1).setVisibility(View.GONE);
        }
    }

    public Bitmap resizeBitmapImage(Bitmap source, int maxResolution)
    {
        int width = source.getWidth();
        int height = source.getHeight();
        int newWidth = width;
        int newHeight = height;
        float rate = 0.0f;

        if(width > height)
        {
            if(maxResolution < width)
            {
                rate = maxResolution / (float) width;
                newHeight = (int) (height * rate);
                newWidth = maxResolution;
            }
        }
        else
        {
            if(maxResolution < height)
            {
                rate = maxResolution / (float) height;
                newWidth = (int) (width * rate);
                newHeight = maxResolution;
            }
        }

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        listener.onCancel();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        listener.onDismiss();
    }

    @Override
    public void onShow(DialogInterface dialogInterface) {
        listener.onShow();
    }


    public static boolean getDeviceTypeGlass()
    {
        boolean isGlassType = false;
        if (Build.MODEL.equals("T1100G") || Build.MODEL.equals("T1100S") || Build.MODEL.equals("T1200G") || Build.MODEL.equals("T21G")|| Build.MODEL.equals("MZ1000"))
            isGlassType = true;
        return isGlassType;
    }
}
