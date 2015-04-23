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
 * Is-composing events generator (see RFC3994)
 * 
 * @author jexa7410
 */
public class IsComposingGenerator {

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(IsComposingGenerator.class
            .getSimpleName());

    // Event IDs
    private static enum ComposingEvent {
        IS_COMPOSING, IS_NOT_COMPOSING
    };

    private ExpirationTimer mExpirationTimer;

    private ComposingEvent mLastComposingEvent = ComposingEvent.IS_NOT_COMPOSING;
    private long mLastOnComposingTimestamp = -1;
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
     * 
     * @param isComposing
     */
    public void handleIsComposingEvent() {
        boolean activated = sLogger.isActivated();
        if (activated) {
            sLogger.debug("handleIsComposingEvent");
        }
        long now = System.currentTimeMillis();
        if (ComposingEvent.IS_NOT_COMPOSING == mLastComposingEvent && mIsLastCommandSucessfull) {
            mIsLastCommandSucessfull = mSession.sendIsComposingStatus(true);
            if (!mIsLastCommandSucessfull) {
                sLogger.warn("mSession.sendIsComposingStatus command failed");
            }
            if (mExpirationTimer == null) {
                if (activated) {
                    sLogger.debug("No active ExpirationTimer : schedule new task");
                }
                mExpirationTimer = new ExpirationTimer(now);
            }
        }
        mLastOnComposingTimestamp = now;
    }

    /**
     * Expiration Timer
     */
    private class ExpirationTimer extends TimerTask {

        private final static String TIMER_NAME = "IS_COMPOSING_GENERATOR_TIMER";

        private long mActivationDate;
        private Timer mTimer;

        public ExpirationTimer(long activationDate) {
            mActivationDate = activationDate;
            mTimer = new Timer(TIMER_NAME);
            mTimer.schedule(this, mRcsSettings.getIsComposingTimeout());
        }

        @Override
        public void run() {
            boolean activated = sLogger.isActivated();
            if (activated) {
                sLogger.debug("OnComposing timer has expired: ");
            }

            long now = System.currentTimeMillis();
            if (mActivationDate < mLastOnComposingTimestamp) {
                if (activated) {
                    sLogger.debug(" --> user is still composing");
                }
                mExpirationTimer = new ExpirationTimer(now);
            } else {
                if (activated) {
                    sLogger.debug(" --> go into IDLE state");
                }
                mLastComposingEvent = ComposingEvent.IS_NOT_COMPOSING;
                mIsLastCommandSucessfull = mSession.sendIsComposingStatus(false);
                if (!mIsLastCommandSucessfull) {
                    sLogger.warn("mSession.sendIsComposingStatus command failed");
                }
                mExpirationTimer = null;
            }
            mTimer.cancel();
            mTimer = null;
        }
    }

}
