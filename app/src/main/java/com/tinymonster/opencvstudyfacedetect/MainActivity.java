package com.tinymonster.opencvstudyfacedetect;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private JavaCameraView Main_surface_view;
    private static final String TAG="MainActivity";
    private File mCascadeFile;
    private CascadeClassifier haarCascade;
    private Mat mRgba;
    private boolean mIsFrontCamera=false;
    private Button Main_bt_back;
    private Button Main_bt_front;
    int size=0;
//    private Mat TempRGB;
//    private Mat TempGray;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("MainActivity","1");
        setContentView(R.layout.activity_main);
        Log.e("MainActivity","2");
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            Log.e("MainActivity,请求权限"," ");
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},2);
        }else {
            Log.e("MainActivity,跳转到相机"," ");
            initView();
            Log.e("MainActivity","3");
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, MainActivity.this, mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }
    private void initView(){
        Main_surface_view=(JavaCameraView)findViewById(R.id.Main_surface_view);
        Main_bt_back=(Button)findViewById(R.id.Main_bt_back);
        Main_bt_front=(Button)findViewById(R.id.Main_bt_front);
        Main_bt_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main_surface_view.setVisibility(SurfaceView.GONE);
                mIsFrontCamera=false;
                Main_surface_view.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                Main_surface_view.setVisibility(SurfaceView.VISIBLE);
                Main_surface_view.setCvCameraViewListener(MainActivity.this);
                Main_surface_view.enableView();
            }
        });
        Main_bt_front.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main_surface_view.setVisibility(SurfaceView.GONE);
                mIsFrontCamera=true;
                Main_surface_view.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                Main_surface_view.setVisibility(SurfaceView.VISIBLE);
                Main_surface_view.setCvCameraViewListener(MainActivity.this);
                Main_surface_view.enableView();
            }
        });
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("MainActivity", "OpenCV loaded successfully");
                    try{
                        InputStream is=getResources().openRawResource(R.raw.lbpcascade_frontalface);//读取级联分类器
                        File cascadeDir=getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile=new File(cascadeDir,"cascade.xml");//创建输出文件
                        FileOutputStream os=new FileOutputStream(mCascadeFile);
                        byte[] buffer=new byte[4096];//缓存
                        int bytesRead;
                        while ((bytesRead=is.read(buffer))!=-1){//直到文件末尾
                            os.write(buffer,0,bytesRead);//写入文件
                        }
                        is.close();
                        os.close();
                        haarCascade=new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if(haarCascade.empty()){
                            Toast.makeText(MainActivity.this,"加载级联分类器失败",Toast.LENGTH_SHORT).show();
                            haarCascade=null;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        Log.e("MainActivity","未找到级联分类器");
                    }
//                    Main_surface_view.enableView();
//                    Main_surface_view.setCameraIndex(1);
//                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        size=(int)(height*0.3);
        Log.e("MainActivity","size:"+size);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mGray=inputFrame.gray();
        mRgba=inputFrame.rgba();
        if(mIsFrontCamera){
            Core.flip(mRgba,mRgba,1);//翻转图片
            Core.flip(mGray,mGray,1);//翻转图片
        }
        MatOfRect faces=new MatOfRect();
        if(haarCascade!=null){
            haarCascade.detectMultiScale(mGray,faces,1.1,2,2,new Size(size,size),new Size());
        }
            Rect[] facesArray=faces.toArray();
            for(int i=0;i<facesArray.length;i++){
                Log.e("MainActivity",facesArray[i].tl()+" "+facesArray[i].br());
                Core.rectangle(mRgba,facesArray[i].tl(),facesArray[i].br(),new Scalar(0,255,0,255),3);
            }
        return mRgba;
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    protected void onPause() {
        super.onPause();
        Main_surface_view.disableView();
    }
    

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(Main_surface_view!=null){
            Main_surface_view.disableView();
        }
    }
    /*
返回
 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 2:
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED
                        ||ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
                    Log.e("请求权限完成，跳转"," ");
                    initView();
                    if (!OpenCVLoader.initDebug()) {
                        Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
                    } else {
                        Log.d(TAG, "OpenCV library found inside package. Using it!");
                        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                    }
                }else {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},2);
                    Log.e("再次请求权限"," ");
                }
                break;
        }
    }
}
