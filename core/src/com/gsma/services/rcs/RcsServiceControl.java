/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.services.rcs;

import com.gsma.services.rcs.Intents.Service;
import com.gsma.services.rcs.RcsServiceControlLog.EnableRcseSwitch;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A utility class to control the activation of the RCS service.
 */
public class RcsServiceControl {

    /**
     * RCS stack package name
     */
    public final static String RCS_STACK_PACKAGENAME = "com.gsma.rcs";

    /**
     * Singleton of RcsServiceControl
     */
    private static volatile RcsServiceControl sInstance;

    private final Context mContext;

    private final String mPackageName;

    private final static String LOG_TAG = "[RCS][" + RcsServiceControl.class.getSimpleName() + "]";

    private static final String[] PROJECTION = new String[] {
        RcsServiceControlLog.VALUE
    };

    private static final String WHERE_CLAUSE = new StringBuilder(RcsServiceControlLog.KEY).append(
            "=?").toString();

    private RcsServiceControl(Context ctx) {
        mContext = ctx;
        mPackageName = mContext.getPackageName();
    }

    /**
     * Gets an instance of RcsServiceControl
     * 
     * @param ctx the context.
     * @return RcsServiceControl the singleton instance.
     */
    public static RcsServiceControl getInstance(Context ctx) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (RcsServiceControl.class) {
            if (sInstance == null) {
                if (ctx == null) {
                    throw new IllegalArgumentException("Context is null");
                }
                sInstance = new RcsServiceControl(ctx);
            }
        }
        return sInstance;
    }

    /**
     * IntentUtils class sets appropriate flags to an intent using reflection
     */
    static class IntentUtils {

        private static final int HONEYCOMB_MR1_VERSION_CODE = 12;

        private static final int JELLY_BEAN_VERSION_CODE = 16;

        private static final String ADD_FLAGS_METHOD_NAME = "addFlags";

        private static final Class<?>[] ADD_FLAGS_PARAM = new Class[] {
            int.class
        };

        private static final String FLAG_EXCLUDE_STOPPED_PACKAGES = "FLAG_EXCLUDE_STOPPED_PACKAGES";

        private static final String FLAG_RECEIVER_FOREGROUND = "FLAG_RECEIVER_FOREGROUND";

        /**
         * Using reflection to add FLAG_EXCLUDE_STOPPED_PACKAGES support backward compatibility.
         * 
         * @param intent Intent to set flags
         */
        static void tryToSetExcludeStoppedPackagesFlag(Intent intent) {

            if (Build.VERSION.SDK_INT < HONEYCOMB_MR1_VERSION_CODE) {
                /*
                 * Since FLAG_EXCLUDE_STOPPED_PACKAGES is introduced only from API level
                 * HONEYCOMB_MR1_VERSION_CODE we need to do nothing if we are running on a version
                 * prior that so we just return then.
                 */
                return;
            }

            try {
                Method addflagsMethod = intent.getClass().getDeclaredMethod(ADD_FLAGS_METHOD_NAME,
                        ADD_FLAGS_PARAM);
                Field flagExcludeStoppedPackages = intent.getClass().getDeclaredField(
                        FLAG_EXCLUDE_STOPPED_PACKAGES);
                addflagsMethod.invoke(intent, flagExcludeStoppedPackages.getInt(IntentUtils.class));
            } catch (Exception e) {
                // Do nothing
            }
        }

        /**
         * Using reflection to add FLAG_RECEIVER_FOREGROUND support backward compatibility.
         * 
         * @param intent Intent to set flags
         */
        static void tryToSetReceiverForegroundFlag(Intent intent) {

            if (Build.VERSION.SDK_INT < JELLY_BEAN_VERSION_CODE) {
                /*
                 * Since FLAG_RECEIVER_FOREGROUND is introduced only from API level
                 * JELLY_BEAN_VERSION_CODE we need to do nothing if we are running on a version
                 * prior that so we just return then.
                 */
                return;
            }

            try {
                Method addflagsMethod = intent.getClass().getDeclaredMethod(ADD_FLAGS_METHOD_NAME,
                        ADD_FLAGS_PARAM);
                Field flagReceiverForeground = intent.getClass().getDeclaredField(
                        FLAG_RECEIVER_FOREGROUND);
                addflagsMethod.invoke(intent, flagReceiverForeground.getInt(IntentUtils.class));
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    /**
     * Update flags of the broadcast intent to increase performance
     * 
     * @param intent
     */
    private void trySetIntentForActivePackageAndReceiverInForeground(Intent intent) {
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(intent);
        IntentUtils.tryToSetReceiverForegroundFlag(intent);
    }

    /**
     * Returns true if the RCS stack is installed and not disabled on the device.
     * 
     * @return boolean true if the RCS stack is installed and not disabled on the device.
     */
    public boolean isAvailable() {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    RCS_STACK_PACKAGENAME, 0);
            return (appInfo.flags & ApplicationInfo.FLAG_STOPPED) == 0;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private boolean IsDataRoamingEnabled() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null) {
            return false;
        }
        return cm.getActiveNetworkInfo().isRoaming();
    }

    /**
     * Query a string value knowing the corresponding key.
     * 
     * @param ctx the context
     * @param key the key
     * @return the string value
     * @throws RcsPersistentStorageException
     */
    private static String getStringValueSetting(Context ctx, final String key)
            throws RcsPersistentStorageException {
        ContentResolver cr = ctx.getContentResolver();
        String[] selectionArgs = new String[] {
            key
        };
        Cursor cursor = null;
        try {
            cursor = cr.query(RcsServiceControlLog.CONTENT_URI, PROJECTION, WHERE_CLAUSE,
                    selectionArgs, null);
            if (cursor == null) {
                throw new RcsPersistentStorageException(
                        "Fail to query RCS Service Control Log for key=" + key + "!");
            }
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(RcsServiceControlLog.VALUE));
            }
            return null;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Gets how to show the RCS enabled/disabled switch
     * 
     * @return EnableRcseSwitch instance
     * @throws RcsPersistentStorageException
     */
    private EnableRcseSwitch getEnableRcseSwitch() throws RcsPersistentStorageException {
        String result = getStringValueSetting(mContext, RcsServiceControlLog.ENABLE_RCS_SWITCH);
        return EnableRcseSwitch.valueOf(Integer.parseInt(result));
    }

    /**
     * Returns true if the RCS stack de-activation/activation is allowed by the client.
     * 
     * @return boolean true if the RCS stack de-activation/activation is allowed by the client.
     * @throws RcsPersistentStorageException
     */
    public boolean isActivationModeChangeable() throws RcsPersistentStorageException {
        EnableRcseSwitch enableRcseSwitch = getEnableRcseSwitch();
        switch (enableRcseSwitch) {
            case ALWAYS_SHOW:
                return true;
            case ONLY_SHOW_IN_ROAMING:
                return IsDataRoamingEnabled();
            case NEVER_SHOW:
            default:
                return false;
        }
    }
    /**
     * Checks if the RCS stack is marked as active on the device.
     * 
     * @return true if the RCS stack is marked as active on the device.
     * @throws RcsPersistentStorageException
     */
    public boolean isActivated() throws RcsPersistentStorageException {
        String result = getStringValueSetting(mContext, RcsServiceControlLog.SERVICE_ACTIVATED);
        return Boolean.parseBoolean(result);
    }

    /**
     * Deactive/Activate the RCS stack in case these operations are allowed (see
     * isStackActivationStatusChangeable) or else throws an RcsPermissionDeniedException.
     * 
     * @param active True is activation is enabled.
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     */
    public void setActivationMode(boolean active) throws RcsPermissionDeniedException,
            RcsPersistentStorageException {
        boolean activationChangeable = isActivationModeChangeable();
        if (!activationChangeable) {
            throw new RcsPermissionDeniedException("Stack activation mode not changeable");
        }
        Intent intent = new Intent(Service.ACTION_SET_ACTIVATION_MODE);
        intent.setPackage(RCS_STACK_PACKAGENAME);
        intent.putExtra(Service.EXTRA_SET_ACTIVATION_MODE, active);
        trySetIntentForActivePackageAndReceiverInForeground(intent);
        mContext.sendBroadcast(intent);
    }

    /**
     * Queries for the compatibility report between the client RCS API and core RCS stack.
     */
    public void queryCompatibility() {
        Intent intent = new Intent(Service.ACTION_QUERY_COMPATIBILITY);
        intent.putExtra(Service.EXTRA_QUERY_COMPATIBILITY_CODENAME, RcsService.Build.API_CODENAME);
        intent.putExtra(Service.EXTRA_QUERY_COMPATIBILITY_VERSION, RcsService.Build.API_VERSION);
        intent.putExtra(Service.EXTRA_QUERY_COMPATIBILITY_INCREMENT,
                RcsService.Build.API_INCREMENTAL);
        intent.putExtra(Service.EXTRA_QUERY_COMPATIBILITY_PACKAGENAME, mPackageName);
        intent.setPackage(RCS_STACK_PACKAGENAME);
        trySetIntentForActivePackageAndReceiverInForeground(intent);
        mContext.sendBroadcast(intent);
    }

    /**
     * Queries for the RCS service Starting state.
     */
    public void queryServiceStartingState() {
        Intent intent = new Intent(Service.ACTION_QUERY_SERVICE_STARTING_STATE);
        intent.putExtra(Service.EXTRA_QUERY_SERVICE_STARTING_STATE_PACKAGENAME, mPackageName);
        intent.setPackage(RCS_STACK_PACKAGENAME);
        trySetIntentForActivePackageAndReceiverInForeground(intent);
        mContext.sendBroadcast(intent);
    }
}
