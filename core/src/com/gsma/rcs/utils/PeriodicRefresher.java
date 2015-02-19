/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
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

package com.gsma.rcs.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Periodic refresher
 * 
 * @author JM. Auffret
 */
public abstract class PeriodicRefresher {

    private static final int KITKAT_VERSION_CODE = 19;

    private static final int MILLISEC_CONVERSION_RATE = 1000;

    private static final String SET_EXACT_METHOD_NAME = "setExact";

    private static final String ERROR_SET_EXACT_METHOD_NOT_FOUND = "Failed to get setExact method";

    private static final String ERROR_SET_EXACT_METHOD_NO_ACCESS = "No access to the definition of setExact method";

    private static final String ERROR_SET_EXACT_METHOD_INVOKE = "Can't invoke setExact method";

    private static final Class[] SET_EXACT_METHOD_PARAM = new Class[] {
            int.class,
            long.class,
            PendingIntent.class
    };

    /**
     * Keep alive manager
     */
    private KeepAlive alarmReceiver = new KeepAlive();

    /**
     * Alarm intent
     */
    private PendingIntent alarmIntent;

    /**
     * Action
     */
    private String action;

    /**
     * Timer state
     */
    private boolean mTimerStarted = false;

    /**
     * Polling period
     */
    private int pollingPeriod;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public PeriodicRefresher() {
        // Create a unique pending intent
        this.action = this.toString(); // Unique action ID
        this.alarmIntent = PendingIntent.getBroadcast(AndroidFactory.getApplicationContext(), 0,
                new Intent(action), 0);
    }

    /**
     * Periodic processing
     *
     * @throws RcsServiceException
     */
    public abstract void periodicProcessing() throws RcsServiceException;

    /**
     * Start the timer
     * 
     * @param expirePeriod Expiration period in seconds
     * @throws RcsServiceException
     */
    public void startTimer(int expirePeriod) throws RcsServiceException {
        startTimer(expirePeriod, 1.0);
    }

    /**
     * Start the timer
     * 
     * @param expirePeriod Expiration period in seconds
     * @param delta Delta to apply on the expire period in percentage
     * @throws RcsServiceException
     */
    public synchronized void startTimer(int expirePeriod, double delta) throws RcsServiceException {
        // Check expire period
        if (expirePeriod <= 0) {
            // Expire period is null
            if (logger.isActivated()) {
                logger.debug("Timer is deactivated");
            }
            return;
        }

        // Calculate the effective refresh period
        pollingPeriod = (int) (expirePeriod * delta);
        if (logger.isActivated()) {
            logger.debug("Start timer at period=" + pollingPeriod + "s (expiration=" + expirePeriod
                    + "s)");
        }

        final Context ctx = AndroidFactory.getApplicationContext();
        ctx.registerReceiver(alarmReceiver, new IntentFilter(action));
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < KITKAT_VERSION_CODE) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + pollingPeriod
                    * MILLISEC_CONVERSION_RATE, alarmIntent);
            mTimerStarted = true;
        } else {
            try {
                Method setExactMethod = alarmManager.getClass().getDeclaredMethod(
                        SET_EXACT_METHOD_NAME, SET_EXACT_METHOD_PARAM);
                setExactMethod.invoke(alarmManager, AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + pollingPeriod * MILLISEC_CONVERSION_RATE,
                        alarmIntent);
                mTimerStarted = true;
            } catch (NoSuchMethodException e) {
                throw new RcsServiceException(new StringBuilder(ERROR_SET_EXACT_METHOD_NOT_FOUND)
                        .append(e.getMessage()).toString());
            } catch (IllegalAccessException e) {
                throw new RcsServiceException(new StringBuilder(ERROR_SET_EXACT_METHOD_NO_ACCESS)
                        .append(e.getMessage()).toString());
            } catch (InvocationTargetException e) {
                throw new RcsServiceException(new StringBuilder(ERROR_SET_EXACT_METHOD_INVOKE)
                        .append(e.getMessage()).toString());
            }
        }
    }

    /**
     * Stop the timer
     */
    public synchronized void stopTimer() {
        if (!mTimerStarted) {
            // Already stopped
            return;
        }

        if (logger.isActivated()) {
            logger.debug("Stop timer");
        }

        // The timer is stopped
        mTimerStarted = false;

        // Cancel alarm
        AlarmManager am = (AlarmManager) AndroidFactory.getApplicationContext().getSystemService(
                Context.ALARM_SERVICE);
        am.cancel(alarmIntent);

        // Unregister the alarm receiver
        try {
            AndroidFactory.getApplicationContext().unregisterReceiver(alarmReceiver);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    /**
     * Keep alive manager
     */
    private class KeepAlive extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Thread t = new Thread() {
                public void run() {
                    // Processing
                    try {
                        periodicProcessing();
                    } catch (RcsServiceException e) {
                        stopTimer();
                    }
                }
            };
            t.start();
        }
    }
}
