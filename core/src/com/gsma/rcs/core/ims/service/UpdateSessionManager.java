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

package com.gsma.rcs.core.ims.service;

import javax2.sip.Dialog;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ipcall.IPCallError;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Update session manager
 * 
 * @author owom5460
 */
public class UpdateSessionManager {

    /**
     * Session to be renegociated
     */
    private ImsServiceSession mSession;

    /**
     * Re-Invite invitation status
     */
    private int mReInviteStatus = ImsServiceSession.INVITATION_NOT_ANSWERED;

    /**
     * Wait user answer for reInvite invitation
     */
    private Object mWaitUserAnswer = new Object();

    /**
     * Ringing period (in seconds)
     */
    private final int mRingingPeriod;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(UpdateSessionManager.class.getName());

    /**
     * Constructor
     * 
     * @param session Session to be refreshed
     * @param rcsSettings
     */
    public UpdateSessionManager(ImsServiceSession session, RcsSettings rcsSettings) {
        mSession = session;
        mRingingPeriod = rcsSettings.getRingingPeriod();
    }

    /**
     * Create ReInvite
     * 
     * @param featureTags featureTags to set in reInvite
     * @param content reInvite content
     * @return reInvite request
     */
    public SipRequest createReInvite(String[] featureTags, String content) {
        if (sLogger.isActivated()) {
            sLogger.debug("createReInvite()");
        }

        SipRequest reInvite = null;

        try {
            // Increment the Cseq number of the dialog path
            mSession.getDialogPath().incrementCseq();
            if (sLogger.isActivated()) {
                sLogger.info("Increment DialogPath CSeq - DialogPath CSeq ="
                        + mSession.getDialogPath().getCseq());
            }

            // Increment internal stack CSeq (NIST stack issue?)
            Dialog dlg = mSession.getDialogPath().getStackDialog();
            while ((dlg != null) && (dlg.getLocalSeqNumber() < mSession.getDialogPath().getCseq())) {
                dlg.incrementLocalSequenceNumber();
                if (sLogger.isActivated()) {
                    sLogger.info("Increment LocalSequenceNumber -  Dialog local Seq Number ="
                            + dlg.getLocalSeqNumber());
                }
            }

            // create ReInvite
            reInvite = SipMessageFactory.createReInvite(mSession.getDialogPath(), featureTags,
                    content);
            if (sLogger.isActivated()) {
                sLogger.info("reInvite created -  reInvite CSeq =" + reInvite.getCSeq());
            }

            // Set the Authorization header
            mSession.getAuthenticationAgent().setAuthorizationHeader(reInvite);

            // Set the Proxy-Authorization header
            mSession.getAuthenticationAgent().setProxyAuthorizationHeader(reInvite);

        } catch (SipException e) {
            // Unexpected error
            mSession.handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        } catch (CoreException e) {
            // Unexpected error
            mSession.handleError(new IPCallError(IPCallError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }

        return reInvite;

    }

    /**
     * Send ReInvite
     * 
     * @param request ReInvite request
     * @param serviceContext service context of ReInvite
     */
    public void sendReInvite(SipRequest request, int serviceContext) {
        if (sLogger.isActivated()) {
            sLogger.debug("sendReInvite()");
        }

        final SipRequest reInvite = request;
        final int reInviteContext = serviceContext;

        Thread thread = new Thread() {
            public void run() {
                SipTransactionContext ctx;
                try {
                    // Send ReINVITE request
                    ctx = mSession.getImsService().getImsModule().getSipManager()
                            .sendSipMessageAndWait(reInvite, mSession.getResponseTimeout());

                    if (ctx.isSipResponse()) { // Analyze the received response
                        if (ctx.getStatusCode() == 200) {

                            // // set received sdp response as remote sdp content
                            mSession.getDialogPath().setRemoteContent(
                                    ctx.getSipResponse().getSdpContent());

                            // notify session with 200OK response
                            mSession.handleReInviteResponse(200, ctx.getSipResponse(),
                                    reInviteContext);

                            // send SIP ACK
                            mSession.getImsService().getImsModule().getSipManager()
                                    .sendSipAck(mSession.getDialogPath());

                        } else if (ctx.getStatusCode() == 603) {
                            // notify session with 603 response
                            mSession.handleReInviteResponse(ImsServiceSession.INVITATION_REJECTED,
                                    ctx.getSipResponse(), reInviteContext);
                        } else if (ctx.getStatusCode() == 408) {
                            // notify session with 408 response
                            mSession.handleReInviteResponse(
                                    ImsServiceSession.TERMINATION_BY_TIMEOUT, ctx.getSipResponse(),
                                    reInviteContext);
                        } else if (ctx.getStatusCode() == 407) {
                            // notify session with 407 Proxy Authent required
                            mSession.handleReInvite407ProxyAuthent(ctx.getSipResponse(),
                                    reInviteContext);
                        } else {
                            // Other error response => generate call error
                            mSession.handleError(new ImsSessionBasedServiceError(
                                    ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, ctx
                                            .getSipResponse().getStatusCode()
                                            + " "
                                            + ctx.getSipResponse().getReasonPhrase()));
                        }
                    } else {
                        // No response received: timeout => notify session
                        mSession.handleReInviteResponse(ImsServiceSession.TERMINATION_BY_TIMEOUT,
                                ctx.getSipResponse(), reInviteContext);
                    }
                } catch (SipException e) {
                    // Unexpected error => generate call error
                    if (sLogger.isActivated()) {
                        sLogger.error("Send ReInvite has failed", e);
                    }
                    mSession.handleError(new ImsSessionBasedServiceError(
                            ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, e.getMessage()));
                }
            }
        };
        thread.start();

    }

    /**
     * Receive RE-INVITE request
     * 
     * @param request RE-INVITE request
     * @param featureTags featureTags to set in request
     * @param sdpResponse
     * @param serviceContext service context of reInvite request
     */
    public void send200OkReInviteResp(SipRequest request, String[] featureTags, String sdpResponse,
            int serviceContext) {
        if (sLogger.isActivated()) {
            sLogger.debug("receiveReInvite()");
        }

        final SipRequest reInvite = request;
        final String sdp = sdpResponse;
        final int reInviteContext = serviceContext;
        final String[] respFeatureTags = featureTags;

        Thread thread = new Thread() {
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Send 200 OK");
                    }
                    // Create 200 OK response
                    SipResponse resp = SipMessageFactory.create200OkReInviteResponse(
                            mSession.getDialogPath(), reInvite, respFeatureTags, sdp);
                    // Send 200 OK response
                    SipTransactionContext ctx = mSession.getImsService().getImsModule()
                            .getSipManager().sendSipMessageAndWait(resp);

                    // Analyze the received response
                    if (ctx.isSipAck()) {
                        // ACK received
                        if (sLogger.isActivated()) {
                            sLogger.info("ACK request received");
                        }
                        // notify local listener
                        mSession.handleReInviteAck(200, reInviteContext);
                    } else {
                        if (sLogger.isActivated()) {
                            sLogger.debug("No ACK received for ReINVITE");
                        }
                        // No ACK received => generate call error for local client
                        mSession.handleError(new ImsSessionBasedServiceError(
                                ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION,
                                "ack not received"));
                    }
                } catch (Exception e) {
                    // Unexpected error => generate call error for local client
                    if (sLogger.isActivated()) {
                        sLogger.error("Session update refresh has failed", e);
                    }
                    mSession.handleError(new ImsSessionBasedServiceError(
                            ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, e.getMessage()));
                }
            }
        };

