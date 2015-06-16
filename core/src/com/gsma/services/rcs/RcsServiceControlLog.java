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

import android.net.Uri;
import android.util.SparseArray;

/**
 * Content provider for service control
 * 
 */
public class RcsServiceControlLog {
    /**
     * Content provider URI
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.services.rcs.provider.service/service");

    /**
     * The name of the column containing the key setting for a row.
     */
    public static final String KEY = "key";

    /**
     * The name of the column containing the value setting for a row.
     */
    public static final String VALUE = "value";

    /**
     * Key to get enableRcseSwitch value
     */
    public static final String ENABLE_RCS_SWITCH = "enableRcseSwitch";
    
    /**
     * Key to get ServiceActivated value
     */
    public static final String SERVICE_ACTIVATED = "ServiceActivated";
    
    /**
     * EnableRcseSwitch describes whether or not to show the RCS enabled/disabled switch permanently
     */
    public enum EnableRcseSwitch {
        /**
         * the switch is shown permanently
         */
        ALWAYS_SHOW(1),
        /**
         * the switch is only shown during roaming
         */
        ONLY_SHOW_IN_ROAMING(0),
        /**
         * the switch is never shown
         */
        NEVER_SHOW(-1);

        private int mValue;

        private static SparseArray<EnableRcseSwitch> mValueToEnum = new SparseArray<EnableRcseSwitch>();
        static {
            for (EnableRcseSwitch entry : EnableRcseSwitch.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private EnableRcseSwitch(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * @param value
         * @return NetworkAccessType
         */
        public static EnableRcseSwitch valueOf(int value) {
            EnableRcseSwitch entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(EnableRcseSwitch.class.getName()).append(".").append(value).toString());
        }

    };
}
