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

import android.os.Handler;
import android.os.Message;

/**
 * Is-composing events generator (see RFC3994)
 * 
 * @author jexa7410
 */
public class IsComposingGenerator {
    // Event IDs
    private final static int IS_STARTING_COMPOSING = 1;
    private final static int IS_STILL_COMPOSING = 2;
    private final static int MESSAGE_WAS_SENT = 3;
    private final static int ACTIVE_MESSAGE_NEEDS_REFRESH = 4;
    private final static int IS_IDLE = 5;

    // Active state refresh interval (in ms)
    private final static int ACTIVE_STATE_REFRESH = 60 * 1000;

    /**
     * Idle timeout in ms
     */
    private int mIdleTimeOut = 0;

    /**
     * Is composing state
     */
    private boolean mComposing = false;
    
    /**
     * Timeout clock
     */
    private TimeoutClock mTimeoutClock = new TimeoutClock();

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
        mIdleTimeOut = rcsSettings.getIsComposingTimeout()*1000;
    }

    /**
     * Edit text has activity
     */
    public void hasActivity() {
        if (!mComposing) {
            // If we were not already in isComposing state
            mTimeoutClock.sendEmptyMessage(IS_STARTING_COMPOSING);
            mComposing = true;
        } else {
            // We already were composing
            mTimeoutClock.sendEmptyMessage(IS_STILL_COMPOSING);
        }
    }

    /**
     * Edit text has no activity anymore
     */
    public void hasNoActivity() {
        mComposing = false;
    }

    /**
     * The message was sent
     */
    public void messageWasSent() {
        mTimeoutClock.sendEmptyMessage(MESSAGE_WAS_SENT);
    }
    
    /**
     * Timeout clock
     */
    private class TimeoutClock extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case IS_STARTING_COMPOSING: {
                    // Send a typing status "active"
                    mSession.sendIsComposingStatus(true);

                    // In IDLE_TIME_OUT we will need to send a is-idle status
                    // message
                    mTimeoutClock.sendEmptyMessageDelayed(IS_IDLE, mIdleTimeOut);

                    // In ACTIVE_STATE_REFRESH we will need to send an active
                    // status message refresh
                    mTimeoutClock.sendEmptyMessageDelayed(ACTIVE_MESSAGE_NEEDS_REFRESH,
                            ACTIVE_STATE_REFRESH);
                    break;
                }
                case IS_STILL_COMPOSING: {
                    // Cancel the IS_IDLE messages in queue, if there was one
                    mTimeoutClock.removeMessages(IS_IDLE);

                    // In IDLE_TIME_OUT we will need to send a is-idle status
                    // message
                    mTimeoutClock.sendEmptyMessageDelayed(IS_IDLE, mIdleTimeOut);
                    break;
                }
                case MESSAGE_WAS_SENT: {
                    // We are now going to idle state
                    hasNoActivity();

                    // Cancel the IS_IDLE messages in queue, if there was one
                    mTimeoutClock.removeMessages(IS_IDLE);

                    // Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in
                    // queue, if there was one
                    mTimeoutClock.removeMessages(ACTIVE_MESSAGE_NEEDS_REFRESH);
                    break;
                }
                case ACTIVE_MESSAGE_NEEDS_REFRESH: {
                    // We have to refresh the "active" state
                    mSession.sendIsComposingStatus(true);

                    // In ACTIVE_STATE_REFRESH we will need to send an active
                    // status message refresh
                    mTimeoutClock.sendEmptyMessageDelayed(ACTIVE_MESSAGE_NEEDS_REFRESH,
                            ACTIVE_STATE_REFRESH);
                    break;
                }
                case IS_IDLE: {
                    // End of typing
                    hasNoActivity();

                    // Send a typing status "idle"
                    mSession.sendIsComposingStatus(false);

                    // Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in
                    // queue, if there was one
                    mTimeoutClock.removeMessages(ACTIVE_MESSAGE_NEEDS_REFRESH);
                    break;
                }
            }
        }
    }
}
