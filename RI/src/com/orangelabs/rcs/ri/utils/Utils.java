/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.ri.utils;

import com.orangelabs.rcs.ri.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.Set;

/**
 * Utility functions
 * 
 * @author Jean-Marc AUFFRET
 */
public class Utils {

    private static final Random sPendingIntentIdGenerator = new Random();

    /**
     * Gets a unique ID for pending intent
     * 
     * @return unique ID for pending intent
     */
    public static int getUniqueIdForPendingIntent() {
        return sPendingIntentIdGenerator.nextInt();
    }

    private static final String LOGTAG = LogUtils.getTag(Utils.class.getSimpleName());

    /**
     * Returns the application version from manifest file
     * 
     * @param ctx Context
     * @return Application version or null if not found
     */
    public static String getApplicationVersion(Context ctx) {
        String version = null;
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            version = info.versionName;
        } catch (NameNotFoundException e) {
        }
        return version;
    }

    /**
     * Display a toast
     * 
     * @param ctx Context
     * @param message Message to be displayed
     */
    public static void displayToast(Context ctx, String message) {
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Display a long toast
     * 
     * @param ctx Context
     * @param message Message to be displayed
     */
    public static void displayLongToast(Context ctx, String message) {
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Display toast and log an exception
     * 
     * @param context Context of activity
     * @param e exception to log
     */
    public static void displayToast(Context context, Exception e) {
        displayToast(context, "Exception occurred", e);
    }

    /**
     * Display toast and log an exception
     * 
     * @param context Context of activity
     * @param message message to display
     * @param e exception to log
     */
    public static void displayToast(Context context, String message, Exception e) {
        Log.i(LOGTAG, message, e);
        displayLongToast(context, message.concat(": see Logcat!"));
    }

    /**
     * Show a message and exit activity
     * 
     * @param activity Activity
     * @param msg Message to be displayed
     */
    public static void showMessageAndExit(final Activity activity, String msg) {
        showMessageAndExit(activity, msg, null, null);
    }

    /**
     * Show a message and exit activity
     * 
     * @param activity the activity.
     * @param msg the message
     * @param locker the locker
     */
    public static void showMessageAndExit(final Activity activity, String msg, LockAccess locker) {
        showMessageAndExit(activity, msg, locker, null);
    }

    /**
     * Show a message and exit activity
     * 
     * @param activity Activity
     * @param msg Message to be displayed
     * @param locker a locker to only execute once
     * @param e the exception
     */
    public static void showMessageAndExit(final Activity activity, String msg, LockAccess locker,
            Exception e) {
        // Do not execute if activity is Fishing
        if (activity.isFinishing()) {
            return;
        }
        // Do not execute if already executed once
        if (locker != null && !locker.tryLock()) {
            return;
        }

        if (e != null) {
            Log.e(LOGTAG, "Exception enforces exit of activity!", e);
        } else {
            if (LogUtils.isActive) {
                Log.w(LOGTAG,
                        new StringBuilder("Exit activity ").append(activity.getLocalClassName())
                                .append(" <").append(msg).append(">").toString());
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(msg);
        builder.setTitle(R.string.title_msg);
        builder.setCancelable(false);
        builder.setPositiveButton(activity.getString(R.string.label_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Show an message
     * 
     * @param activity Activity
     * @param msg Message to be displayed
     * @return Dialog
     */
    public static AlertDialog showMessage(Activity activity, String msg) {
        if (LogUtils.isActive) {
            Log.w(LOGTAG, "Activity " + activity.getLocalClassName() + " message=" + msg);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(msg);
        builder.setTitle(R.string.title_msg);
        builder.setCancelable(false);
        builder.setPositiveButton(activity.getString(R.string.label_ok), null);
        AlertDialog alert = builder.create();
        alert.show();
        return alert;
    }

    /**
     * Show a picture and exit activity
     * 
     * @param activity Activity
     * @param uri Picture to be displayed
     */
    public static void showPictureAndExit(final Activity activity, Uri uri) {
        if (activity.isFinishing()) {
            return;
        }
        try {
            String filename = FileUtils.getFileName(activity, uri);
            Toast.makeText(activity, activity.getString(R.string.label_receive_image, filename),
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/*");
            activity.startActivity(intent);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "showPictureAndExit", e);
            }
        }
    }

    /**
     * Show an info with a specific title
     * 
     * @param activity Activity
     * @param title Title of the dialog
     * @param items List of items
     */
    public static void showList(Activity activity, String title, Set<String> items) {
        if (activity.isFinishing()) {
            return;
        }

        CharSequence[] chars = items.toArray(new CharSequence[items.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setPositiveButton(activity.getString(R.string.label_ok), null);
        builder.setItems(chars, null);
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Show a progress dialog with the given parameters
     * 
     * @param activity Activity
     * @param msg Message to be displayed
     * @return Dialog
     */
    public static ProgressDialog showProgressDialog(Activity activity, String msg) {
        ProgressDialog dlg = new ProgressDialog(activity);
        dlg.setMessage(msg);
        dlg.setIndeterminate(true);
        dlg.setCancelable(true);
        dlg.setCanceledOnTouchOutside(false);
        dlg.show();
        return dlg;
    }

    /**
     * Format a date to string
     * 
     * @param d Date
     * @return String
     */
    public static String formatDateToString(long d) {
        if (d > 0L) {
            Date df = new Date(d);
            return DateFormat.getDateInstance().format(df);
        } else {
            return "";
        }
    }

    /**
     * Construct an NTP time from a date in milliseconds
     * 
     * @param date Date in milliseconds
     * @return NTP time in string format
     */
    public static String constructNTPtime(long date) {
        long ntpTime = 2208988800L;
        long startTime = (date / 1000) + ntpTime;
        return String.valueOf(startTime);
    }

    /**
     * Returns the local IP address
     * 
     * @return IP address
     */
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration<InetAddress> addr = intf.getInetAddresses(); addr
                        .hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) addr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the label to show the progress status
     * 
     * @param currentSize Current size in bytes transferred
     * @param totalSize Total size in bytes to be transferred
     * @return progress label
     */
    public static String getProgressLabel(long currentSize, long totalSize) {
        StringBuilder value = new StringBuilder();
        value.append(currentSize / 1024);
        if (totalSize != 0) {
            value.append('/');
            value.append(totalSize / 1024);
        }
        value.append(" Kb");
        return value.toString();
    }

}
