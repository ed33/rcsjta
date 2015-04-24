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

package com.gsma.rcs.core.ims.service.im.chat;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Is-composing events generator
 * 
 * @author jexa7410
 */
public class IsComposingGenerator {

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(IsComposingGenerator.class
            .getSimpleName());

    /**
     * OnComposing events
     */
    private static enum ComposingEvent {
        IS_COMPOSING, IS_NOT_COMPOSING
    };

    /**
     * Expiration timer
     */
    private ExpirationTimer mExpirationTimer;

    /**
     * The last composing event
     */
    private ComposingEvent mLastComposingEvent = ComposingEvent.IS_NOT_COMPOSING;

    /**
     * The timestamp of the last composing event
     */
    private long mLastOnComposingTimestamp = -1;

    /**
     * The status of the last sendIsComposingStatus command
     */
    private boolean mIsLastCommandSucessfull = true;

    /**
     * RcsSettings
     */
    private RcsSettings mRcsSettings;

    /**
     * Chat session
     */
    private ChatSession mSession;

    /**
     * Constructor
     * 
     * @param session Chat session
     * @param settings Settings
     */
    public IsComposingGenerator(ChatSession session, RcsSettings rcsSettings) {
        mSession = session;
        mRcsSettings = rcsSettings;
    }

    /**
     * Handle isComposingEvent from API
     */
    public void handleIsComposingEvent() {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("handleIsComposingEvent");
        }

        mLastOnComposingTimestamp = System.currentTimeMillis();
        if (ComposingEvent.IS_NOT_COMPOSING == mLastComposingEvent && mIsLastCommandSucessfull) {
            mIsLastCommandSucessfull = mSession.sendIsComposingStatus(true);
        }
        mLastComposingEvent = ComposingEvent.IS_COMPOSING;
        if (mExpirationTimer == null) {
            if (logActivated) {
                sLogger.debug("No active ExpirationTimer : schedule new task");
            }
            if (!mIsLastCommandSucessfull && logActivated) {
                sLogger.debug("The last sendIsComposingStatus command has failed");
            }
            mExpirationTimer = new ExpirationTimer(mLastOnComposingTimestamp);
        }
    }

    /**
     * Expiration Timer
     */
    private class ExpirationTimer extends TimerTask {

        private final static String TIMER_NAME = "IS_COMPOSING_GENERATOR_TIMER";

        private long mActivationDate;
        private Timer mTimer;

        /**
         * Default constructor
         * 
         * @param activationDate
         */
        public ExpirationTimer(long activationDate) {
            mActivationDate = activationDate;
            mTimer = new Timer(TIMER_NAME);
            mTimer.schedule(this, mRcsSettings.getIsComposingTimeout());
        }

        @Override
        public void run() {
            boolean logActivated = sLogger.isActivated();
            if (logActivated) {
                sLogger.debug("OnComposing timer has expired: ");
            }

            long now = System.currentTimeMillis();
            if (mActivationDate < mLastOnComposingTimestamp) {
                if (logActivated) {
                    sLogger.debug(" --> user is still composing");
                }
                if (!mIsLastCommandSucessfull) {
                    if (logActivated) {
                        sLogger.debug(" --> The last sendIsComposingStatus command has failed. Send a new one");
                    }
                    mIsLastCommandSucessfull = mSession.sendIsComposingStatus(true);
                }
                mExpirationTimer = new ExpirationTimer(now);
            } else {
                if (logActivated) {
                    sLogger.debug(" --> go into IDLE state");
                }
                mLastComposingEvent = ComposingEvent.IS_NOT_COMPOSING;
                if(mIsLastCommandSucessfull){
                    mIsLastCommandSucessfull = mSession.sendIsComposingStatus(false);    
                }                
                mExpirationTimer = null;
            }
            mTimer.cancel();
            mTimer = null;
        }
    }

}
