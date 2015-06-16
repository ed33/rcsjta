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

package com.orangelabs.rcs.ri.service;

import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.Intents.Service;

import com.orangelabs.rcs.ri.utils.LogUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;

/**
 * @author yplo6403
 */
public class QueryRcsServiceStartingState extends AsyncTask<Void, Void, Boolean> {

    private static final long INTENT_RESPONSE_TIMEOUT = 2000;

    private static final String LOGTAG = LogUtils.getTag(QueryRcsServiceStartingState.class
            .getSimpleName());

    private final Context mContext;
    private final RcsServiceControl mRcsServiceControl;
    private final IListener mIListener;
    private final Object mLock = new Object();
    private Boolean mStarted = false;
    private RcsServiceReceiver mRcsServiceReceiver;
    private final long mDelay;

    /**
     * Constructor
     * 
     * @param context Application context
     * @param rcsServiceControl utility to control RCS service
     * @param delay Delay before querying RCS service start 
     * @param iListener listener
     */
    public QueryRcsServiceStartingState(Context context, RcsServiceControl rcsServiceControl,
            long delay, IListener iListener) {
        mContext = context;
        mRcsServiceControl = rcsServiceControl;
        mIListener = iListener;
        mRcsServiceReceiver = new RcsServiceReceiver();
        mDelay = delay;
    }

    /**
     * Interface to listen for the response
     */
    public interface IListener {
        /**
         * Callback response
         * 
         * @param serviceStarted True if RCS services are started
         */
        public void handleResponse(boolean serviceStarted);
    }

    private class RcsServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Service.ACTION_RESP_SERVICE_STARTING_STATE.equals(intent.getAction())) {
                return;
            }
            mStarted = intent.getBooleanExtra(Service.EXTRA_RESP_SERVICE_STARTING_STATE, false);
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "RCS service started=".concat(mStarted.toString()));
            }
            synchronized (mLock) {
                mLock.notify();
            }
        }

    }

    @Override
    protected void onPreExecute() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Service.ACTION_RESP_SERVICE_STARTING_STATE);
        mContext.registerReceiver(mRcsServiceReceiver, filter);
    }

    @Override
    protected Boolean doInBackground(Void... param) {
        if (mDelay > 0) {
            try {
                Thread.sleep(mDelay);
            } catch (InterruptedException ex) {
                mContext.unregisterReceiver(mRcsServiceReceiver);
                mRcsServiceReceiver = null;
                return false;
            }
        }
        mRcsServiceControl.queryServiceStartingState();
        synchronized (mLock) {
            try {
                mLock.wait(INTENT_RESPONSE_TIMEOUT);
                return mStarted;
            } catch (InterruptedException e) {
                mContext.unregisterReceiver(mRcsServiceReceiver);
                mRcsServiceReceiver = null;
                return false;
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (mRcsServiceReceiver != null) {
            mContext.unregisterReceiver(mRcsServiceReceiver);
        }
        mIListener.handleResponse(result != null && result);
    }
}
