package com.watt.camera1n2.Control;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.watt.camera1n2.R;

import java.util.ArrayList;


public class ZoomControll extends FrameLayout implements SenSorScroll.ScrollListener ,loadMsgID , zoomListener {

    private static final String TAG = "Daengjun";

    SenSorScroll mTiltScrollController = new SenSorScroll(getContext(), this);

    zoomListener zoomlistener;

    public void zoomListener(zoomListener zoomlistener){
        this.zoomlistener = zoomlistener;
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onZoomOut() {
    }

    public interface ZoomViewListener {

        void onZoomStarted(float zoom, float zoomx, float zoomy);

        void onZooming(float zoom, float zoomx, float zoomy);

        void onZoomEnded(float zoom, float zoomx, float zoomy);
    }

    // zooming
    float zoom = 1.0f;
    float maxZoom = 2.0f;
    float smoothZoom = 1.0f;
    float zoomX, zoomY;
    float smoothZoomX, smoothZoomY;
    private boolean scrolling, display_move_control = true;

    // minimap variables
    private boolean showMinimap = false;
    private int miniMapHeight = -1;

    // touching variables
    private long lastTapTime;
    private float touchStartX, touchStartY;
    private float touchLastX, touchLastY;
    private float startd;
    private boolean pinching;
    private float lastd;
    private float lastdx1, lastdy1;
    private float lastdx2, lastdy2;
    float dxk, dyk;
    private float zoomlevelChoice;
    private int listData, data;
    private int viewCheck = 0;
    private ImageView imageView;
    private int position = 0;
    private final Matrix m = new Matrix();
    private final Paint p = new Paint();
    private ZoomControll zooms;
    private ZoomViewListener listener;
    private RelativeLayout relative;
    private Bitmap ch;
    private Bitmap bimapImg;
    private Uri uriImg;
    private String ImgAdress;
    private ArrayList<Uri> UriImgdatas = new ArrayList<Uri>();
    private ArrayList<String> Stringimgdatas = new ArrayList<String>();
    private ArrayList<Bitmap> Bitmapimgdatas = new ArrayList<Bitmap>();
    private boolean FirstOpen = true;
    private float minimapx, minimapy , num;
    private PDFControll pdfView;
    private Context context;
    private int fullPage;
    private int currentPage;
    private String filePath;
    private boolean pageStatus;
    boolean minimapBorder = true;
    boolean modelGlass = true;
    private Bitmap minimapImage;



    public ZoomControll(final Context context) {
        super(context);
    }

    public ZoomControll(final Context context, final AttributeSet attrs) {
        super(context, attrs);

    }

    public float getZoom() {
        return zoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(final float maxZoom) {
        if (maxZoom < 1.0f) {
            return;
        }

        this.maxZoom = maxZoom;
    }


    public void setMiniMapEnabled(final boolean showMiniMap) {
        this.showMinimap = showMiniMap;
    }

    public boolean isMiniMapEnabled() {
        return showMinimap;
    }

    public void setMiniMapHeight(final int miniMapHeight) {
        if (miniMapHeight < 0) {
            return;
        }
        this.miniMapHeight = miniMapHeight;
    }

    public int getMiniMapHeight() {
        return miniMapHeight;
    }

    public void zoomTo(final float zoom, final float x, final float y) {
        this.zoom = Math.min(zoom, maxZoom);
        zoomX = x;
        zoomY = y;
        smoothZoomTo(this.zoom, x, y);
    }

    public void smoothZoomTo(final float zoom, final float x, final float y) {
        //내가 현재 이동하려는 좌표를 알려고 x,y값을 넣어주는것
        smoothZoom = clamp(1.0f, zoom, maxZoom);
        smoothZoomX = x;
        smoothZoomY = y;


        if (listener != null) {
            listener.onZoomStarted(smoothZoom, x, y);
        }
    }

    public ZoomViewListener getListener() {
        return listener;
    }

    public void setListner(final ZoomViewListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {

        if (ev.getPointerCount() == 1) {
            processSingleTouchEvent(ev);
        }

        // // double touch
        if (ev.getPointerCount() == 2) {
            processDoubleTouchEvent(ev);
        }
        if (ev.getPointerCount() == 11) {
            processDoubleTouchEvent(ev);
        }

        return true;
    }

    private void processSingleTouchEvent(final MotionEvent ev) {

        processSingleTouchOutsideMinimap(ev);

    }

    private void processSingleTouchOutsideMinimap(final MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();
        float lx = x - touchStartX;
        float ly = y - touchStartY;
        final float l = (float) Math.hypot(lx, ly);
        float dx = x - touchLastX;
        float dy = y - touchLastY;
        touchLastX = x;
        touchLastY = y;


        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = x;
                touchStartY = y;
                touchLastX = x;
                touchLastY = y;
                dx = 0;
                dy = 0;
                lx = 0;
                ly = 0;
                scrolling = false;
                break;

            case MotionEvent.ACTION_MOVE:


                if (scrolling || (smoothZoom > 1.0f && l > 30.0f)) {
                    if (!scrolling) {
                        scrolling = true;
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(ev);
                    }
                    if (System.currentTimeMillis() - lastTapTime > 200) {
                        smoothZoomX -= dx/(zoom/2);
                        smoothZoomY -= dy/(zoom/2);
                    }

                    return;
                }
                break;

            case MotionEvent.ACTION_BUTTON_PRESS:
                if (l < 30.0f) {
//                    scrolling = true;
                }

            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_UP:
                /*daengjun
                 *모바일에서만 두번터치 줌 활성화 , 비활성화 */
//                if (!Build.MODEL.equals("T1100G") || !Build.MODEL.equals("T1100S") || !Build.MODEL.equals("T1200G")) {
//
//                    if (l < 30.0f) {
//                        // check double tap
//                        if (System.currentTimeMillis() - lastTapTime < 500) {
//                            if (smoothZoom == 1.0f) {
//                                smoothZoomTo(maxZoom, x, y);
//                                zoomlevelcheck(maxZoom);
//                            } else {
//                                smoothZoomTo(1.0f, getWidth() / 2.0f, getHeight() / 2.0f);
//                                zoomlevelcheck(1);
//                            }
//                            lastTapTime = 0;
//                            ev.setAction(MotionEvent.ACTION_CANCEL);
//                            super.dispatchTouchEvent(ev);
//                            return;
//                        }
//                        zoomlevelcheck(zoom);
//                        lastTapTime = System.currentTimeMillis();
//                        performClick();
//                    }
//                }
//                break;

            default:
                break;
        }

//        ev.setLocation(zoomX + (x * getWidth()) / zoom, zoomY + (y * getHeight()) / zoom);

        ev.getX();
        ev.getY();

        super.dispatchTouchEvent(ev);
    }

    private void processDoubleTouchEvent(final MotionEvent ev) {
        final float x1 = ev.getX(0);
        final float dx1 = x1 - lastdx1;
        lastdx1 = x1;
        final float y1 = ev.getY(0);
        final float dy1 = y1 - lastdy1;
        lastdy1 = y1;
        final float x2 = ev.getX(1);
        final float dx2 = x2 - lastdx2;
        lastdx2 = x2;
        final float y2 = ev.getY(1);
        final float dy2 = y2 - lastdy2;
        lastdy2 = y2;

        float  d = (float) Math.hypot(x2 - x1, y2 - y1);
        float dd = d - lastd;
        lastd = d;
        final float ld = Math.abs(d - startd);

        Math.atan2(y2 - y1, x2 - x1);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startd = d;
                pinching = false;
                break;

            case MotionEvent.ACTION_MOVE:
                /* daengjun 모바일 기기에서 줌하는 부분 */
                if (pinching || ld > 30.0f) {
                    pinching = true;
                    dxk = (dx1 + dx2) ;
                    dyk = (dy1 + dy2) ;

                    zoomlevelcheck(zoom);

                    smoothZoomTo(Math.max(1.0f, zoom * d / (d - dd)), zoomX - dxk / zoom, zoomY - dyk / zoom);

                    lastTapTime = System.currentTimeMillis();
                }

                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            default:
                pinching = false;
                break;
        }

        ev.setAction(MotionEvent.ACTION_CANCEL);
        super.dispatchTouchEvent(ev);
    }

    private float clamp(final float min, final float value, final float max) {
        return Math.max(min, Math.min(value, max));
    }

    private float lerp(final float a, final float b, final float k) {
        return a + (b - a) * k;
    }

    private float bias(final float a, final float b, final float k) {
        return Math.abs(b - a) >= k ? a + k * Math.signum(b - a) : b;
    }

    @Override
    protected void dispatchDraw(final Canvas canvas) {
        zoom = lerp(bias(zoom, smoothZoom, 0.05f), smoothZoom, 0.2f);

        smoothZoomX = clamp(0.5f * getWidth() / smoothZoom, smoothZoomX, getWidth() - 0.5f * getWidth() / smoothZoom);
        smoothZoomY = clamp(0.5f * getHeight() / smoothZoom, smoothZoomY, getHeight() - 0.5f * getHeight() / smoothZoom);

        zoomX = lerp(bias(zoomX, smoothZoomX, 0.1f), smoothZoomX, 0.35f);
        zoomY = lerp(bias(zoomY, smoothZoomY, 0.1f), smoothZoomY, 0.35f);
        if (zoom != smoothZoom && listener != null) {
            listener.onZooming(zoom, zoomX, zoomY);
        }

//        final boolean animating = Math.abs(zoom - smoothZoom) > 0.0000001f
//                || Math.abs(zoomX - smoothZoomX) > 0.0000001f || Math.abs(zoomY - smoothZoomY) > 0.0000001f;

        if (getChildCount() == 0) {
            return;
        }
        m.setTranslate(0.5f * getWidth(), 0.5f * getHeight());

        if (modelGlass) {

            if (zoom == 1) {
                m.preScale((float) (zoom / 1.2), zoom);
            } else {
                m.preScale(zoom / 3, zoom / 3);
            }
        }
        else{
            m.preScale(zoom,zoom);
        }

        m.preTranslate(-clamp(0.5f * getWidth() / zoom, zoomX, getWidth() - 0.5f * getWidth() / zoom),
                -clamp(0.5f * getHeight() / zoom, zoomY, getHeight() - 0.5f * getHeight() / zoom));

        final View v = getChildAt(0);
        m.preTranslate(v.getLeft(), v.getTop());
        v.setDrawingCacheEnabled(true);

        canvas.save();
        canvas.concat(m);

        imageView.draw(canvas);

        canvas.restore();
        showMinimap = true;


        if (showMinimap) {
            if (miniMapHeight < 0) {
                miniMapHeight = getHeight() / 3;
            }

            final int w = (int) (miniMapHeight * (float) getWidth() / getHeight() / 1.4);
            final int h = (int) (miniMapHeight / 1.3);

            int widthMinus = 0 ;
            if (modelGlass) {
                /* 글라스 미니맵 가로 위치값 */
                widthMinus = 10;
            }
            else{
                /* 모바일기기 미니맵 가로 위치값 */
                widthMinus = 30;
            }
            int minimapWidth = (int) (getWidth() * 2 / 2.6 - widthMinus);
            int minimapHeight = 5;//(int) (getHeight() * 2 / 2.8 + 10);

            canvas.translate(minimapWidth, minimapHeight);
            p.setAntiAlias(true);

            if(minimapImage!=null) {
                canvas.drawBitmap(minimapImage, 0, 0, p);
            }
         /* p.setAlpha(100);  // 투명도 조정
            minimap = false; // 미니맵 활성화, 비활성화 */

            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.BLACK);
            p.setStrokeWidth(1);
            final float  dx = (w * zoomX / getWidth());
            final float dy = (h * zoomY / getHeight()) ;

            if (zoom == 1&& minimapBorder) {

                if(minimapx==dx){
                    minimapBorder = false;
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(dx>0)
                        minimapx = dx;
                        minimapy = dy;
                    }
                }, 200);
            }

