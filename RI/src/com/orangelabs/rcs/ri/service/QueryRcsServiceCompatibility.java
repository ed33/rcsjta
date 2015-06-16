
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

import com.gsma.services.rcs.Intents.Service;
import com.gsma.services.rcs.RcsServiceControl;

import com.orangelabs.rcs.ri.utils.LogUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;

/**
 * @author yplo6403
 */
public class QueryRcsServiceCompatibility extends AsyncTask<Void, Void, String> {

    private static final long INTENT_RESPONSE_TIMEOUT = 10000;
    
    private final Context mContext;
    private final RcsServiceControl mRcsServiceControl;
    private final IListener mIListener;
    private final Object mLock = new Object();
    private String mReport;
    private RcsServiceReceiver mRcsServiceReceiver;

    /**
     * Constructor
     * 
     * @param context Application context
     * @param rcsServiceControl utility to control RCS service
     * @param iListener listener
     */
    public QueryRcsServiceCompatibility(Context context, RcsServiceControl rcsServiceControl,
            IListener iListener) {
        mContext = context;
        mRcsServiceControl = rcsServiceControl;
        mIListener = iListener;
        mRcsServiceReceiver = new RcsServiceReceiver();
    }

    /**
     * Interface to listen for the response
     */
    public interface IListener {
        /**
         * Callback response
         * 
         * @param report of compatibility
         */
        public void handleResponse(String report);
    }

    private class RcsServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Service.ACTION_RESP_COMPATIBILITY.equals(intent.getAction())) {
                return;
            }
            Bundle extras = intent.getExtras();
            if(extras == null) {
                return;
            } 
            mReport = extras.getString(Service.EXTRA_RESP_COMPATIBILITY);
            if (mReport == null) {
                return;
            }
            synchronized (mLock) {
                mLock.notify();
            }
        }

    }

    @Override
    protected void onPreExecute() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Service.ACTION_RESP_COMPATIBILITY);
        mContext.registerReceiver(mRcsServiceReceiver, filter);
    }

    @Override
    protected String doInBackground(Void... param) {
        mRcsServiceControl.queryCompatibility();
        synchronized (mLock) {
            try {
                mLock.wait(INTENT_RESPONSE_TIMEOUT);
            } catch (InterruptedException e) {
                mContext.unregisterReceiver(mRcsServiceReceiver);
                mRcsServiceReceiver = null;
            }
        }
        return mReport;
    }

    @Override
    protected void onPostExecute(String result) {
        if (mRcsServiceReceiver != null) {
            mContext.unregisterReceiver(mRcsServiceReceiver);
        }
        mIListener.handleResponse(result);
    }
}
