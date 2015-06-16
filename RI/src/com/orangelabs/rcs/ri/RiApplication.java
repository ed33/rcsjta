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

package com.orangelabs.rcs.ri;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceControl;

import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.service.QueryRcsServiceCompatibility;
import com.orangelabs.rcs.ri.service.QueryRcsServiceCompatibility.IListener;
import com.orangelabs.rcs.ri.utils.LogUtils;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This subclass of Application allows to get a resource content from a static context
 * 
 * @author YPLO6403
 */
public class RiApplication extends Application {

    private static Context mContext;

    /**
     * Array of participant statuses
     */
    public static String[] sParticipantStatuses;

    /**
     * Array of delivery statuses
     */
    public static String[] sDeliveryStatuses;

    /**
     * Array of delivery reason codes
     */
    public static String[] sDeliveryReasonCode;

    /**
     * Array of Group CHAT states
     */
    public static String[] sGroupChatStates;

    /**
     * Array of Group CHAT reason codes
     */
    public static String[] sGroupChatReasonCodes;

    /**
     * Array of message reason codes
     */
    public static String[] sMessageReasonCodes;

    /**
     * Array of message statuses
     */
    public static String[] sMessagesStatuses;

    /**
     * Array of file transfer states
     */
    public static String[] sFileTransferStates;

    /**
     * Array of file transfer reason codes
     */
    public static String[] sFileTransferReasonCodes;

    /**
     * Array of Image sharing states
     */
    public static String[] sImageSharingStates;

    /**
     * Array of Image sharing reason codes
     */
    public static String[] sImageSharingReasonCodes;

    /**
     * Array of Video sharing states
     */
    public static String[] sVideoSharingStates;

    /**
     * Array of Video sharing reason codes
     */
    public static String[] sVideoReasonCodes;

    /**
     * Array of Geolocation sharing states
     */
    public static String[] sGeolocSharingStates;

    /**
     * Array of Geolocation sharing reason codes
     */
    public static String[] sGeolocReasonCodes;

    /**
     * Array of MULTIMEDIA Messaging Session states
     */
    public static String[] sMultimediaStates;

    /**
     * Array of MULTIMEDIA Messaging Session codes
     */
    public static String[] sMultimediaReasonCodes;

    /**
     * Array of group chat events
     */
    public static String[] sGroupChatEvents;

    private static Map<Direction, String> sDirectionToString;

    private static RcsServiceControl mRcsServiceControl;

    private static final String LOGTAG = LogUtils.getTag(RiApplication.class.getSimpleName());

    /**
     * Gets direction
     * 
     * @param direction Direction
     * @return String
     */
    public static String getDirection(Direction direction) {
        return sDirectionToString.get(direction);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        Resources resources = getResources();
        sParticipantStatuses = convertForUI(resources.getStringArray(R.array.participant_statuses));
        sDeliveryStatuses = convertForUI(resources.getStringArray(R.array.delivery_statuses));
        sDeliveryReasonCode = convertForUI(resources.getStringArray(R.array.delivery_reason_codes));
        sGroupChatStates = convertForUI(resources.getStringArray(R.array.group_chat_states));
        sGroupChatReasonCodes = convertForUI(resources
                .getStringArray(R.array.group_chat_reason_codes));
        sMessageReasonCodes = convertForUI(resources.getStringArray(R.array.message_reason_codes));
        sMessagesStatuses = convertForUI(resources.getStringArray(R.array.message_statuses));
        sFileTransferStates = convertForUI(resources.getStringArray(R.array.file_transfer_states));
        sFileTransferReasonCodes = convertForUI(resources
                .getStringArray(R.array.file_transfer_reason_codes));
        sImageSharingStates = convertForUI(resources.getStringArray(R.array.ish_states));
        sImageSharingReasonCodes = convertForUI(resources.getStringArray(R.array.ish_reason_codes));
        sVideoSharingStates = convertForUI(resources.getStringArray(R.array.vsh_states));
        sVideoReasonCodes = convertForUI(resources.getStringArray(R.array.vsh_reason_codes));
        sGeolocSharingStates = convertForUI(resources.getStringArray(R.array.gsh_states));
        sGeolocReasonCodes = convertForUI(resources.getStringArray(R.array.gsh_reason_codes));
        sMultimediaStates = convertForUI(resources.getStringArray(R.array.mms_states));
        sMultimediaReasonCodes = convertForUI(resources.getStringArray(R.array.mms_reason_codes));
        sGroupChatEvents = convertForUI(resources.getStringArray(R.array.group_chat_event));

        sDirectionToString = new HashMap<Direction, String>();
        sDirectionToString.put(Direction.INCOMING, resources.getString(R.string.label_incoming));
        sDirectionToString.put(Direction.OUTGOING, resources.getString(R.string.label_outgoing));
        sDirectionToString.put(Direction.IRRELEVANT,
                resources.getString(R.string.label_direction_unknown));

        mRcsServiceControl = RcsServiceControl.getInstance(mContext);
        List<RcsServiceName> services = Arrays.asList(RcsServiceName.values());
        ConnectionManager.createInstance(mContext, new HashSet<RcsServiceName>(services)).connectApis(0);

        QueryRcsServiceCompatibility queryCompatibility = new QueryRcsServiceCompatibility(
                mContext, mRcsServiceControl, new IListener() {
                    @Override
                    public void handleResponse(String report) {
                        if (report == null) {
                            Log.d(LOGTAG, "Failed to query for RCS service compatibility report");
                        } else {
                            Log.d(LOGTAG, "RCS service compatibility report=".concat(report));
                        }
                    }
                });
        queryCompatibility.execute();
    }

    private String[] convertForUI(String[] strings) {
        List<String> stringList = Arrays.asList(strings);
        for (int i = 0, l = stringList.size(); i < l; ++i) {
            stringList.set(i, stringList.get(i).toLowerCase(Locale.getDefault()).replace('_', ' '));
        }
        return (String[]) stringList.toArray();
    }

    /**
     * Gets the application context
     * 
     * @return the application context
     */
    public static Context getAppContext() {
        return mContext;
    }

    /**
     * Gets the RCS service control singleton
     * 
     * @return the RCS service control singleton
     */
    public static RcsServiceControl getRcsServiceControl() {
        return mRcsServiceControl;
    }

}
