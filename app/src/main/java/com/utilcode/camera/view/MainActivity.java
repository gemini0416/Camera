package com.utilcode.camera.view;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.utilcode.camera.R;
import com.utilcode.camera.ifs.FrameCallback;
import com.utilcode.camera.util.PermissionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements FrameCallback {
    private AppCompatSeekBar mSeek;
    private CameraView mCameraView;
    private int num = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA, Manifest
                .permission.WRITE_EXTERNAL_STORAGE}, 10, initViewRunnable);
    }

    private Runnable initViewRunnable = new Runnable() {
        @Override
        public void run() {
            setContentView(R.layout.activity_main);

            mSeek=findViewById(R.id.mSeek);
            mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mCameraView.setBeautyFilterProgress(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            mCameraView= findViewById(R.id.mCameraView);
            Toolbar mToolbar = findViewById(R.id.toolbar);
            mToolbar.setTitle("");
            setSupportActionBar(mToolbar);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.mSwitchCamera){
                        mSeek.setVisibility(View.GONE);
                        mCameraView.switchCamera();
                    }
                    return false;
                }
            });
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(requestCode == 10, grantResults, initViewRunnable,
                new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.permission), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    public void onClick(View view){
        switch (view.getId()){
            case R.id.mGallery:
                mSeek.setVisibility(View.GONE);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType("image/*");
                startActivity(intent);
                break;
            case R.id.mShutter:
                mSeek.setVisibility(View.GONE);
                mCameraView.takePhoto();
                break;
            case R.id.mFilter:
                if (mSeek.getVisibility() == View.GONE) {
                    mSeek.setVisibility(View.VISIBLE);
                    break;
                }
                if (++num == mCameraView.getImageFilterCount()){
                    num = 0;
                }
                mCameraView.onSelectImage(num);
                break;
            case R.id.mBeauty:
                mSeek.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.camera, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mCameraView!=null) {
            mCameraView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCameraView!=null) {
            mCameraView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCameraView!=null) {
            mCameraView.onDestroy();
        }
    }

    @Override
    public void onFrame(final byte[] bytes, long time) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap=Bitmap.createBitmap(720,1280, Bitmap.Config.ARGB_8888);
                ByteBuffer b=ByteBuffer.wrap(bytes);
                bitmap.copyPixelsFromBuffer(b);
                saveBitmap(bitmap);
                bitmap.recycle();
            }
        }).start();
    }

    protected String getSD(){
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    //图片保存
    public void saveBitmap(Bitmap b) {
        String storePath = getSD() + File.separator + "camera";
        File appDir = new File(storePath);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        final String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            b.compress(Bitmap.CompressFormat.JPEG, 60, fos);
            fos.flush();
            fos.close();

            Uri uri = Uri.fromFile(file);
            getBaseContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.success)+"->"+fileName, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.failed)+"->"+fileName, Toast.LENGTH_SHORT).show();
                }
            });
            e.printStackTrace();
        }
    }
}
