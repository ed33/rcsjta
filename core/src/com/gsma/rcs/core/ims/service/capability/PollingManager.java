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

package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

/**
 * Polling manager which updates capabilities periodically
 * 
 * @author Jean-Marc AUFFRET
 */
public class PollingManager extends PeriodicRefresher {

    /**
     * Capability service
     */
    private final CapabilityService mImsService;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContatManager;

    /**
     * The logger
     */
    private final static Logger sLogger = Logger.getLogger(PollingManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param rcsSettings
     * @param contactManager
     */
    public PollingManager(CapabilityService parent, RcsSettings rcsSettings,
            ContactManager contactManager) {
        mImsService = parent;
        mRcsSettings = rcsSettings;
        mContatManager = contactManager;
    }

    /**
     * Start polling
     */
    public void start() {
        long pollingPeriod = mRcsSettings.getCapabilityPollingPeriod();
        if (pollingPeriod == 0) {
            return;
        }
        startTimer(System.currentTimeMillis(), pollingPeriod);
    }

    /**
     * Stop polling
     */
    public void stop() {
        stopTimer();
    }

    /**
     * Update processing
     */
    public void periodicProcessing() {
        // Make a registration
        if (sLogger.isActivated()) {
            sLogger.info("Execute new capabilities update");
        }

        // Update all contacts capabilities if refresh timeout has not expired
        Set<ContactId> contacts = mContatManager.getAllContactsFromRcsContactProvider();
        for (ContactId contact : contacts) {
            requestContactCapabilities(contact);
        }

        // Restart timer
        startTimer(System.currentTimeMillis(), mRcsSettings.getCapabilityPollingPeriod());
    }

    /**
     * Request contact capabilities
     * 
     * @param contact Contact identifier
     */
    private void requestContactCapabilities(ContactId contact) {
        // Read capabilities from the database
        Capabilities capabilities = mContatManager.getContactCapabilities(contact);
        boolean locActivated = sLogger.isActivated();
        if (capabilities == null) {
            if (locActivated) {
                sLogger.debug("No capability exist for ".concat(contact.toString()));
            }

            // New contact: request capabilities from the network
            mImsService.getOptionsManager().requestCapabilities(contact);
            return;

        }
        if (isCapabilityRefreshRequired(capabilities.getTimestampOfLastResponse(), mRcsSettings)) {
            if (locActivated) {
                sLogger.debug("Capabilities have expired for ".concat(contact.toString()));
            }

            // Capabilities are too old: request capabilities from the network
            if (capabilities.isPresenceDiscoverySupported()) {
                // If contact supports capability discovery via presence, use the selected
                // discoveryManager
                mImsService.getAnonymousFetchManager().requestCapabilities(contact);
            } else {
                // The contact only supports OPTIONS requests
                mImsService.getOptionsManager().requestCapabilities(contact);
            }
        } else {
            if (locActivated) {
                sLogger.debug("Capabilities exist for ".concat(contact.toString()));
            }
        }
    }

    /**
     * Check if refresh of capability is required
     * 
     * @param timestampOfLastResponse time of last capability response in milliseconds
     * @param rcsSettings
     * @return true if capability refresh is required
     */
    private boolean isCapabilityRefreshRequired(long timestampOfLastResponse,
            RcsSettings rcsSettings) {
        long now = System.currentTimeMillis();
        // Is current time before last capability response ? (may occur if system time has been
        // modified)
        if (now < timestampOfLastResponse) {
            return true;
        }
        // Is current time after capability expiration time ?
        return (now > (timestampOfLastResponse + rcsSettings.getCapabilityExpiryTimeout()));
    }
}
