/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2basic;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class CameraActivity extends AppCompatActivity {
    //定义两个fragment
    private Camera2BasicFragment fb;
//    private Camera2VideoFragment fv;
    //定义一个按钮
    private Button button;
    private static final String[] VIDEO_PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int VIDEO_PERMISSIONS_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
//        button = (Button) findViewById(R.id.button);
//        button.setOnClickListener((View.OnClickListener) this);
//
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit();
        }
//    }
//    private void initFragment1(){
//        //开启事务，fragment的控制是由事务来实现的
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//
//        if(fb == null){
//            fb = new Camera2BasicFragment();
//
//        }
//        //隐藏所有fragment
//        hideFragment(transaction);
//        //显示需要显示的fragment
//        transaction.show(fb);
//        transaction.commit();
//    }
//    private void initFragment2(){
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//
//        if(fv == null){
//            fv = new Camera2VideoFragment();
//            transaction.add(R.id.container, fv);
//        }
//        hideFragment(transaction);
//        transaction.show(fv);
//
//        transaction.commit();
//    }
//    private void hideFragment(FragmentTransaction transaction){
//        if(fb != null){
//            transaction.hide(fb);
//        }
//        if(fv != null){
//            transaction.hide(fv);
//        }
//
//    }
//
//    public void onClick(View v) {//点击哪个按钮就显示哪个fragment;
//        if(v == button){
//            initFragment1();
//        }else if(){
//            initFragment2();
//        }
//    }
    }
    private void requestPermission() {
        // 当API大于 23 时，才动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, VIDEO_PERMISSIONS, VIDEO_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case VIDEO_PERMISSIONS_CODE:
                //权限请求失败
                if (grantResults.length == VIDEO_PERMISSIONS.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            //弹出对话框引导用户去设置
                            showDialog();
                            Toast.makeText(this, "请求权限被拒绝", Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                } else {
                    Toast.makeText(this, "已授权", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    //弹出提示框
    private void showDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("录像需要相机、录音和读写权限，是否去设置？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        goToAppSetting();
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}