            /*미니맵 이미지 검은 테두리 처음에만 할당하기 위해서 */
            if(!minimapBorder)
            canvas.drawRect(minimapx - 0.5f * w, minimapy - 0.5f * h, minimapx + 0.5f * w, minimapy + 0.5f * h, p);
            canvas.clipRect(minimapx - 0.5f * w, minimapy - 0.5f * h, minimapx + 0.5f * w, minimapy + 0.5f * h);

            if(!FirstOpen) {
            if(!minimapBorder)
                prograssOff();
            }

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(Color.RED);

            Paint clearPaint = new Paint();
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

            if (modelGlass) {
     /* 글라스와 모바일에서 미니맵 위치가 조금 차이가 있음 */
                if (zoom > 1) {
                    canvas.drawRect(dx - 1.5f * w / zoom - 2, dy - 1.5f * h / zoom - 2, dx + 1.5f * w / zoom - 2, dy + 1.5f * h / zoom - 2,p);
                } else {
                    canvas.drawRect(0, 0, 0, 0, clearPaint);
                }
            }
            else{
                if (zoom > 1) {
                    canvas.clipRect(dx - 0.5f * w / zoom - 2, dy - 0.5f * h / zoom - 2, dx + 0.5f * w / zoom - 2, dy + 0.5f * h / zoom - 2);
                } else {
                    canvas.drawRect(0, 0, 0, 0, clearPaint);
                }
            }
        }

        getRootView().invalidate();
        invalidate();
    }

    public void zoomlevel_choice(float num) {
        zoom = num;
        smoothZoomTo(num, zoomX - dxk / zoom, zoomY - dyk / zoom);
    }

    public void Move_Sensor(double dx, double dy) {
        smoothZoomX -= dx / zoom;
        smoothZoomY -= dy / zoom;
    }

    @Override
    public void onTilt(float x, float y) {

        if (modelGlass) {
            if (display_move_control) {
                Move_Sensor(-x * zoom/2.5, -y * zoom/2.5);
            }
        }
    }

    View imageview_item = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.zoom_item, null, false);
    View relative_item = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.activity_image_view, null, false);

    TextView zoom_control1 = relative_item.findViewById(R.id.zoomlevel_voice1);
    TextView zoom_control2 = relative_item.findViewById(R.id.zoomlevel_voice2);
    TextView zoom_control3 = relative_item.findViewById(R.id.zoomlevel_voice3);
    TextView zoom_control4 = relative_item.findViewById(R.id.zoomlevel_voice4);
    TextView zoom_control5 = relative_item.findViewById(R.id.zoomlevel_voice5);

    TextView zoomlevel1 = relative_item.findViewById(R.id.level1);
    TextView zoomlevel2 = relative_item.findViewById(R.id.level2);
    TextView zoomlevel3 = relative_item.findViewById(R.id.level3);
    TextView zoomlevel4 = relative_item.findViewById(R.id.level4);
    TextView zoomlevel5 = relative_item.findViewById(R.id.level5);

    RelativeLayout relativeLayout = relative_item.findViewById(R.id.container);

    RelativeLayout displayBorder = relative_item.findViewById(R.id.border2);
    RelativeLayout nextBorder = relative_item.findViewById(R.id.border3);
    RelativeLayout beforeBorder = relative_item.findViewById(R.id.border4);
    TextView imageScale = relative_item.findViewById(R.id.reduction);


    View process = relative_item.findViewById(R.id.loading);

    TextView close = relative_item.findViewById(R.id.viewerClose);

    TextView displayMoveOn = relative_item.findViewById(R.id.displayMove);
    TextView displayMoveOff = relative_item.findViewById(R.id.displayStop);

    TextView next_btn = relative_item.findViewById(R.id.nextImg);
    TextView before_btn = relative_item.findViewById(R.id.beforeImg);

    TextView item_1 = relative_item.findViewById(R.id.select_item1);
    TextView item_2 = relative_item.findViewById(R.id.select_item2);
    TextView item_3 = relative_item.findViewById(R.id.select_item3);
    TextView item_4 = relative_item.findViewById(R.id.select_item4);
    TextView item_5 = relative_item.findViewById(R.id.select_item5);

    Animation fadeInAnim_in = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
    Animation fadeInAnim_out = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);

    public void inItListener() {

        displayMoveOn.setOnClickListener(Tiltcontroll_voice_input);
        displayMoveOff.setOnClickListener(Tiltcontroll_voice_input);
        zoom_control1.setOnClickListener(Tiltcontroll_voice_input);
        zoom_control2.setOnClickListener(Tiltcontroll_voice_input);
        zoom_control3.setOnClickListener(Tiltcontroll_voice_input);
        zoom_control4.setOnClickListener(Tiltcontroll_voice_input);
        zoom_control5.setOnClickListener(Tiltcontroll_voice_input);
        next_btn.setOnClickListener(Tiltcontroll_voice_input);
        before_btn.setOnClickListener(Tiltcontroll_voice_input);
        close.setOnClickListener(Tiltcontroll_voice_input);

        item_1.setOnClickListener(Tiltcontroll_voice_input);
        item_2.setOnClickListener(Tiltcontroll_voice_input);
        item_3.setOnClickListener(Tiltcontroll_voice_input);
        item_4.setOnClickListener(Tiltcontroll_voice_input);
        item_5.setOnClickListener(Tiltcontroll_voice_input);

        if (Build.MODEL.equals("T1100G") || Build.MODEL.equals("T1100S") || Build.MODEL.equals("T1200G") || Build.MODEL.equals("T21G")) {
            modelGlass = true;
        }
        else{
            modelGlass = false;
        }

        item_5.setText(getResources().getString(R.string.select4));
        imageScale.setOnClickListener(Tiltcontroll_voice_input);
        nextBorder.setVisibility(GONE);
        beforeBorder.setVisibility(GONE);

        if (modelGlass) {
            item_4.setEnabled(false);
        }
        else{
            displayBorder.setVisibility(GONE);
            item_2.setEnabled(false);
            item_4.setEnabled(false);
        }
    }

    public void startViewer(ZoomControll zoom, RelativeLayout layout) {
        relative = layout;

        if (relative.getVisibility() == View.GONE) {
            display_move_control = true;
            zoomlevelcheck(1);
            handlerload();
            relative.setVisibility(VISIBLE);
//            relative.startAnimation(fadeInAnim_in);

        } else {
            if (FirstOpen) {
                /* 처음 실행되는 부분 딱한번만 실행됨 */
                FirstOpen = false;
                zoomlevelcheck(1);
                prograssOn();
                inItListener();
                zooms = zoom;
                imageView = imageview_item.findViewById(R.id.viewImg);
                handlerload();

                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                zooms.setLayoutParams(layoutParams);

                zooms.setMiniMapEnabled(true);
                if (modelGlass) {
                    zooms.setMaxZoom(10f);
                }
                else{
                    zooms.setMaxZoom(7f);
                }
                imageview_item.setBackgroundColor(getContext().getResources().getColor(R.color.gray));
                relative.addView(relativeLayout, 0);
                zooms.addView(imageview_item, 0);
                relative.addView(zooms, 0);
                relative.setBackgroundColor(getContext().getResources().getColor(R.color.gray));
//                relative.startAnimation(fadeInAnim_in);
            }
        }
    }


    public void handlerload(){
        Message msg = new Message();
        msg.what = imageLoad;
        eventHandle.sendMessage(msg);
    }


    private OnClickListener Tiltcontroll_voice_input = new OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.displayMove) {
                Message msg = new Message();
                msg.arg1 = move;
                msg.what = displayContol;
                eventHandle.sendMessage(msg);
            } else if (id == R.id.displayStop) {
                Message msg = new Message();
                msg.arg1 = stop;
                msg.what = displayContol;
                eventHandle.sendMessage(msg);
            }
            else if (id == R.id.viewerClose) {

                zoomlistener.onClose();
            }

            else if (id == R.id.zoomlevel_voice1) {
                if (zoom != 1) {
                    zoomlevelcheck(1f);
                }
            } else if (id == R.id.zoomlevel_voice2) {
                zoomlevelcheck(5f);
            } else if (id == R.id.zoomlevel_voice3) {
                zoomlevelcheck(6f);
            } else if (id == R.id.zoomlevel_voice4) {
                zoomlevelcheck(7f);
            } else if (id == R.id.zoomlevel_voice5) {
                zoomlevelcheck(8f);
            } else if (id == R.id.nextImg) {
                Message msg = new Message();
                msg.what = PageContol;
                msg.arg1 = nextPage;
                eventHandle.sendMessage(msg);
            }
            else if (id == R.id.beforeImg) {
                Message msg = new Message();
                msg.what = PageContol;
                msg.arg1 = beforePage;
                eventHandle.sendMessage(msg);
            }
            else if (id == R.id.select_item1) {
                Message msg = new Message();
                msg.what = select1;
                eventHandle.sendMessage(msg);

            } else if (id == R.id.select_item2) {
                Message msg = new Message();
                msg.arg1 = select2;
                msg.what = displayContol;
                eventHandle.sendMessage(msg);

            }