        thread.start();

    }

    /**
     * Receive RE-INVITE request
     * 
     * @param request RE-INVITE request
     * @param featureTags featureTags to set in request
     * @param serviceContext service context of reInvite request
     */
    public void waitUserAckAndSendReInviteResp(SipRequest request, String[] featureTags,
            int serviceContext) {
        if (sLogger.isActivated()) {
            sLogger.debug("receiveReInviteAndWait()");
        }

        mReInviteStatus = ImsServiceSession.INVITATION_NOT_ANSWERED;
        final SipRequest reInvite = request;
        final int reInviteContext = serviceContext;
        final String[] respFeatureTags = featureTags;

        Thread thread = new Thread() {
            public void run() {
                try {
                    // wait user answer
                    int answer = waitInvitationAnswer();

                    if (answer == ImsServiceSession.INVITATION_REJECTED) {
                        // Invitation declined by user
                        if (sLogger.isActivated()) {
                            sLogger.debug("reInvite has been rejected by user");
                        }

                        // send error to remote client
                        mSession.sendErrorResponse(reInvite,
                                mSession.getDialogPath().getLocalTag(),
                                603);
                        mSession.handleReInviteUserAnswer(ImsServiceSession.INVITATION_REJECTED,
                                reInviteContext);
                    } else if (answer == ImsServiceSession.INVITATION_NOT_ANSWERED) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Session has been rejected on timeout");
                        }

                        // send error to remote client
                        mSession.sendErrorResponse(reInvite,
                                mSession.getDialogPath().getLocalTag(),
                                603);
                        mSession.handleReInviteUserAnswer(
                                ImsServiceSession.INVITATION_NOT_ANSWERED,
                                reInviteContext);

                    } else if (answer == ImsServiceSession.INVITATION_ACCEPTED) {
                        if (sLogger.isActivated()) {
                            sLogger.debug("Send 200 OK");
                        }

                        // build sdp response
                        String sdp = mSession.buildReInviteSdpResponse(reInvite, reInviteContext);
                        if (sdp == null) {
                            // sdp null - terminate session and send error
                            mSession.handleError(new ImsSessionBasedServiceError(
                                    ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION,
                                    "error on sdp building, sdp is null "));
                            return;
                        }

                        // set sdp response as local content
                        mSession.getDialogPath().setLocalContent(sdp);

                        mSession.handleReInviteUserAnswer(ImsServiceSession.INVITATION_ACCEPTED,
                                reInviteContext);

                        // create 200OK response
                        SipResponse resp = SipMessageFactory.create200OkReInviteResponse(
                                mSession.getDialogPath(), reInvite, respFeatureTags, sdp);

                        // Send response
                        SipTransactionContext ctx = mSession.getImsService().getImsModule()
                                .getSipManager().sendSipMessageAndWait(resp);

                        // Analyze the received response
                        if (ctx.isSipAck()) {
                            // ACK received
                            if (sLogger.isActivated()) {
                                sLogger.info("ACK request received");
                                sLogger.info("ACK status code = " + ctx.getStatusCode());
                            }

                            // notify local listener
                            mSession.handleReInviteAck(200, reInviteContext);
                        } else {
                            if (sLogger.isActivated()) {
                                sLogger.debug("No ACK received for INVITE");
                            }
                            // No ACK received: send error
                            mSession.handleError(new ImsSessionBasedServiceError(
                                    ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION,
                                    "ack not received"));
                        }
                    } else {
                        mSession.handleError(new ImsSessionBasedServiceError(
                                ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION,
                                "ack not received"));
                    }
                } catch (Exception e) {
                    if (sLogger.isActivated()) {
                        sLogger.error("Session update refresh has failed", e);
                    }
                    // Unexpected error
                    mSession.handleError(new ImsSessionBasedServiceError(
                            ImsSessionBasedServiceError.UNEXPECTED_EXCEPTION, e.getMessage()));
                }
            }
        };

        thread.start();
    }

    /**
     * Reject the session invitation
     * 
     * @param code Error code
     */
    public void rejectReInvite(int code) {
        if (sLogger.isActivated()) {
            sLogger.debug("ReInvite  has been rejected");
        }

        synchronized (mWaitUserAnswer) {
            mReInviteStatus = ImsServiceSession.INVITATION_REJECTED;

            // Unblock semaphore
            mWaitUserAnswer.notifyAll();
        }

        // Decline the invitation
        // session.sendErrorResponse(session.getDialogPath().getInvite(),
        // session.getDialogPath().getLocalTag(), code);
    }

    /**
     * Accept the session invitation
     */
    public void acceptReInvite() {
        if (sLogger.isActivated()) {
            sLogger.debug("ReInvite has been accepted");
        }

        synchronized (mWaitUserAnswer) {
            mReInviteStatus = ImsServiceSession.INVITATION_ACCEPTED;

            // Unblock semaphore
            mWaitUserAnswer.notifyAll();
        }
    }

    /**
     * Wait session invitation answer
     * 
     * @return Answer
     */
    public int waitInvitationAnswer() {
        if (mReInviteStatus != ImsServiceSession.INVITATION_NOT_ANSWERED) {
            return mReInviteStatus;
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Wait session invitation answer");
        }

        try {
            synchronized (mWaitUserAnswer) {
                // Wait until received response or received timeout
                mWaitUserAnswer.wait(mRingingPeriod * 500);
            }
        } catch (InterruptedException e) {

        }

        return mReInviteStatus;
    }

}
