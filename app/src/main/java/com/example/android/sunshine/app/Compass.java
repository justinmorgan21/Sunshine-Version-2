package com.example.android.sunshine.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * Created by jmorgan on 9/15/2016.
 */
public class Compass extends View {
    private Paint mTextPaint;
    private Paint mCirclePaint;
    private Paint mLinePaint;

    private float mViewWidth;
    private float mViewHeight;

    private float mDirectionRadians;

    private GestureDetector mDetector;

    private Matrix matrix;
    private int framePerSeconds = 100;
    private long animationDuration = 10000;
    private long startTime;
    private boolean restart;
    private boolean animate;

    // when view created through code
    public Compass(Context context) {
        super(context);
        init();
    }

    // when view created from resource
    public Compass(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // when view created through inflation
    public Compass(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        init();
    }


    private void init() {
        mDirectionRadians = 0.0F;

        mViewWidth = 200;
        mViewHeight = 200;

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.BLUE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(36);

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(Color.BLACK);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(6);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setColor(Color.RED);
        mLinePaint.setStrokeWidth(6);

        matrix = new Matrix();
        startTime = System.currentTimeMillis();
        restart = true;
        animate = false;

        mDetector = new GestureDetector(Compass.this.getContext(), new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return true;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
            }

            @Override
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                animate = true;
                return false;
            }
        });
    }

    @Override
    protected void onMeasure(int wMeasureSpec, int hMeasureSpec) {
        super.onMeasure(wMeasureSpec, hMeasureSpec);
        setMeasuredDimension((int)mViewWidth, (int)mViewHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw outer circle
        float centerX = mViewWidth/2;
        float centerY = mViewHeight/2;
        canvas.drawCircle(centerX, centerY, mViewHeight/2 - 10, mCirclePaint);

        // Draw letters for directions
        int textWidth = (int) mTextPaint.measureText("N");
        float directionSpacing = 6;
        canvas.drawText("N", centerX, (2 * textWidth), mTextPaint);
        canvas.drawText("E", mViewWidth - textWidth - directionSpacing, centerY + (textWidth / 2), mTextPaint);
        canvas.drawText("S", centerX, mViewHeight - (textWidth / 2) - directionSpacing, mTextPaint);
        canvas.drawText("W", textWidth + directionSpacing, centerY + (textWidth / 2), mTextPaint);

        // Animate needle
        long elapsedTime = 0;
        if (animate) {
            if (restart) {
                startTime = System.currentTimeMillis();
                restart = false;
            }
            elapsedTime = System.currentTimeMillis() - startTime;
            matrix.postRotate(60f, 100, 100);
            canvas.concat(matrix);
        }

        float scalar = (mViewWidth / 2) - textWidth;
        float xEnd = (float)(centerX + scalar * Math.sin(mDirectionRadians));
        float yEnd = (float)(centerY - scalar * Math.cos(mDirectionRadians));
        canvas.drawLine(centerX, centerY, xEnd, yEnd, mLinePaint);

        if (animate) {
            if (elapsedTime < animationDuration)
                this.postInvalidateDelayed(10000 / framePerSeconds);
        }
    }

    public void update(float windDirDegrees) {
        mDirectionRadians = windDirDegrees * ((float) Math.PI / 180);
        AccessibilityManager accessibilityManager =
                (AccessibilityManager) getContext().getSystemService(
                        Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            sendAccessibilityEvent(
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        }
        invalidate();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent ev) {
        ev.getText().add("" + mDirectionRadians * ((float) Math.PI / 180));
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = mDetector.onTouchEvent(event);
        if (!result) {
            result = true;
        }
//        if(event.getAction() == MotionEvent.ACTION_DOWN) {
//        }
        invalidate();
        return true;
    }
}