//            else if (id == R.id.select_item3) {
//                Message msg = new Message();
//                msg.what = PageContol;
//                msg.arg1 = beforePage;
//                eventHandle.sendMessage(msg);
//
//            }
            else if (id == R.id.select_item3) {
                zoomlistener.onZoomOut();
            }else if (id == R.id.select_item4) {
                Message msg = new Message();
                msg.what = PageContol;
                msg.arg1 = nextPage;
                eventHandle.sendMessage(msg);
            }  else if (id == R.id.select_item5) {

                zoomlistener.onClose();
            }

            else if (id == R.id.reduction) {
                zoomlistener.onZoomOut();
            }
        }
    };


    public Handler eventHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case select1:
                    num = zoom;

                    if (num == 1) {
                        zoomlevelcheck(5);
                        num = num + 1;
                    } else if (num == 5) {
                        zoomlevelcheck(6);
                        num = num + 1;

                    } else if (num == 6) {
                        zoomlevelcheck(7);
                        num = num + 1;

                    } else if (num == 7) {
                        zoomlevelcheck(8);
                        num = num + 1;
                    } else {
                        zoomlevelcheck(1);
                    }
                    break;

                case displayContol:
                    displayControl(msg.arg1);
                    break;

                case PageContol:
                    pageControl(msg.arg1);
                    break;
                case imageLoad:
                    loadImg();
            }
        }
    };



    public void resetzoom(){
        zoomlevelcheck(1);
    }

    private void pageControl(int control) {

        Log.d(TAG, "pageControl: " + pageStatus);

        if(control == nextPage){

            /*nextPage*/
            if (viewCheck != 1) {
                pageStatus = true;
                Log.d(TAG, "pageControl: position" + position);

                switch (listData){
                    case 1:
                        zoomlevelcheck(1);
                        position = position + 1;

                        if(position == UriImgdatas.size()) {
                            next_btn.setTextColor(getResources().getColor(R.color.Transparency));
                            before_btn.setTextColor(getResources().getColor(R.color.white));
                        }
                        if (position < UriImgdatas.size()) {
                            prograssOn();
                            loadImg();
                            if(pageStatus)
                                PageEnable(true,false);
                            prograssOff();
                        }
                        else {
                            position = UriImgdatas.size() - 1;
                            Toast.makeText(getContext(), "마지막 이미지 입니다", Toast.LENGTH_SHORT).show();
                        }

                        break;
                    case 2:
                        zoomlevelcheck(1);
                        position = position + 1;
                        if (position < Stringimgdatas.size()) {
                            prograssOn();
                            loadImg();
                            if(pageStatus)
                                PageEnable(true,false);
                            prograssOff();
                        } else {
                            position = Stringimgdatas.size() - 1;
                            Toast.makeText(getContext(), "마지막 이미지 입니다", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 3:
                        zoomlevelcheck(1);
                        position = position + 1;
                        if (position < Bitmapimgdatas.size()) {
                            prograssOn();
                            loadImg();
                            if(pageStatus)
                                PageEnable(true,false);
                            prograssOff();
                        } else {
                            position = Bitmapimgdatas.size() - 1;
                            Toast.makeText(getContext(), "마지막 이미지 입니다", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
            else{
                relative.setVisibility(GONE);

                if (currentPage != fullPage - 1) {
                    currentPage = currentPage + 1;
                    Log.d(TAG, "deangjun currentPage : " + currentPage);
                }

                pdfView.openPdf(context, filePath);
                setImage(pdfView.showPage(currentPage));
                count(currentPage, fullPage);
                startViewer(null, relative);
                prograssOff();

            }

        }

        /*beforePage*/
        else{
            if (viewCheck != 1) {
                pageStatus = true;

                if (listData != 0) {
                    zoomlevelcheck(1);
                    position = position - 1;
                    if (position > -1) {
                        prograssOn();
                        loadImg();
                        if(pageStatus)
                            PageEnable(true,false);
                        prograssOff();
                    }

                    else {
                        position = 0;
                        Toast.makeText(getContext(), "첫번째 이미지 입니다", Toast.LENGTH_SHORT).show();

                    }
                }
            }
            else{
                relative.setVisibility(GONE);

                if (currentPage > 0) {
                    currentPage = currentPage - 1;
                    Log.d(TAG, "deangjun currentPage : " + currentPage);
                }

                pdfView.openPdf(context, filePath);
                setImage(pdfView.showPage(currentPage));
                count(currentPage, fullPage);
                startViewer(null, relative);
                prograssOff();
            }
        }
    }

    private void displayControl(int btn) {


        if (zoomlevelChoice > 1) {

            switch (btn){
                case move:
                    displayMoveOff.setTextColor(getContext().getResources().getColor(R.color.Transparency));
                    displayMoveOn.setTextColor(getContext().getResources().getColor(R.color.yellow));
                    display_move_control = true;
                    break;

                case stop:
                    displayMoveOff.setTextColor(getContext().getResources().getColor(R.color.yellow));
                    displayMoveOn.setTextColor(getContext().getResources().getColor(R.color.Transparency));
                    display_move_control = false;
                    break;

                case select2:
                    if (!display_move_control) {
                        displayMoveOff.setTextColor(getContext().getResources().getColor(R.color.Transparency));
                        displayMoveOn.setTextColor(getContext().getResources().getColor(R.color.yellow));
                        display_move_control = true;
                    } else {
                        displayMoveOff.setTextColor(getContext().getResources().getColor(R.color.yellow));
                        displayMoveOn.setTextColor(getContext().getResources().getColor(R.color.Transparency));
                        display_move_control = false;
                    }
                    break;
            }
        }
    }

    private void close() {
        display_move_control = false;
        relative.startAnimation(fadeInAnim_out);
        relative.setVisibility(GONE);
        if (viewCheck != 1)
            reset();
    }

    private void setMinimapImage(){
        final View v = getChildAt(0);
        final int w = (int) ((getHeight()/3) * (float) getWidth() / getHeight() / 1.4);
        final int h = (int) ((getHeight()/3)/ 1.3);

        Bitmap resize_bitmap = Bitmap.createScaledBitmap(v.getDrawingCache(), w, h, true);
        minimapImage = resize_bitmap;
    }

    private void loadImg() {
        pageStatus = true;

        switch (data) {
            case 1:
                Glide.with(this)
                        .load(ImgAdress)
                        .override(Target.SIZE_ORIGINAL)
                        .into(imageView);

                PageEnable(false,true);

                break;
            case 2:
                Glide.with(this)
                        .load(uriImg)
                        .override(Target.SIZE_ORIGINAL)
                        .into(imageView);
                PageEnable(false,true);
                break;
            case 3:
                Glide.with(this)
                        .asBitmap()
                        .load(bimapImg)
                        .override(Target.SIZE_ORIGINAL)
                        .into(new CustomTarget<Bitmap>() {

                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                imageView.setImageBitmap(resource);
                                setMinimapImage();

                            }
                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }

                        });

                PageEnable(false,true);

                break;
            case 4:
                if (position == 0 && position != (UriImgdatas.size() - 1)) {
                    next_btn.setTextColor(getResources().getColor(R.color.white));
                    before_btn.setTextColor(getResources().getColor(R.color.Transparency));
                    pageStatus = false;
                } else if (position != 0 && position == (UriImgdatas.size() - 1)) {
                    next_btn.setTextColor(getResources().getColor(R.color.Transparency));
                    before_btn.setTextColor(getResources().getColor(R.color.white));
                    pageStatus = false;
                } else if (position == 0 && position != (UriImgdatas.size() - 1)) {
                    PageEnable(true,false);
                }

                Glide.with(this)
                        .load(UriImgdatas.get(position))
                        .override(Target.SIZE_ORIGINAL,Target.SIZE_ORIGINAL)
                        .into(imageView);

                break;
            case 5:
                if (position == 0 && position != (Stringimgdatas.size() - 1)) {
                    next_btn.setTextColor(getResources().getColor(R.color.white));
                    before_btn.setTextColor(getResources().getColor(R.color.Transparency));
                    pageStatus = false;

                } else if (position != 0 && position == (Stringimgdatas.size() - 1)) {
                    next_btn.setTextColor(getResources().getColor(R.color.Transparency));
                    before_btn.setTextColor(getResources().getColor(R.color.white));
                    pageStatus = false;

                } else if (position == 0 && position != (Stringimgdatas.size() - 1)) {
                    PageEnable(true,false);
                }

                Glide.with(this)
                        .load(Stringimgdatas.get(position))
                        .override(Target.SIZE_ORIGINAL)
                        .into(imageView);
                break;
            case 6:
                if (position == 0 && position != (Bitmapimgdatas.size() - 1)) {
                    next_btn.setTextColor(getResources().getColor(R.color.white));
                    before_btn.setTextColor(getResources().getColor(R.color.Transparency));
                    pageStatus = false;

                } else if (position != 0 && position == (Bitmapimgdatas.size() - 1)) {
                    next_btn.setTextColor(getResources().getColor(R.color.Transparency));
                    before_btn.setTextColor(getResources().getColor(R.color.white));
                    pageStatus = false;

                } else if (position == 0 && position != (Bitmapimgdatas.size() - 1)) {
                    PageEnable(true,false);
                }

                Glide.with(this)
                        .load(Bitmapimgdatas.get(position))
                        .override(Target.SIZE_ORIGINAL)
                        .into(imageView);
                break;
        }

        Log.d(TAG, "daengjun pageStatus : "  + pageStatus);
    }


    public void setImage(String adress) {
        ImgAdress = adress;
        data = 1;
    }

    public void setImage(Uri uri) {
        uriImg = uri;
        data = 2;
    }

    public void setImage(Bitmap bitmap) {
        bimapImg = bitmap;
        data = 3;
    }

    public void setImage(ArrayList<Uri> uri) {
        position = 0;
        UriImgdatas = uri;
        data = 4;
        listData = 1;
    }

    public void setStringData(ArrayList<String> string) {
        position = 0;
        Stringimgdatas = string;
        data = 5;
        listData = 2;
    }

    public void setBitmapData(ArrayList<Bitmap> bitmap) {
        position = 0;
        Bitmapimgdatas = bitmap;
        data = 6;
        listData = 3;
    }


    private void reset() {
        data = 0;
        listData = 0;
    }

    public void prograssOn() {
        if(process !=null)
            process.setVisibility(VISIBLE);
    }

    public void prograssOff() {
        if(process !=null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    process.setVisibility(GONE);

                }
            }, 800);
        }
    }

    public void PageEnable(boolean activation,boolean enable){
        if(activation){
            next_btn.setTextColor(getResources().getColor(R.color.white));
            before_btn.setTextColor(getResources().getColor(R.color.white));
        }
        else{
            if(enable){
                next_btn.setEnabled(false);
                before_btn.setEnabled(false);
            }
            next_btn.setTextColor(getResources().getColor(R.color.Transparency));
            before_btn.setTextColor(getResources().getColor(R.color.Transparency));
        }

    }


    public void errorMessage(){
        Toast.makeText(context, "잘못된 파일 형식입니다.", Toast.LENGTH_SHORT).show();
    }

    public void pdfSetting(Context pdfContext,String filePath){
        context = pdfContext;
        pdfView = new PDFControll();
        this.filePath = filePath;

    }


    public void count(int pageCount, int fullPage) {
        /* PDF를 열때 사용하는 메서드 */
        viewCheck = 1;

        currentPage = pageCount;
        this.fullPage = fullPage;

        Log.d(TAG, "daengjun pageCount: " + pageCount);
        Log.d(TAG, "daengjun fullpage: " + fullPage);

        if (pageCount == 0 && pageCount + 1 != fullPage) {
            next_btn.setTextColor(this.getResources().getColor(R.color.white));
            before_btn.setTextColor(this.getResources().getColor(R.color.Transparency));
            next_btn.setEnabled(true);
            before_btn.setEnabled(false);
        } else if (pageCount > 0 && pageCount + 1 == fullPage) {
            next_btn.setTextColor(this.getResources().getColor(R.color.Transparency));
            before_btn.setTextColor(this.getResources().getColor(R.color.white));
            next_btn.setEnabled(false);
            before_btn.setEnabled(true);
        } else if (pageCount == 0 && pageCount + 1 == fullPage) {
            next_btn.setTextColor(this.getResources().getColor(R.color.Transparency));
            before_btn.setTextColor(this.getResources().getColor(R.color.Transparency));
            next_btn.setEnabled(false);
            before_btn.setEnabled(false);
        } else {
            next_btn.setTextColor(this.getResources().getColor(R.color.white));
            before_btn.setTextColor(this.getResources().getColor(R.color.white));
            next_btn.setEnabled(true);
            before_btn.setEnabled(true);
        }
    }

    private void zoomlevelcheck(float check) {

        if (modelGlass) {
            zoomcheckNone();
        }
        else{
            if(check<6)
                zoomcheckNone();
        }

        displayMoveOff.setEnabled(true);
        displayMoveOn.setEnabled(true);

        if(check == 1){
            displayMoveOff.setTextColor(getContext().getResources().getColor(R.color.Transparency));
            displayMoveOn.setTextColor(getContext().getResources().getColor(R.color.Transparency));
            displayMoveOff.setEnabled(false);
            displayMoveOn.setEnabled(false);
        }

        zoomlevelChoice = check;
        zoomcheck();
        zoomlevel_choice(zoomlevelChoice);
    }


    private void zoomcheck() {

        if (modelGlass) {
            switch ((int) zoomlevelChoice) {
                case 1:
                    zoomlevel1.setTextColor(this.getResources().getColor(R.color.yellow));

                    if (!display_move_control) {
                        display_move_control = true;
                    }
                    break;
                case 5:
                    zoomlevel2.setTextColor(this.getResources().getColor(R.color.yellow));
                    if (display_move_control) {
                        displayMoveOn.setTextColor(getContext().getResources().getColor(R.color.yellow));
                    }
                    break;
                case 6:
                    zoomlevel3.setTextColor(this.getResources().getColor(R.color.yellow));
                    if (display_move_control) {
                        displayMoveOn.setTextColor(getContext().getResources().getColor(R.color.yellow));
                    }

                    break;
                case 7:
                    zoomlevel4.setTextColor(this.getResources().getColor(R.color.yellow));
                    if (display_move_control) {
                        displayMoveOn.setTextColor(getContext().getResources().getColor(R.color.yellow));
                    }

                    break;
                case 8:
                    zoomlevel5.setTextColor(this.getResources().getColor(R.color.yellow));
                    if (display_move_control) {
                        displayMoveOn.setTextColor(getContext().getResources().getColor(R.color.yellow));
                    }

                    break;
            }
        }
        else{
            switch ((int) zoomlevelChoice) {
                case 1:
                    zoomlevel1.setTextColor(this.getResources().getColor(R.color.yellow));
                    break;
                case 2:
                    zoomlevel2.setTextColor(this.getResources().getColor(R.color.yellow));
                    break;
                case 3:
                    zoomlevel3.setTextColor(this.getResources().getColor(R.color.yellow));
                    break;
                case 4:
                    zoomlevel4.setTextColor(this.getResources().getColor(R.color.yellow));
                    break;
                case 5:
                    zoomlevel5.setTextColor(this.getResources().getColor(R.color.yellow));
                    break;
            }
        }



    }

    private void zoomcheckNone() {

        if (modelGlass) {
            switch ((int) zoomlevelChoice) {
                case 1:
                    zoomlevel1.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
                case 5:
                    zoomlevel2.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
                case 6:
                    zoomlevel3.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
                case 7:
                    zoomlevel4.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
                case 8:
                    zoomlevel5.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
            }
        }
        else{
            switch ((int) zoomlevelChoice) {
                case 1:
                    zoomlevel1.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
                case 2:
                    zoomlevel2.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
                case 3:
                    zoomlevel3.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
                case 4:
                    zoomlevel4.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
                case 5:
                    zoomlevel5.setTextColor(this.getResources().getColor(R.color.Transparency));
                    break;
            }


        }
    }


}




