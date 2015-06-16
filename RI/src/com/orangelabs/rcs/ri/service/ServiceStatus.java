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

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.capability.CapabilityService;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Display and monitor the service status
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServiceStatus extends Activity implements RcsServiceListener {

    private RcsService mApi;

    private LockAccess mExitOnce = new LockAccess();

    private RcsServiceControl mRcsServiceControl;

    private TextView mServiceBound;

    private TextView mServiceActivated;

    private CheckBox mServiceActivationRefresh;

    private TextView mServiceStarted;

    private QueryRcsServiceStartingState mQueryRcsServiceStartingState;

    private static final String LOGTAG = LogUtils.getTag(ServiceStatus.class.getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRcsServiceControl = RiApplication.getRcsServiceControl();

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.service_status);

        mServiceBound = (TextView) findViewById(R.id.service_bound);
        mServiceActivated = (TextView) findViewById(R.id.service_activated);
        mServiceStarted = (TextView) findViewById(R.id.service_started);
        mServiceActivationRefresh = (CheckBox) findViewById(R.id.service_refresh_all);
        mServiceActivationRefresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                displayServiceActivation();
                displayServiceStarted();
            }

        });

        // Display service status by default
        displayServiceBinding(false);

        displayServiceActivation();

        displayServiceStarted();

        // Register service up event listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RcsService.ACTION_SERVICE_UP);
        registerReceiver(serviceUpListener, intentFilter);

        // Instantiate API
        mApi = new CapabilityService(getApplicationContext(), this);

        // Connect API
        try {
            mApi.connect();
        } catch (RcsPermissionDeniedException e) {
            mApi = null;
            Utils.showMessageAndExit(this, getString(R.string.label_api_not_compatible), mExitOnce);
        }
    }

    private void displayServiceActivation() {
        try {
            mServiceActivated.setText(Boolean.toString(mRcsServiceControl.isActivated()));
        } catch (RcsPersistentStorageException e) {
            Log.e(LOGTAG, "Failed to check if stack is activated", e);
            mServiceActivated.setText(getString(R.string.error_service_activated));
        }
    }

    private void displayServiceStarted() {
        mQueryRcsServiceStartingState = new QueryRcsServiceStartingState(this, mRcsServiceControl,
                0, new QueryRcsServiceStartingState.IListener() {

                    @Override
                    public void handleResponse(boolean serviceStarted) {
                        mServiceStarted.setText(Boolean.toString(serviceStarted));
                    }
                });
        mQueryRcsServiceStartingState.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister service up event listener
        try {
            unregisterReceiver(serviceUpListener);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
        if (mQueryRcsServiceStartingState != null) {
            mQueryRcsServiceStartingState.cancel(true);
        }

        if (mApi != null) {
            // Disconnect API
            mApi.disconnect();
        }
    }

    /**
     * Callback called when service is connected. This method is called when the service is well
     * connected to the RCS service (binding procedure successful): this means the methods of the
     * API may be used.
     */
    public void onServiceConnected() {
        // Display service binding status
        displayServiceBinding(true);
    }

    /**
     * Callback called when service has been disconnected. This method is called when the service is
     * disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     */
    public void onServiceDisconnected(ReasonCode error) {
        // Display service binding status
        displayServiceBinding(false);
    }

    /**
     * Display service status
     * 
     * @param status Status
     */
    private void displayServiceBinding(boolean status) {
        mServiceBound.setText(String.valueOf(status));
    }

    /**
     * RCS service up event listener
     */
    private BroadcastReceiver serviceUpListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            // Retry a connection to the service
            try {
                mApi.connect();
            } catch (RcsPermissionDeniedException e) {
                mApi = null;
                Utils.showMessageAndExit(ServiceStatus.this,
                        getString(R.string.label_api_not_compatible), mExitOnce);
            }
        }
    };
}
