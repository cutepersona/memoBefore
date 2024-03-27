package com.peng.power.memo.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


public class VisualizerView extends View {
    private static final int MAX_AMPLITUDE = 32767;

    private float[] amplitudes;
    private float[] vectors;
    private int insertIdx = 0;
    private Paint pointPaint;
    private Paint linePaint;
    private int mViewWidth;
    private int mViewHeight;

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint = new Paint();
        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(1);
        pointPaint = new Paint();
        pointPaint.setColor(Color.BLUE);
        pointPaint.setStrokeWidth(1);
    }

    @Override
    protected void onSizeChanged(int width, int h, int oldw, int oldh) {
        this.mViewWidth = width;
        this.mViewHeight = h;
        amplitudes = new float[this.mViewWidth * 2]; // xy for each point across the width
        vectors = new float[this.mViewWidth * 4]; // xxyy for each line across the width
    }

    /**
     * 음성 데시벨 그래프 draw
     */
    public void addAmplitude(int amplitude) {
        invalidate();
        // 값이 클수록 1과의 거리가 멀어지는 연산법
        float percent = ((float) (MAX_AMPLITUDE - amplitude) / MAX_AMPLITUDE);

        int ampIdx = insertIdx * 2;
        amplitudes[ampIdx++] = insertIdx;   // x
        amplitudes[ampIdx] = (mViewHeight / 2);  // y
        int vectorIdx = insertIdx * 4;
        percent = (float) ((float) Math.floor(percent * 100) / 100.0);

//        DEBUG.d("amplitude : " + amplitude);
//        DEBUG.d("percent  : " + percent);
        vectors[vectorIdx++] = insertIdx;   // x0 상향 라인
        vectors[vectorIdx++] = (mViewHeight / 2) * percent; // y0 중간점
        vectors[vectorIdx++] = insertIdx;   // x1 하향 라인
        vectors[vectorIdx] = (mViewHeight / 2) / percent;  // y1 중간점
        // insert index must be shorter than screen width
        insertIdx = ++insertIdx >= mViewWidth ? 0 : insertIdx;
    }

    // 그래프 Canvas 초기화
    public void initGraph() {
        insertIdx = 0;
        amplitudes = new float[this.mViewWidth * 2]; // xy for each point across the width
        vectors = new float[this.mViewWidth * 4]; // xxyy for each line across the width
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawLines(vectors, linePaint);
        canvas.drawPoints(amplitudes, linePaint);
    }
}
