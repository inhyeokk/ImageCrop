package com.rkddlsgur983.crop;

import android.content.Intent;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MAIN_ACTIVITY";

    private Camera camera;

    private ImageView ivMain;
    private Button btnCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = new Camera(this);
        camera.checkCameraHardware();
        camera.checkPermission();

        ivMain = findViewById(R.id.iv_main);

        btnCamera = findViewById(R.id.btn_camera);
        btnCamera.setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == btnCamera.getId()) {
                camera.captureCamera();
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case Camera.MY_PERMISSION_CAMERA:
                for (int result: grantResults){ // granted: 0 denied: -1
                    if (result < 0){
                        Toast.makeText(this, Camera.PERMISSION_DENIED_MSG, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case Camera.REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    camera.cropPhoto();
                }
                break;
            case Camera.REQUEST_CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    String path = "";
                    try {
                        path = intent.getStringExtra(Camera.EXTRA_NAME_CROPPED_PHOTO_PATH);
                    } catch (NullPointerException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    ivMain.setImageBitmap(Camera.getBitmapOfCapturedPhoto(path));
                } else if (resultCode == RESULT_CANCELED){
                    // do nothing
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            newConfig.orientation = Configuration.ORIENTATION_PORTRAIT;
    }
}
