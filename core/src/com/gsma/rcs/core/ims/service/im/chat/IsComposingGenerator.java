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
import android.os.Looper;
import android.os.Message;
import android.util.SparseArray;

/**
 * Is-composing events generator (see RFC3994)
 * 
 * @author jexa7410
 */
public class IsComposingGenerator {

    // Event IDs
    private static enum ComposingEvent {
        IS_STARTING_COMPOSING(0), IS_STILL_COMPOSING(1), MESSAGE_WAS_SENT(2), ACTIVE_MESSAGE_NEEDS_REFRESH(
                3), IS_IDLE(4);

        private int mValue;

        private static SparseArray<ComposingEvent> mValueToEnum = new SparseArray<ComposingEvent>();
        static {
            for (ComposingEvent entry : ComposingEvent.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private ComposingEvent(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public static ComposingEvent valueOf(int value) {
            ComposingEvent entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + ComposingEvent.class.getName() + "." + value);
        }
    }

    // Active state refresh interval (in ms)
    private final static int ACTIVE_STATE_REFRESH = 60 * 1000;

    /**
     * Idle timeout in ms
     */
    private RcsSettings mRcsSettings;

    /**
     * Is composing state
     */
    private boolean mComposing = false;

    /**
     * Timeout clock
     */
    private TimeoutClock mTimeoutClock = new TimeoutClock(Looper.getMainLooper());

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
     * Set composing activity
     * 
     * @param isComposing
     */
    public void setOngoingActivity(boolean isComposing) {
        if (isComposing) {
            if (!mComposing) {
                // If we were not already in isComposing state
                mTimeoutClock.sendEmptyMessage(ComposingEvent.IS_STARTING_COMPOSING);
            } else {
                // We already were composing
                mTimeoutClock.sendEmptyMessage(ComposingEvent.IS_STILL_COMPOSING);
            }
        }
        mComposing = isComposing;
    }

    /**
     * The message was sent
     */
    public void messageWasSent() {
        mTimeoutClock.sendEmptyMessage(ComposingEvent.MESSAGE_WAS_SENT);
    }

    /**
     * Timeout clock
     */
    private class TimeoutClock extends Handler {

        public TimeoutClock(Looper looper) {
            super(looper);
        }

        public void sendEmptyMessageDelayed(ComposingEvent composingEvent, long delayMillis) {
            super.sendEmptyMessageDelayed(composingEvent.toInt(), delayMillis);
        }

        public void removeMessages(ComposingEvent composingEvent) {
            super.removeMessages(composingEvent.toInt());
        }

        public void sendEmptyMessage(ComposingEvent composingEvent) {
            super.sendEmptyMessage(composingEvent.toInt());
        }

        public void handleMessage(Message msg) {
            switch (ComposingEvent.valueOf(msg.what)) {
                case IS_STARTING_COMPOSING:
                    // Send a typing status "active"
                    mSession.sendIsComposingStatus(true);

                    // In IDLE_TIME_OUT we will need to send a is-idle status
                    // message
                    mTimeoutClock.sendEmptyMessageDelayed(ComposingEvent.IS_IDLE,
                            mRcsSettings.getIsComposingTimeout());

                    // In ACTIVE_STATE_REFRESH we will need to send an active
                    // status message refresh
                    mTimeoutClock.sendEmptyMessageDelayed(
                            ComposingEvent.ACTIVE_MESSAGE_NEEDS_REFRESH, ACTIVE_STATE_REFRESH);
                    break;

                case IS_STILL_COMPOSING:
                    // Cancel the IS_IDLE messages in queue, if there was one
                    mTimeoutClock.removeMessages(ComposingEvent.IS_IDLE);

                    // In IDLE_TIME_OUT we will need to send a is-idle status
                    // message
                    mTimeoutClock.sendEmptyMessageDelayed(ComposingEvent.IS_IDLE,
                            mRcsSettings.getIsComposingTimeout());
                    break;

                case MESSAGE_WAS_SENT:
                    // We are now going to idle state
                    setOngoingActivity(false);

                    // Cancel the IS_IDLE messages in queue, if there was one
                    mTimeoutClock.removeMessages(ComposingEvent.IS_IDLE);

                    // Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in
                    // queue, if there was one
                    mTimeoutClock.removeMessages(ComposingEvent.ACTIVE_MESSAGE_NEEDS_REFRESH);
                    break;

                case ACTIVE_MESSAGE_NEEDS_REFRESH:
                    // We have to refresh the "active" state
                    mSession.sendIsComposingStatus(true);

                    // In ACTIVE_STATE_REFRESH we will need to send an active
                    // status message refresh
                    mTimeoutClock.sendEmptyMessageDelayed(
                            ComposingEvent.ACTIVE_MESSAGE_NEEDS_REFRESH, ACTIVE_STATE_REFRESH);
                    break;

                case IS_IDLE:
                    // End of typing
                    setOngoingActivity(false);

                    // Send a typing status "idle"
                    mSession.sendIsComposingStatus(false);

                    // Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in
                    // queue, if there was one
                    mTimeoutClock.removeMessages(ComposingEvent.ACTIVE_MESSAGE_NEEDS_REFRESH);
                    break;
            }
        }
    }
}
