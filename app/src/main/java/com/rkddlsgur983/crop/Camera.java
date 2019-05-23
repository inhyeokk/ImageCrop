package com.rkddlsgur983.crop;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Camera {

    private static final String TAG = "CAMERA";

    public static final String PERMISSION_GRANTED_MSG = "PERMISSION_GRANTED";
    public static final String PERMISSION_DENIED_MSG = "PERMISSION_DENIED";

    public static final String EXTRA_NAME_PHOTO_PATH = "PHOTO_PATH";
    public static final String EXTRA_NAME_CROPPED_PHOTO_PATH = "CROPPED_PHOTO_PATH";

    public static final int MY_PERMISSION_CAMERA = 1001;
    public static final int REQUEST_TAKE_PHOTO = 1002;
    public static final int REQUEST_TAKE_ALBUM = 1003;
    public static final int REQUEST_CROP_PHOTO = 1004;

    public static final double PADDING_START_X_RATE = 0.085;
    public static final double PADDING_START_Y_RATE = 0.0475;
    public static final double PADDING_CROP_X_RATE = 0.0725;
    public static final double PADDING_CROP_Y_RATE = 0.04;

    public static final File STORAGE_DIR = new File(Environment.getExternalStorageDirectory() + "/Pictures", "img");

    private Activity activity;
    private String currentPhotoPath;

    public Camera(Activity activity) {
        this.activity = activity;
    }

    public boolean checkCameraHardware() {

        if (activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        } else {
            activity.finish();
            return false;
        }
    }

    public void checkPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ) {
                Log.d(TAG, PERMISSION_GRANTED_MSG);
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                Log.d(TAG, PERMISSION_DENIED_MSG);
            }
        }
    }

    public void captureCamera() {

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)){
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createPhotoFile();
                } catch (IOException e ){
                    Log.e(TAG, e.getMessage());
                }
                if (photoFile != null) {
                    Uri providerURI = FileProvider.getUriForFile(activity, activity.getPackageName(), photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI);
                    activity.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        }
    }

    private File createPhotoFile() throws IOException {

        File storageDir = STORAGE_DIR;

        if(!storageDir.exists()){
            storageDir.mkdirs();
        }
        File imageFile = File.createTempFile(getCurrentTime(), ".jpg", storageDir);
        imageFile.deleteOnExit();
        currentPhotoPath = imageFile.getAbsolutePath();

        return imageFile;
    }

    public static String createPhotoFileByBitmap(Bitmap bitmap) throws  IOException {

        File storageDir = STORAGE_DIR;

        if(!storageDir.exists()){
            storageDir.mkdirs();
        }
        File imageFile = File.createTempFile(getCurrentTime(), ".jpg", storageDir);
        OutputStream out = new FileOutputStream(imageFile);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        return imageFile.getAbsolutePath();
    }

    private static String getCurrentTime() {

        Long date = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date resultDate = new Date(date);
        String result = dateFormat.format(resultDate);

        return result;
    }

    public static Bitmap getBitmapOfCapturedPhoto(String path) {

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int exifOrientation;

        if (exif != null) {
            exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            bitmap = exifOrientationToDegrees(bitmap, exifOrientation);
        }
        return bitmap;
    }

    private static Bitmap exifOrientationToDegrees(Bitmap origin, int exifOrientation) {

        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return rotate(origin, 90);
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return rotate(origin, 180);
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return rotate(origin, 270);
        }
        return origin;
    }

    private static Bitmap rotate(Bitmap src, float degree) {

        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(),
                src.getHeight(), matrix, true);
    }

    public static Bitmap setPaddingToBitmap(Bitmap bitmap) {

        Bitmap outputImage = Bitmap.createBitmap(
                (int)Math.floor(bitmap.getWidth() * (1 + PADDING_START_X_RATE * 2)),    // width
                (int)Math.floor(bitmap.getHeight() * (1 + PADDING_START_Y_RATE * 2)),   // height
                Bitmap.Config.ARGB_8888
        );
        Canvas can = new Canvas(outputImage);
        can.drawBitmap(bitmap,
                (int)Math.floor(bitmap.getWidth() * PADDING_START_X_RATE),
                (int)Math.floor(bitmap.getHeight() * PADDING_START_Y_RATE),
                null
        );
        return outputImage;
    }

    public void cropPhoto() {

        Intent intent = new Intent(activity, CropActivity.class);
        intent.putExtra(EXTRA_NAME_PHOTO_PATH, currentPhotoPath);
        activity.startActivityForResult(intent, REQUEST_CROP_PHOTO);
    }

    public void getAlbum() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        activity.startActivityForResult(intent, REQUEST_TAKE_ALBUM);
    }

    public Uri galleryAddPhoto(String path) {

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(path);
        Uri contentURI = Uri.fromFile(f);
        mediaScanIntent.setData(contentURI);
        activity.sendBroadcast(mediaScanIntent);

        return contentURI;
    }

    public static boolean deleteTempFiles() {

        if (STORAGE_DIR.isDirectory()) {
            File[] files = STORAGE_DIR.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteTempFiles();
                    } else {
                        f.delete();
                    }
                }
            }
        }
        return STORAGE_DIR.delete();
    }
}
