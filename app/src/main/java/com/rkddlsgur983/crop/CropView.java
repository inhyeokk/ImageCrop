package com.rkddlsgur983.crop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CropView extends AppCompatImageView {

    private final String TAG = "CROP_VIEW";

    private Paint paint;

    private Point[] points;
    private Point start;
    private Point offset;

    private int minimumSize;
    private int initWidth;
    private int initHeight;
    private int cropWidth, cropHeight;
    private int halfCorner;
    private int cornerColor;
    private int edgeColor;
    private int outsideColor;
    private int corner = 5;

    private Drawable[] drawables;

    private Context context;

    public CropView(Context context) {
        super(context);
        this.context = context;
        init(null);
    }

    public CropView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs);
    }

    public CropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs){

        paint = new Paint();
        start = new Point();
        offset = new Point();

        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CropView, 0, 0);

        initWidth = ta.getDimensionPixelSize(R.styleable.CropView_initWidth, 20);
        initHeight = ta.getDimensionPixelSize(R.styleable.CropView_initHeight, 20);
        cropWidth = initWidth;
        cropHeight = initHeight;
        halfCorner = (ta.getDimensionPixelSize(R.styleable.CropView_cornerSize, 20))/2;
        minimumSize = ta.getDimensionPixelSize(R.styleable.CropView_minimumSize, 100);

        cornerColor = ta.getColor(R.styleable.CropView_cornerColor, Color.BLACK);
        edgeColor = ta.getColor(R.styleable.CropView_edgeColor, Color.WHITE);
        outsideColor = ta.getColor(R.styleable.CropView_outsideCropColor, Color.parseColor("#00000088"));

        points = new Point[4];
        points[0] = new Point(0, 0);
        points[1] = new Point(initWidth, 0);
        points[2] = new Point(0, initHeight);
        points[3] = new Point(initWidth, initHeight);

        drawables = new Drawable[4];
        for (int i = 0; i < 4; i++) {
            drawables[i] = ta.getDrawable(R.styleable.CropView_cornerDrawable);
            drawables[i].setTint(cornerColor);
        }
        ta.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(edgeColor);
        paint.setStrokeWidth(4);

        //crop rectangle
        canvas.drawRect(points[0].x + halfCorner,points[0].y + halfCorner, points[0].x + halfCorner + cropWidth, points[0].y + halfCorner + cropHeight, paint);

        //set paint to draw outside color, fill
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(outsideColor);

        //top rectangle
        canvas.drawRect(halfCorner, halfCorner, getWidth() - halfCorner, halfCorner + points[0].y, paint);
        //left rectangle
        canvas.drawRect(halfCorner, halfCorner + points[0].y, halfCorner + points[0].x, halfCorner + points[2].y, paint);
        //right rectangle
        canvas.drawRect(halfCorner + points[1].x, halfCorner + points[1].y, getWidth() - halfCorner, halfCorner + points[3].y, paint);
        //bottom rectangle
        canvas.drawRect(halfCorner, halfCorner + points[2].y, getWidth() - halfCorner, getHeight() - halfCorner, paint);

        for (int i = 0; i < 4; i++){
            drawables[i].setBounds(points[i].x, points[i].y, points[i].x + halfCorner * 2, points[i].y + halfCorner * 2); //set bounds of drawables
            drawables[i].draw(canvas); //place corner drawables
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:

                start.x = (int)event.getX();
                start.y = (int)event.getY();

                corner = getCorner(start.x, start.y);

                //get the offset of touch(x,y) from corner top-left point
                offset = getOffset(start.x, start.y, corner);

                //account for touch offset in starting point
                start.x = start.x - offset.x;
                start.y = start.y - offset.y;

                break;
            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_MOVE:
                if (corner == 0) {
                    cropWidth = ((int)Math.floor(event.getX() - offset.x) >= 0) ? Math.max(minimumSize, points[3].x - (int)Math.floor(event.getX() - offset.x)) : points[3].x;
                    cropHeight = ((int)Math.floor(event.getY() - offset.y) >= 0) ? Math.max(minimumSize, points[3].y - (int)Math.floor(event.getY() - offset.y)) : points[3].y;
                    points[2].x = points[3].x - cropWidth;
                    points[0].x = points[3].x - cropWidth;
                    points[0].y = points[3].y - cropHeight;
                    points[1].y = points[3].y - cropHeight;

                    start.x = points[0].x;
                    start.y = points[0].y;
                    invalidate();
                } else if (corner == 1){
                    cropWidth = Math.min(Math.max(minimumSize, (int)(cropWidth + Math.floor(event.getX()) - start.x - offset.x)), cropWidth + getWidth() - points[1].x - 2 * halfCorner);
                    cropHeight = ((int)Math.floor(event.getY() - offset.y) >= 0) ? Math.max(minimumSize, points[2].y - (int)Math.floor(event.getY() - offset.y)) : points[2].y;

                    points[3].x = points[2].x + cropWidth;
                    points[1].x = points[2].x + cropWidth;
                    points[1].y = points[2].y - cropHeight;
                    points[0].y = points[2].y - cropHeight;

                    start.x = points[1].x;
                    start.y = points[1].y;
                    invalidate();
                } else if (corner == 2){
                    cropWidth = ((int)Math.floor(event.getX()- offset.x) >= 0) ? Math.max(minimumSize, points[1].x - (int)Math.floor(event.getX() - offset.x)) : points[1].x;
                    cropHeight = Math.min(Math.max(minimumSize, (int)(cropHeight + Math.floor(event.getY()) - start.y - offset.y)), cropHeight + getHeight() - points[2].y - 2 * halfCorner);

                    points[0].x = points[1].x - cropWidth;
                    points[2].x = points[1].x - cropWidth;
                    points[2].y = points[1].y + cropHeight;
                    points[3].y = points[1].y + cropHeight;

                    start.x = points[2].x;
                    start.y = points[2].y;
                    invalidate();
                } else if (corner == 3){
                    cropWidth = Math.min(Math.max(minimumSize, (int)(cropWidth + Math.floor(event.getX()) - start.x - offset.x)), cropWidth + getWidth() - points[3].x - 2 * halfCorner);
                    cropHeight = Math.min(Math.max(minimumSize, (int)(cropHeight + Math.floor(event.getY()) - start.y - offset.y)), cropHeight + getHeight() - points[3].y - 2 * halfCorner);

                    points[1].x = points[0].x + cropWidth;
                    points[3].x = points[0].x + cropWidth;
                    points[3].y = points[0].y + cropHeight;
                    points[2].y = points[0].y + cropHeight;

                    start.x = points[3].x;
                    start.y = points[3].y;
                    invalidate();
                } else {
                    points[0].x = Math.max(points[0].x + (int)Math.min(Math.floor(event.getX() - start.x - offset.x), Math.floor(getWidth() - points[0].x - 2 * halfCorner - cropWidth)), 0);
                    points[1].x = Math.max(points[1].x + (int)Math.min(Math.floor(event.getX() - start.x - offset.x), Math.floor(getWidth() - points[1].x - 2 * halfCorner)), cropWidth);
                    points[2].x = Math.max(points[2].x + (int)Math.min(Math.floor(event.getX() - start.x - offset.x), Math.floor(getWidth() - points[2].x - 2 * halfCorner - cropWidth)), 0);
                    points[3].x = Math.max(points[3].x + (int)Math.min(Math.floor(event.getX() - start.x - offset.x), Math.floor(getWidth() - points[3].x - 2 * halfCorner)), cropWidth);

                    points[0].y = Math.max(points[0].y + (int)Math.min(Math.floor(event.getY() - start.y - offset.y), Math.floor(getHeight() - points[0].y - 2 * halfCorner - cropHeight)), 0);
                    points[1].y = Math.max(points[1].y + (int)Math.min(Math.floor(event.getY() - start.y - offset.y), Math.floor(getHeight() - points[1].y - 2 * halfCorner - cropHeight)), 0);
                    points[2].y = Math.max(points[2].y + (int)Math.min(Math.floor(event.getY() - start.y - offset.y), Math.floor(getHeight() - points[2].y - 2 * halfCorner)), cropHeight);
                    points[3].y = Math.max(points[3].y + (int)Math.min(Math.floor(event.getY() - start.y - offset.y), Math.floor(getHeight() - points[3].y - 2 * halfCorner)), cropHeight);

                    start.x = (int)Math.floor(event.getX());
                    start.y = (int)Math.floor(event.getY());
                    invalidate();
                }
                break;
        }
        return true;
    }

    private int getCorner(float x, float y) {

        int corner = 5;
        for (int i = 0; i < points.length; i++) {
            float dx = x - points[i].x;
            float dy = y - points[i].y;
            int max = halfCorner * 2;
            if (dx <= max && dx >= 0 && dy <= max && dy >= 0){
                return i;
            }
        }
        return corner;
    }

    private Point getOffset(int left, int top, int corner) {

        Point offset = new Point();
        if (corner == 5) {
            offset.x = 0;
            offset.y = 0;
        } else {
            offset.x = left - points[corner].x;
            offset.y = top - points[corner].y;
        }
        return offset;
    }

    public int getCropX() {
        return points[0].x + (int)Math.floor(getWidth() * Camera.PADDING_CROP_X_RATE);
    }

    public int getCropY() {
        return points[0].y + (int)Math.floor(getHeight() * Camera.PADDING_CROP_Y_RATE);
    }

    public int getCropWidth() {
        return cropWidth;
    }

    public int getCropHeight() {
        return cropHeight;
    }
}