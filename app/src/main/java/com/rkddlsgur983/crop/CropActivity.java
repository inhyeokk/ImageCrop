package com.rkddlsgur983.crop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.io.IOException;

public class CropActivity extends AppCompatActivity {

    private final String TAG = "CROP_ACTIVITY";

    private ImageView ivClose, ivCheck;
    private CropView cvMain;
    private SeekBar sbBrightness, sbContrast;

    private float brightnessVal = 0f;
    private float contrastVal = 1f;
    private final int defaultBrightness = 105;
    private final int defaultContrast = 50;
    private static final int brightnessMax = 210;
    private static final int contrastMax = 100;

    private String currentPhotoPath;
    private Bitmap origin = null;
    private Bitmap scaled = null;
    private Bitmap crop = null;

    public static final int DENSITY_MDPI = 101;
    public static final int DENSITY_HDPI = 102;
    public static final int DENSITY_XHDPI = 103;
    public static final int DENSITY_XXHDPI = 104;
    public static final int DENSITY_XXXHDPI = 105;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        ivClose = findViewById(R.id.iv_close);
        ivClose.setOnClickListener(onClickListener);
        ivCheck = findViewById(R.id.iv_check);
        ivCheck.setOnClickListener(onClickListener);
        cvMain = findViewById(R.id.cv_main);
        origin = getBitmap();
        scaled = Camera.setPaddingToBitmap(origin);
        cvMain.setImageBitmap(scaled);

        sbBrightness = findViewById(R.id.sb_brightness);
        sbBrightness.setMax(brightnessMax);
        sbBrightness.setProgress(defaultBrightness);
        sbBrightness.setOnSeekBarChangeListener(seekBarChangeListener);

        sbContrast = findViewById(R.id.sb_contrast);
        sbContrast.setMax(contrastMax);
        sbContrast.setProgress(defaultContrast);
        sbContrast.setOnSeekBarChangeListener(seekBarChangeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (crop != null && !crop.isRecycled()) {
            crop.recycle();
            crop = null;
        }
        if (scaled != null && !scaled.isRecycled()) {
            scaled.recycle();
            scaled = null;
        }
        if (origin != null && !origin.isRecycled()) {
            origin.recycle();
            origin = null;
        }
        // Camera.deleteTempFiles();
    }

    private Bitmap getBitmap() {
        currentPhotoPath = getIntent().getStringExtra(Camera.EXTRA_NAME_PHOTO_PATH);
        return Camera.getBitmapOfCapturedPhoto(currentPhotoPath);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.iv_close:
                    setResult(RESULT_CANCELED);
                    finish();
                    break;

                case R.id.iv_check:
                    Intent intent = new Intent();
                    String path;
                    if (crop != null) {
                        path = getResult(crop);
                        intent.putExtra(Camera.EXTRA_NAME_CROPPED_PHOTO_PATH, path);
                    } else {
                        path = getResult(scaled);
                        intent.putExtra(Camera.EXTRA_NAME_CROPPED_PHOTO_PATH, path);
                    }
                    setResult(RESULT_OK, intent);
                    finish();
                    break;
            }
        }
    };

    public String getResult(Bitmap bitmap) {

        String path = currentPhotoPath;
        Bitmap result = null;
        double widthRatio = (double)bitmap.getWidth() / (double)cvMain.getWidth();
        double heightRatio = (double)bitmap.getHeight() / (double)cvMain.getHeight();

        try {
            result = Bitmap.createBitmap(bitmap,
                    (int)Math.floor(widthRatio * cvMain.getCropX()),        // x
                    (int)Math.floor(heightRatio * cvMain.getCropY()),       // y
                    (int)Math.floor(widthRatio * cvMain.getCropWidth()),    // width
                    (int)Math.floor(heightRatio * cvMain.getCropHeight())   // height
            );
            path = Camera.createPhotoFileByBitmap(result);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (result != null && !result.isRecycled()) {
                result.isRecycled();
            }
        }

        return path;
    }

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            switch (seekBar.getId()) {
                case R.id.sb_brightness:
                    brightnessVal = (float) (progress - 105) / 1f;
                    break;

                case R.id.sb_contrast:
                    contrastVal = (float) (progress + 50) / 100f;
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            crop = changeBitmapBrightnessContrast(scaled, brightnessVal, contrastVal);
            cvMain.setImageBitmap(crop);
        }
    };

    /**
     *
     * @param bmp input bitmap
     * @param brightness -105..105 0 is default / origin range: -255..255
     * @param contrast 0.5..1.5 1 is default / origin range: 0..10
     * @return new bitmap
     */
    private Bitmap changeBitmapBrightnessContrast(Bitmap bmp, float brightness, float contrast) {
        ColorMatrix cm = new ColorMatrix(new float[] {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }

    public int getPixelByDensity(Bitmap bitmap, int size) {
        int result = size;
        int densityDpi = bitmap.getDensity();
        Log.d(TAG, densityDpi + "");
        switch (densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                result = (int)(size * 0.75);
                break;

            case DisplayMetrics.DENSITY_MEDIUM:
                result = size;
                break;

            case DisplayMetrics.DENSITY_TV:
            case DisplayMetrics.DENSITY_HIGH:
                result = (int)(size * 1.5);
                break;

            case DisplayMetrics.DENSITY_XHIGH:
            case DisplayMetrics.DENSITY_280:
                result = size * 2;
                break;

            case DisplayMetrics.DENSITY_XXHIGH:
            case DisplayMetrics.DENSITY_360:
            case DisplayMetrics.DENSITY_400:
            case DisplayMetrics.DENSITY_420:
                result = size * 3;
                break;

            case DisplayMetrics.DENSITY_XXXHIGH:
            case DisplayMetrics.DENSITY_560:
                result = size * 4;
                break;
        }
        return result;
    }

    public static int dpToPx(int density, int size) {
        int result = size;
        switch (density) {
            case DENSITY_MDPI:
                result = size;
                break;
            case DENSITY_HDPI:
                result = (int)(size * 1.5);
                break;
            case DENSITY_XHDPI:
                result = size * 2;
                break;
            case DENSITY_XXHDPI:
                result = size * 3;
                break;
            case DENSITY_XXXHDPI:
                result = size * 4;
                break;
        }
        return result;
    }

    public int getPixelByBitmapDensity(Bitmap bitmap, int dp) {
        double density = bitmap.getDensity() / 160f;
        return (int)density * dp;
    }
}
