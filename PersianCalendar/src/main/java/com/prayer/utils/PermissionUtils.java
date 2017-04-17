/*
 * Copyright (c) 2016 Metin Kale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prayer.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.byagowi.persiancalendar.R;;
import com.prayer.settings.Prefs;

/**
 * Created by metin on 14.12.2015.
 */
public class PermissionUtils {
    private static final int REQUEST_CALENDAR = 1;
    private static final int REQUEST_CAMERA = 2;
    private static final int REQUEST_STORAGE = 3;
    private static final int REQUEST_LOCATION = 4;
    private static final int REQUEST_NOT_POLICY = 5;

    private static PermissionUtils mInstance;
    public boolean pCalendar;
    public boolean pCamera;
    public boolean pStorage;
    public boolean pLocation;
    public boolean pNotPolicy;

    private PermissionUtils(Context c) {
        checkPermissions(c);
    }

    public static PermissionUtils get(Context c) {
        if (mInstance == null) {
            mInstance = new PermissionUtils(c);
        }
        return mInstance;
    }

    private void checkPermissions(Context c) {
        pCalendar = ContextCompat.checkSelfPermission(c, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
        pCamera = ContextCompat.checkSelfPermission(c, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        pStorage = ContextCompat.checkSelfPermission(c, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        pLocation = ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            pNotPolicy = nm.isNotificationPolicyAccessGranted();
        } else
            pNotPolicy = true;
    }

    public void needNotificationPolicy(final Activity act) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        NotificationManager nm = (NotificationManager) act.getSystemService(Context.NOTIFICATION_SERVICE);
        pNotPolicy = nm.isNotificationPolicyAccessGranted();
        if (!pNotPolicy) {
            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            act.startActivity(intent);
        }
    }

    public void needCamera(final Activity act) {
        if (!pCamera) {


            AlertDialog.Builder builder = new AlertDialog.Builder(act);

            builder.setTitle(R.string.permissionCameraTitle).setMessage(R.string.permissionCameraText).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                }
            });

            builder.show();

        }
    }


    public void needLocation(final Activity act) {
        if (!pLocation) {

            AlertDialog.Builder builder = new AlertDialog.Builder(act);

            builder.setTitle(R.string.permissionLocationTitle).setMessage(R.string.permissionLocationText).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                }
            });


            builder.show();

        }
    }


    public void needCalendar(final Activity act, boolean force) {
        if (!pCalendar && (!"-1".equals(Prefs.getCalendar()) || force)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(act);

            builder.setTitle(R.string.permissionCalendarTitle).setMessage(R.string.permissionCalendarText).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.WRITE_CALENDAR}, REQUEST_CALENDAR);
                }
            });


            builder.show();
        }

    }


    public void needStorage(final Activity act) {
        if (!pStorage) {

            AlertDialog.Builder builder = new AlertDialog.Builder(act);

            builder.setTitle(R.string.permissionStorageTitle).setMessage(R.string.permissionStorageText).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
                }
            });


            builder.show();

        }
    }


    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    pCamera = true;
                } else {
                    pCamera = false;
                }
                return;

            case REQUEST_CALENDAR:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    pCalendar = true;
                } else {
                    pCalendar = false;
                }
                return;

            case REQUEST_NOT_POLICY:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    pNotPolicy = true;
                } else {
                    pNotPolicy = false;
                }
                return;

            case REQUEST_LOCATION:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    pLocation = true;
                } else {
                    pLocation = false;
                    Prefs.setCalendar("-1");
                }
                return;

            case REQUEST_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    pStorage = true;
                } else {
                    pStorage = false;
                }
                return;


        }
    }
}
