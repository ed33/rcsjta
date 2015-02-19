/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.protocol.sip;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceException;

import java.io.IOException;

/**
 * Keep-alive manager (see RFC 5626)
 * 
 * @author BJ
 */
public class KeepAliveManager extends PeriodicRefresher {

    private static final String ERROR_SIP_HEARTBEAT_FAILED = "SIP heartbeat has failed";

    /**
     * Keep-alive period (in seconds)
     */
    private int period;

    /**
     * SIP interface
     */
    private SipInterface sip;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public KeepAliveManager(SipInterface sip) {
        this.sip = sip;
        this.period = RcsSettings.getInstance().getSipKeepAlivePeriod();
    }

    /**
     * Start
     *
     * @throws RcsServiceException
     */
    public void start() throws RcsServiceException {
        if (logger.isActivated()) {
            logger.debug("Start keep-alive");
        }
        startTimer(period, 1);
    }

    /**
     * Start
     */
    public void stop() {
        if (logger.isActivated()) {
            logger.debug("Stop keep-alive");
        }
        stopTimer();
    }

    /**
     * Keep-alive processing
     *
     * @throws RcsServiceException
     */
    public void periodicProcessing() throws RcsServiceException {
        if (logger.isActivated()) {
            logger.debug("Send keep-alive");
        }

        // Send a double-CRLF
        try {
            sip.getDefaultSipProvider().getListeningPoints()[0].sendHeartbeat(
                    sip.getOutboundProxyAddr(), sip.getOutboundProxyPort());
            // Start timer
            startTimer(period, 1);
        } catch (IOException e) {
            throw new RcsServiceException(new StringBuilder(ERROR_SIP_HEARTBEAT_FAILED).append(
                    e.getMessage()).toString());
        }
    }

    /**
     * @param period the keep alive period in seconds
     */
    public void setPeriod(int period) {
        this.period = period;
        if (logger.isActivated()) {
            logger.debug("Set keep-alive period \"" + period + "\"");
        }
    }
}
