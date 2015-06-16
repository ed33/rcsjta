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

package com.gsma.rcs.service;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.Intents.Service;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControlLog.EnableRcseSwitch;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.history.HistoryService;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;
import com.gsma.services.rcs.sharing.image.ImageSharingService;
import com.gsma.services.rcs.sharing.video.VideoSharingService;
import com.gsma.services.rcs.upload.FileUploadService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class to control the service activation.
 * 
 * @author yplo6403
 */
public class RcsServiceControlReceiver extends BroadcastReceiver {

    private final static Logger sLogger = Logger.getLogger(RcsServiceControlReceiver.class
            .getSimpleName());

    private RcsSettings mRcsSettings;

    private Context mContext;

    private static final int INVALID_EXTRA = -1;

    private interface IRcsCompatibility {
        public boolean isCompatible(String serviceName, String codename, int version, int increment);
    }

    private static IRcsCompatibility sRcsCompatibility = new IRcsCompatibility() {

        @Override
        public boolean isCompatible(String serviceName, String codename, int version, int increment) {
            if (!RcsService.Build.API_CODENAME.equals(codename)) {
                return false;
            }
            /*
             * For the 1rst release of the core stack this method is common to all services so we
             * don't check the service name
             */
            if (RcsService.Build.API_VERSION != version) {
                return false;
            }
            if (RcsService.Build.API_INCREMENTAL != increment) {
                return false;
            }
            return true;
        }
    };

    private static final Map<String, IRcsCompatibility> sServiceCompatibilityMap = new HashMap<String, IRcsCompatibility>();

    private static final int NOT_COMPATIBLE = 0;
    private static final int COMPATIBLE = 1;
    
    static {
        sServiceCompatibilityMap.put(CapabilityService.class.getSimpleName(), sRcsCompatibility);
        sServiceCompatibilityMap.put(ContactService.class.getSimpleName(), sRcsCompatibility);
        sServiceCompatibilityMap.put(ChatService.class.getSimpleName(), sRcsCompatibility);
        sServiceCompatibilityMap.put(FileTransferService.class.getSimpleName(), sRcsCompatibility);
        sServiceCompatibilityMap.put(FileUploadService.class.getSimpleName(), sRcsCompatibility);
        sServiceCompatibilityMap.put(GeolocSharingService.class.getSimpleName(), sRcsCompatibility);
        sServiceCompatibilityMap.put(HistoryService.class.getSimpleName(), sRcsCompatibility);
        sServiceCompatibilityMap.put(ImageSharingService.class.getSimpleName(), sRcsCompatibility);
        sServiceCompatibilityMap.put(MultimediaSessionService.class.getSimpleName(),
                sRcsCompatibility);
        sServiceCompatibilityMap.put(VideoSharingService.class.getSimpleName(), sRcsCompatibility);
    }

    private boolean getActivationModeChangeable() {
        EnableRcseSwitch enableRcseSwitch = mRcsSettings.getEnableRcseSwitch();
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

    private boolean IsDataRoamingEnabled() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null) {
            return false;
        }
        return cm.getActiveNetworkInfo().isRoaming();
    }

    private void setActivationMode(Context context, boolean active) {
        if (!getActivationModeChangeable()) {
            if (sLogger.isActivated()) {
                sLogger.error("Cannot set activation mode: permission denied");
            }
            return;
        }
        if (mRcsSettings.isServiceActivated() == active) {
            if (sLogger.isActivated()) {
                sLogger.warn("Activation mode already set to ".concat(String.valueOf(active)));
            }
            return;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("setActivationMode: ".concat(String.valueOf(active)));
        }
        mRcsSettings.setServiceActivationState(active);
        if (active) {
            LauncherUtils.launchRcsService(mContext, false, true, mRcsSettings);
        } else {
            LauncherUtils.stopRcsService(mContext);
        }
    }

    private String getCompatibilityReport(String codename, int version, int increment) {
        if (TextUtils.isEmpty(codename) || version == INVALID_EXTRA || increment == INVALID_EXTRA) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Entry<String, IRcsCompatibility> serviceCompatEntry : sServiceCompatibilityMap
                .entrySet()) {
            String service = serviceCompatEntry.getKey();
            IRcsCompatibility iRcsCompatibility = serviceCompatEntry.getValue();
            Boolean compatible = iRcsCompatibility.isCompatible(service, codename, version,
                    increment);
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(service).append("=").append(compatible ? COMPATIBLE : NOT_COMPATIBLE);
        }
        return sb.toString();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LocalContentResolver localContentResolver = new LocalContentResolver(context);
        mRcsSettings = RcsSettings.createInstance(localContentResolver);
        mContext = context;
        String action = intent.getAction();
        if (Service.ACTION_SET_ACTIVATION_MODE.equals(action)) {
            boolean active = intent.getBooleanExtra(Service.EXTRA_SET_ACTIVATION_MODE, true);
            setActivationMode(context, active);
            if (sLogger.isActivated()) {
                sLogger.debug("Set activation=".concat(Boolean.toString(active)));
            }
        } else if (Service.ACTION_QUERY_COMPATIBILITY.equals(action)) {
            String packageName = intent
                    .getStringExtra(Service.EXTRA_QUERY_COMPATIBILITY_PACKAGENAME);
            if (packageName == null) {
                sLogger.warn("No package name for compatibility query!");
                return;
            }
            if (mContext.getPackageName().equals(packageName)) {
                sLogger.warn("Wrong package name for compatibility query!");
                return;
            }
            String codename = intent.getStringExtra(Service.EXTRA_QUERY_COMPATIBILITY_CODENAME);
            int version = intent.getIntExtra(Service.EXTRA_QUERY_COMPATIBILITY_VERSION,
                    INVALID_EXTRA);
            int increment = intent.getIntExtra(Service.EXTRA_QUERY_COMPATIBILITY_INCREMENT,
                    INVALID_EXTRA);

            String report = getCompatibilityReport(codename, version, increment);
            Intent response = new Intent(Service.ACTION_RESP_COMPATIBILITY);
            if (report != null) {
                response.putExtra(Service.EXTRA_RESP_COMPATIBILITY, report);
            }
            response.setPackage(packageName);
            IntentUtils.tryToSetExcludeStoppedPackagesFlag(response);
            IntentUtils.tryToSetReceiverForegroundFlag(response);
            mContext.sendBroadcast(response);
        } else if (Service.ACTION_QUERY_SERVICE_STARTING_STATE.equals(action)) {
            String packageName = intent
                    .getStringExtra(Service.EXTRA_QUERY_SERVICE_STARTING_STATE_PACKAGENAME);
            if (packageName == null) {
                sLogger.warn("No package name for service starting state query!");
                return;
            }
            if (mContext.getPackageName().equals(packageName)) {
                sLogger.warn("Wrong package name for service starting state query!");
                return;
            }
            Core core = Core.getInstance();
            boolean started = core != null && core.isStarted();
            if (sLogger.isActivated()) {
                sLogger.debug("Service started ".concat(Boolean.toString(started)));
            }
            Intent response = new Intent(Service.ACTION_RESP_SERVICE_STARTING_STATE);
            response.putExtra(Service.EXTRA_RESP_SERVICE_STARTING_STATE, started);
            response.setPackage(packageName);
            IntentUtils.tryToSetExcludeStoppedPackagesFlag(response);
            IntentUtils.tryToSetReceiverForegroundFlag(response);
            mContext.sendBroadcast(response);
        }
    }
}
