package com.watt.camera1n2.Control;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class ImageControll extends RelativeLayout {

    boolean error = false;

    public ImageControll(Context context) {
        super(context);
    }

    public ImageControll(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void zoomListener(zoomListener zoomlistener){
        zoomViewer.zoomListener(zoomlistener);
    }

    public void resetzoom(){
        zoomViewer.resetzoom();
    }

    ZoomControll zoomViewer =new ZoomControll(getContext());

    public void showImage(ImageControll layout){

        if(error){
            zoomViewer.errorMessage();
            /*이미지 설정에 문제가있을경우 오류 메시지 화면에 출력 */
        }
        else{
            zoomViewer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
            zoomViewer.startViewer(zoomViewer,layout);
        }
    }
    public void count(int pageCount,int fullPage){
        zoomViewer.count(pageCount, fullPage);
    }
    public void pdfSetting(Context context,String filePath){
        error = false;
        zoomViewer.pdfSetting(context,filePath);
    }
    public void setImage(Bitmap bitmap){
        if(bitmap==null){
            error = true;
        }
        else{
            zoomViewer.setImage(bitmap);
        }
    }
    public void setImage(Uri uri){
        if(uri==null){
            error = true;
        }
        else {
            zoomViewer.setImage(uri);
        }
    }
    public void setImage(String string){
        if(string==null){
            error = true;
        }
        else{
            zoomViewer.setImage(string);
        }
    }
    public void setImage(ArrayList<Uri> uri){
        if(uri==null){
            error = true;
        }
        else{
            zoomViewer.setImage(uri);
        }
    }
    public void setStringData(ArrayList<String> string){
        if(string==null){
            error = true;
        }
        else{
            zoomViewer.setStringData(string);
        }
    }
    public void setBitmapData(ArrayList<Bitmap> bitmap){
        if(bitmap==null){
            error = true;
        } else {
            zoomViewer.setBitmapData(bitmap);
        }
    }




}
