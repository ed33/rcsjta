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

package com.gsma.rcs.core.ims.service.im;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.CoreListener;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession.InvitationStatus;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OriginatingAdhocGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OriginatingOneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.RejoinGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.RestartGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingOneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.chat.standfw.StoreAndForwardManager;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatMessageSession;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatNotificationSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.ImsFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.DownloadFromInviteFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpThumbnail;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpGroupFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.msrp.OriginatingMsrpFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.msrp.TerminatingMsrpFileSharingSession;
import com.gsma.rcs.core.ims.service.upload.FileUploadSession;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.provider.settings.RcsSettingsData.ImMsgTech;
import com.gsma.rcs.service.api.ServerApiMaxAllowedSessionLimitReachedException;
import com.gsma.rcs.service.api.ServerApiPersistentStorageException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax2.sip.header.ContactHeader;
import javax2.sip.message.Response;

/**
 * Instant messaging services (1-1 chat, group chat and file transfer)
 * 
 * @author Jean-Marc AUFFRET
 */
public class InstantMessagingService extends ImsService {

    private final Core mCore;

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    private final MessagingLog mMessagingLog;

    /**
     * OneToOneChatSessionCache with ContactId as key
     */
    private Map<ContactId, OneToOneChatSession> mOneToOneChatSessionCache = new HashMap<ContactId, OneToOneChatSession>();

    /**
     * StoreAndForwardMsgSessionCache with ContactId as key
     */
    private Map<ContactId, TerminatingStoreAndForwardOneToOneChatMessageSession> mStoreAndForwardMsgSessionCache = new HashMap<ContactId, TerminatingStoreAndForwardOneToOneChatMessageSession>();

    /**
     * StoreAndForwardNotifSessionCache with ContactId as key
     */
    private Map<ContactId, TerminatingStoreAndForwardOneToOneChatNotificationSession> mStoreAndForwardNotifSessionCache = new HashMap<ContactId, TerminatingStoreAndForwardOneToOneChatNotificationSession>();

    /**
     * GroupChatSessionCache with ChatId as key
     */
    private Map<String, GroupChatSession> mGroupChatSessionCache = new HashMap<String, GroupChatSession>();

    /**
     * FileSharingSessionCache with FileTransferId as key
     */
    private Map<String, FileSharingSession> mFileTransferSessionCache = new HashMap<String, FileSharingSession>();

    /**
     * FileUploadSessionCache with UploadId as key
     */
    private Map<String, FileUploadSession> mFileUploadSessionCache = new HashMap<String, FileUploadSession>();

    /**
     * GroupChatConferenceSubscriberCache with Conference subscriber's dialog path CallId as key
     */
    private Map<String, GroupChatSession> mGroupChatConferenceSubscriberCache = new HashMap<String, GroupChatSession>();

    /**
     * Group Chat composing status to notify upon MSRP session restart
     */
    private final Map<String, Boolean> mGroupChatComposingStatusToNotify = new HashMap<String, Boolean>();

    /**
     * One-to-One Chat composing status to notify upon MSRP session restart
     */
    private final Map<ContactId, Boolean> mOneToOneChatComposingStatusToNotify = new HashMap<ContactId, Boolean>();

    /**
     * Chat features tags
     */
    public final static String[] CHAT_FEATURE_TAGS = {
        FeatureTags.FEATURE_OMA_IM
    };

    /**
     * File transfer features tags
     */
    public final static String[] FT_FEATURE_TAGS = {
        FeatureTags.FEATURE_OMA_IM
    };

    /**
     * IMDN manager
     */
    private final ImdnManager mImdnManager;

    /**
     * Store & Forward manager
     */
    private final StoreAndForwardManager mStoreAndFwdMgr;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(InstantMessagingService.class.getName());

    private static final String sSizeExceededMsg = "133 Size exceeded";

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param core Core
     * @param rcsSettings RcsSettings
     * @param contactsManager ContactManager
     * @param messagingLog MessagingLog
     * @throws CoreException
     */
    public InstantMessagingService(ImsModule parent, Core core, RcsSettings rcsSettings,
            ContactManager contactsManager, MessagingLog messagingLog) throws CoreException {
        super(parent, true);
        mCore = core;
        mRcsSettings = rcsSettings;
        mContactManager = contactsManager;
        mMessagingLog = messagingLog;
        mStoreAndFwdMgr = new StoreAndForwardManager(this, mRcsSettings, mContactManager,
                mMessagingLog);
        mImdnManager = new ImdnManager(this, mRcsSettings, mMessagingLog);
        mImdnManager.start();
    }

    private void handleMsrpFileTransferInvitationRejected(SipRequest invite, ContactId contact,
            FileTransfer.ReasonCode reasonCode, long timestamp, long timestampSent)
            throws SipPayloadException {
        MmContent content = ContentManager.createMmContentFromSdp(invite, mRcsSettings);
        MmContent fileIcon = FileTransferUtils.extractFileIcon(invite, mRcsSettings);
        mCore.getListener().handleFileTransferInvitationRejected(contact, content, fileIcon,
                reasonCode, timestamp, timestampSent);
    }

    private void handleGroupChatInvitationRejected(SipRequest invite, ContactId contact,
            ReasonCode reasonCode, long timestamp) {
        String chatId = ChatUtils.getContributionId(invite);
        String subject = ChatUtils.getSubject(invite);
        Map<ContactId, ParticipantStatus> participants = ChatUtils.getParticipants(invite,
                ParticipantStatus.FAILED);
        mCore.getListener().handleGroupChatInvitationRejected(chatId, contact, subject,
                participants, reasonCode, timestamp);
    }

    private void send403Forbidden(SipRequest request, String warning) {
        try {
            /* Send a 403 Forbidden */
            if (sLogger.isActivated()) {
                sLogger.info("Send 403 Forbidden (warning=" + warning + ")");
            }
            SipResponse resp = SipMessageFactory.createResponse(request, null, Response.FORBIDDEN,
                    warning);
            getImsModule().getSipManager().sendSipResponse(resp);
        } catch (SipException e) {
            /* Better exception handling after CR037 */
            sLogger.error("Can't send 403 Forbidden response", e);
        }
    }

    /**
     * Start the IMS service
     */
    public synchronized void start() {
        if (isServiceStarted()) {
            // Already started
            return;
        }
        setServiceStarted(true);

        mCore.getListener().tryToStartImServiceTasks(mCore);
    }

    /**
     * Stop the IMS service
     */
    public synchronized void stop() {
        if (!isServiceStarted()) {
            // Already stopped
            return;
        }
        setServiceStarted(false);

        mImdnManager.terminate();
        mImdnManager.interrupt();
    }

    /**
     * Check the IMS service
     */
    public void check() {
    }

    /**
     * Returns the IMDN manager
     * 
     * @return IMDN manager
     */
    public ImdnManager getImdnManager() {
        return mImdnManager;
    }

    /**
     * Get Store & Forward manager
     */
    public StoreAndForwardManager getStoreAndForwardManager() {
        return mStoreAndFwdMgr;
    }

    public void addSession(OneToOneChatSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add OneToOneChatSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mOneToOneChatSessionCache.put(contact, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final OneToOneChatSession session) {
        final ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove OneToOneChatSession with contact '")
                    .append(contact).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mOneToOneChatSessionCache.remove(contact);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public OneToOneChatSession getOneToOneChatSession(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get OneToOneChatSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mOneToOneChatSessionCache.get(contact);
        }
    }

    public void addSession(TerminatingStoreAndForwardOneToOneChatMessageSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add StoreAndForwardMsgSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mStoreAndForwardMsgSessionCache.put(contact, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final TerminatingStoreAndForwardOneToOneChatMessageSession session) {
        final ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove StoreAndForwardMsgSession with contact '")
                    .append(contact).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mStoreAndForwardMsgSessionCache.remove(contact);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public TerminatingStoreAndForwardOneToOneChatMessageSession getStoreAndForwardMsgSession(
            ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get StoreAndForwardMsgSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mStoreAndForwardMsgSessionCache.get(contact);
        }
    }

    public void addSession(TerminatingStoreAndForwardOneToOneChatNotificationSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add StoreAndForwardNotifSessionCache with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mStoreAndForwardNotifSessionCache.put(contact, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(
            final TerminatingStoreAndForwardOneToOneChatNotificationSession session) {
        final ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder(
                    "Remove StoreAndForwardNotifSessionCache with contact '").append(contact)
                    .append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mStoreAndForwardNotifSessionCache.remove(contact);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public TerminatingStoreAndForwardOneToOneChatNotificationSession getStoreAndForwardNotifSession(
            ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get StoreAndForwardNotifSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mStoreAndForwardNotifSessionCache.get(contact);
        }
    }

    public void addSession(GroupChatSession session) {
        String chatId = session.getContributionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add GroupChatSession with chatId '").append(chatId)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatSessionCache.put(chatId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GroupChatSession session) {
        final String chatId = session.getContributionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove GroupChatSession with chatId '").append(chatId)
                    .append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    SipDialogPath conferenceSubscriberDialogPath = session
                            .getConferenceEventSubscriber().getDialogPath();
                    if (conferenceSubscriberDialogPath != null) {
                        mGroupChatConferenceSubscriberCache.remove(conferenceSubscriberDialogPath
                                .getCallId());
                    }
                    mGroupChatSessionCache.remove(chatId);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public GroupChatSession getGroupChatSession(String chatId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get GroupChatSession with chatId '").append(chatId)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGroupChatSessionCache.get(chatId);
        }
    }

    public void addGroupChatConferenceSubscriber(String callId, GroupChatSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add GroupChatConferenceSubscriber with callId '")
                    .append(callId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatConferenceSubscriberCache.put(callId, session);
        }
    }

    public void removeGroupChatConferenceSubscriber(final String callId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove GroupChatConferenceSubscriber with callId '")
                    .append(callId).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mGroupChatConferenceSubscriberCache.remove(callId);
                }
            }
        }.start();
    }

    public GroupChatSession getGroupChatSessionOfConferenceSubscriber(String callId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get GroupChatSession with ConferenceSunscriber '")
                    .append(callId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGroupChatConferenceSubscriberCache.get(callId);
        }
    }

    public boolean isChatSessionAvailable() {
        synchronized (getImsServiceSessionOperationLock()) {
            /*
             * maxChatSessions == 0 means that the allowed number of chat sessions in use is
             * disabled
             */
            int maxChatSessions = mRcsSettings.getMaxChatSessions();
            if (maxChatSessions == 0) {
                return true;
            }

            return mOneToOneChatSessionCache.size() + mGroupChatSessionCache.size() < maxChatSessions;
        }
    }

    /**
     * Assert if it is allowed to initiate a new chat session right now or the allowed limit has
     * been reached.
     * 
     * @param errorMessage
     */
    public void assertAvailableChatSession(String errorMessage) {
        if (!isChatSessionAvailable()) {
            throw new ServerApiMaxAllowedSessionLimitReachedException(errorMessage);
        }
    }

    public void addSession(FileSharingSession session) {
        String fileTransferId = session.getFileTransferId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add FileSharingSession with fileTransfer ID '")
                    .append(fileTransferId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mFileTransferSessionCache.put(fileTransferId, session);
            /*
             * Only FileSharingSessions of type ImsFileSharingSession has a dialog path. Hence add
             * only those type of sessions to the ImsServiceSession cache and add
             * HttpFileTransferSession to ImsServiceSessionWithoutDialogPath cache.
             */
            if (session instanceof ImsFileSharingSession) {
                addImsServiceSession(session);
            } else if (session instanceof HttpFileTransferSession) {
                addImsServiceSessionWithoutDialogPath(session);
            }
        }
    }

    public void removeSession(final FileSharingSession session) {
        final String fileTransferId = session.getFileTransferId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove FileSharingSession with fileTransfer ID '")
                    .append(fileTransferId).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mFileTransferSessionCache.remove(fileTransferId);
                    /*
                     * Only FileSharingSessions of type ImsFileSharingSession has a dialog path.
                     * Hence it is possible to remove only those type of sessions from the
                     * ImsServiceSession cache and remove HttpFileTransferSession from
                     * ImsServiceSessionWithoutDialogPath cache.
                     */
                    if (session instanceof ImsFileSharingSession) {
                        removeImsServiceSession(session);
                    } else if (session instanceof HttpFileTransferSession) {
                        removeImsServiceSessionWithoutDialogPath(session);
                    }
                }
            }
        }.start();
    }

    public FileSharingSession getFileSharingSession(String fileTransferId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get FileSharingSession with fileTransfer ID '")
                    .append(fileTransferId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mFileTransferSessionCache.get(fileTransferId);
        }
    }

    public void addSession(FileUploadSession session) {
        String uploadId = session.getUploadID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add FileUploadSession with upload ID '")
                    .append(uploadId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mFileUploadSessionCache.put(uploadId, session);
        }
    }

    public void removeSession(final FileUploadSession session) {
        final String uploadId = session.getUploadID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove FileUploadSession with upload ID '")
                    .append(uploadId).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mFileUploadSessionCache.remove(uploadId);
                }
            }
        }.start();
    }

    public FileUploadSession getFileUploadSession(String uploadId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get FileUploadSession with upload ID '")
                    .append(uploadId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mFileUploadSessionCache.get(uploadId);
        }
    }

    public boolean isFileTransferSessionAvailable() {
        synchronized (getImsServiceSessionOperationLock()) {
            /*
             * maxFtSessions == 0 means that the checking of allowed number of file transfer
             * sessions in use is disabled
             */
            int maxFileTransferSessions = mRcsSettings.getMaxFileTransferSessions();
            if (maxFileTransferSessions == 0) {
                return true;
            }

            return mFileTransferSessionCache.size() + mFileUploadSessionCache.size() < maxFileTransferSessions;
        }
    }

    public void assertAvailableFileTransferSession(String errorMessage) throws CoreException {
        if (!isFileTransferSessionAvailable()) {
            throw new ServerApiMaxAllowedSessionLimitReachedException(errorMessage);
        }
    }

    public void assertFileSizeNotExceedingMaxLimit(long size, String errorMessage) {
        /*
         * maxFtSize == 0 means that the checking of allowed number of file transfer size in use is
         * disabled
         */
        long maxFileTransferSize = mRcsSettings.getMaxFileTransferSize();
        if (maxFileTransferSize > 0 && size > maxFileTransferSize) {
            throw new ServerApiPersistentStorageException(errorMessage);
        }
    }

    /**
     * Checks if max number of concurrent outgoing file transfer sessions reached
     * 
     * @return boolean
     */
    public boolean isMaxConcurrentOutgoingFileTransfersReached() {
        int nrOfConcurrentOutgoingFileTransferSessions = 0;
        synchronized (getImsServiceSessionOperationLock()) {
            for (FileSharingSession session : mFileTransferSessionCache.values()) {
                if (!session.isInitiatedByRemote()) {
                    nrOfConcurrentOutgoingFileTransferSessions++;
                }
            }
            /*
             * maxConcurrentOutgoingFilrTransferSessions == 0 means that the checking of allowed
             * concurrent number of outgoing file transfers in use is disabled
             */
            int maxConcurrentOutgoingFileTransferSessions = mRcsSettings
                    .getMaxConcurrentOutgoingFileTransferSessions();
            if (maxConcurrentOutgoingFileTransferSessions == 0) {
                return false;
            }
            if (nrOfConcurrentOutgoingFileTransferSessions >= maxConcurrentOutgoingFileTransferSessions) {
                return true;
            }
            nrOfConcurrentOutgoingFileTransferSessions += mFileUploadSessionCache.size();
            return nrOfConcurrentOutgoingFileTransferSessions >= maxConcurrentOutgoingFileTransferSessions;
        }
    }

    /**
     * Initiate a file transfer session
     * 
     * @param fileTransferId File transfer Id
     * @param contact Remote contact identifier
     * @param content Content of file to sent
     * @param fileIcon Content of fileicon
     * @param timestamp the local timestamp when initiating the file transfer
     * @param timestampSent the timestamp sent in payload for the file transfer
     * @param ftProtocol FileTransferProtocol
     * @return File transfer session
     */
    public FileSharingSession initiateFileTransferSession(String fileTransferId, ContactId contact,
            MmContent content, MmContent fileIcon, long timestamp, long timestampSent,
            FileTransferProtocol ftProtocol) {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a file transfer session with contact " + contact + ", file "
                    + content.toString());
        }
        switch (ftProtocol) {
            case HTTP:
                return new OriginatingHttpFileSharingSession(fileTransferId, this, content,
                        contact, fileIcon, UUID.randomUUID().toString(), mCore, mMessagingLog,
                        mRcsSettings, timestamp, timestampSent, mContactManager);
            case MSRP:
                /*
                 * Since in MSRP communication we do not have a timestampSent to be sent in payload,
                 * then we don't need to pass the timestampSent to OriginatingMsrpFileSharingSession
                 */
                return new OriginatingMsrpFileSharingSession(fileTransferId, this, content,
                        contact, fileIcon, mRcsSettings, timestamp, mContactManager);
            default:
                throw new IllegalArgumentException(
                        "Unknown FileTransferProtocol ".concat(ftProtocol.toString()));
        }
    }

    /**
     * Initiate a group file transfer session
     * 
     * @param fileTransferId File transfer Id
     * @param content The file content to be sent
     * @param fileIcon Content of fileicon
     * @param groupChatId Chat contribution ID
     * @param groupChatSessionId GroupChatSession Id
     * @param timestamp the local timestamp when initiating the file transfer
     * @param timestampSent the timestamp sent in payload for the file transfer
     * @return File transfer session
     */
    public FileSharingSession initiateGroupFileTransferSession(String fileTransferId,
            MmContent content, MmContent fileIcon, String groupChatId, String groupChatSessionId,
            long timestamp, long timestampSent) {
        if (sLogger.isActivated()) {
            sLogger.info("Send file " + content.toString() + " to " + groupChatId);
        }

        FileSharingSession session = new OriginatingHttpGroupFileSharingSession(fileTransferId,
                this, content, fileIcon, ImsModule.IMS_USER_PROFILE.getImConferenceUri(),
                groupChatSessionId, groupChatId, UUID.randomUUID().toString(), mCore, mRcsSettings,
                mMessagingLog, timestamp, timestampSent, mContactManager);

        return session;
    }

    /**
     * Receive a MSRP file transfer invitation
     * 
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     * @throws SipPayloadException
     */
    public void receiveMsrpFileTransferInvitation(SipRequest invite, long timestamp)
            throws SipPayloadException {
        boolean logActivated = sLogger.isActivated();
        String assertedId = SipUtils.getAssertedIdentity(invite);
        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(assertedId);
        if (number == null) {
            if (logActivated) {
                sLogger.error("Discard MSRP FileTransfer Invitation: invalid remote ID '"
                        + assertedId + "'");
            }
            sendErrorResponse(invite, Response.DECLINE);
            return;

        }
        if (logActivated) {
            sLogger.info("Receive a file transfer session invitation");
        }
        ContactId remote = ContactUtil.createContactIdFromValidatedData(number);
        // Test if the contact is blocked
        /**
         * Since in MSRP communication we do not have a timestampSent to be extracted from the
         * payload then we need to fake that by using the local timestamp even if this is not the
         * real proper timestamp from the remote side in this case.
         */
        long timestampSent = timestamp;
        if (mContactManager.isBlockedForContact(remote)) {
            if (logActivated) {
                sLogger.debug("Contact " + remote
                        + " is blocked: automatically reject the file transfer invitation");
            }
            handleMsrpFileTransferInvitationRejected(invite, remote,
                    FileTransfer.ReasonCode.REJECTED_SPAM, timestamp, timestampSent);

            // Send a 603 Decline response
            sendErrorResponse(invite, Response.DECLINE);
            return;
        }

        // Test number of sessions
        if (!isFileTransferSessionAvailable()) {
            if (logActivated) {
                sLogger.debug("The max number of file transfer sessions is achieved: reject the invitation");
            }
            handleMsrpFileTransferInvitationRejected(invite, remote,
                    FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp, timestampSent);

            // Send a 603 Decline response
            sendErrorResponse(invite, Response.DECLINE);
            return;
        }
        /*
         * Reject if file is too big or size exceeds device storage capacity. This control should be
         * done on UI. It is done after end user accepts invitation to enable prior handling by the
         * application.
         */
        MmContent content = ContentManager.createMmContentFromSdp(invite, mRcsSettings);
        FileSharingError error = FileSharingSession.isFileCapacityAcceptable(content.getSize(),
                mRcsSettings);
        if (error != null) {
            /*
             * Extract of GSMA specification: If the file is bigger than FT MAX SIZE, a warning
             * message is displayed when trying to send or receive a file larger than the mentioned
             * limit and the transfer will be cancelled (that is at protocol level, the SIP INVITE
             * request will never be sent or an automatic rejection response SIP 403 Forbidden with
             * a Warning header set to 133 Size exceeded will be sent by the entity that detects
             * that the file size is too big to the other end depending on the scenario).
             */
            send403Forbidden(invite, sSizeExceededMsg);
            int errorCode = error.getErrorCode();
            switch (errorCode) {
                case FileSharingError.MEDIA_SIZE_TOO_BIG:
                    handleMsrpFileTransferInvitationRejected(invite, remote,
                            FileTransfer.ReasonCode.REJECTED_MAX_SIZE, timestamp, timestampSent);
                    break;
                case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
                    handleMsrpFileTransferInvitationRejected(invite, remote,
                            FileTransfer.ReasonCode.REJECTED_LOW_SPACE, timestamp, timestampSent);
                    break;
                default:
                    if (sLogger.isActivated()) {
                        sLogger.error("Unexpected error while receiving MSRP file transfer invitation"
                                .concat(Integer.toString(errorCode)));
                    }
            }
            return;
        }

        FileSharingSession session = new TerminatingMsrpFileSharingSession(this, invite, remote,
                mRcsSettings, timestamp, timestampSent, mContactManager);

        mCore.getListener().handleFileTransferInvitation(session, false, remote,
                session.getRemoteDisplayName(), FileTransferData.UNKNOWN_EXPIRATION);

        session.startSession();
    }

    /**
     * Initiate a one-to-one chat session
     * 
     * @param contact Remote contact identifier
     * @param firstMsg First message
     * @return IM session
     */
    public OneToOneChatSession initiateOneToOneChatSession(ContactId contact, ChatMessage firstMsg) {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Initiate 1-1 chat session with ").append(contact)
                    .append(".").toString());
        }
        long timestamp = firstMsg.getTimestamp();
        return new OriginatingOneToOneChatSession(this, contact, firstMsg, mRcsSettings,
                mMessagingLog, timestamp, mContactManager);
    }

    /**
     * Receive a one-to-one chat session invitation
     * 
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    public void receiveOne2OneChatSession(SipRequest invite, long timestamp)
            throws SipPayloadException, SipNetworkException {
        boolean logActivated = sLogger.isActivated();
        /*
         * Invitation will be rejected if it is OMA SIMPLE IM solution but it doesn't contains first
         * message. Reference to spec: Rich Communication Suite 5.1 Advanced Communications Services
         * and Client Specification Version 4.0 Page 187 3.3.4.2 Technical Realization of 1-to-1
         * Chat features when using OMA SIMPLE IM At the technical level the 1-to-1 Chat service
         * implemented using OMA SIMPLE IM extends the concepts described in section 3.3.4.1 with
         * the following concepts: For OMA SIMPLE IM, first message is always included in a
         * CPIM/IMDN wrapper carried in the SIP INVITE request. So the configuration parameter FIRST
         * MSG IN INVITE defined in Table 80 is always set to 1.
         */
        if (!ChatUtils.isContainingFirstMessage(invite)) {
            ImMsgTech mode = mRcsSettings.getImMsgTech();
            switch (mode) {
                case CPM:
                    /* Only reject the invitation when FirstMessageInInvite is true. */
                    if (mRcsSettings.isFirstMessageInInvite()) {
                        if (logActivated) {
                            sLogger.error("Currently in Cpm mode, Reject 1-1 chat invition due to it doesn't"
                                    .concat("carry first message."));
                        }
                        sendErrorResponse(invite, Response.DECLINE);
                        return;
                    }
                case SIMPLE_IM:
                    if (logActivated) {
                        sLogger.error("Currently in SIMPLE_IM mode, Reject 1-1 chat invition due to it doesn't"
                                .concat("carry first message."));
                    }
                    sendErrorResponse(invite, Response.DECLINE);
                    return;
                default:
                    if (sLogger.isActivated()) {
                        sLogger.error("Unexpected ImMsgTech code:".concat(String.valueOf(mode)));
                    }
                    return;
            }
        }

        String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
        ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
        if (remote == null) {
            if (logActivated) {
                sLogger.error("Discard One2OneChatSession: invalid remote ID '" + referredId + "'");
            }
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;

        }
        if (logActivated) {
            sLogger.info("Receive a 1-1 chat session invitation");
        }
        ChatMessage firstMsg = ChatUtils.getFirstMessage(invite, timestamp);
        // Test if the contact is blocked
        if (mContactManager.isBlockedForContact(remote)) {
            if (logActivated) {
                sLogger.debug("Contact " + remote
                        + " is blocked: automatically reject the chat invitation");
            }

            // Save the message in the spam folder
            if (firstMsg != null && !mMessagingLog.isMessagePersisted(firstMsg.getMessageId())) {
                mMessagingLog.addOneToOneSpamMessage(firstMsg);
            }

            // Send message delivery report if requested
            if (mImdnManager.isDeliveryDeliveredReportsEnabled()
                    && ChatUtils.isImdnDeliveredRequested(invite)) {
                // Check notification disposition
                String msgId = ChatUtils.getMessageId(invite);
                if (msgId != null) {
                    String remoteInstanceId = null;
                    ContactHeader inviteContactHeader = (ContactHeader) invite
                            .getHeader(ContactHeader.NAME);
                    if (inviteContactHeader != null) {
                        remoteInstanceId = inviteContactHeader
                                .getParameter(SipUtils.SIP_INSTANCE_PARAM);
                    }
                    // Send message delivery status via a SIP MESSAGE
                    mImdnManager.sendMessageDeliveryStatusImmediately(remote, msgId,
                            ImdnDocument.DELIVERY_STATUS_DELIVERED, remoteInstanceId, timestamp);
                }
            }

            // Send a 486 Busy response
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;
        }

        /*
         * Save the message if it was not already persisted in the DB. We don't have to reject the
         * session if the message was a duplicate one as the session rejection/keeping will be
         * handled in TerminatingOneToOneChatSession.startSession() in an uniform way as according
         * to the defined race conditions in the specification document.
         */
        if (firstMsg != null && !mMessagingLog.isMessagePersisted(firstMsg.getMessageId())) {
            boolean imdnDisplayRequested = mImdnManager
                    .isSendOneToOneDeliveryDisplayedReportsEnabled()
                    && ChatUtils.isImdnDisplayedRequested(invite);
            mMessagingLog.addIncomingOneToOneChatMessage(firstMsg, imdnDisplayRequested);
        }

        // Test number of sessions
        if (!isChatSessionAvailable()) {
            if (logActivated) {
                sLogger.debug("The max number of chat sessions is achieved: reject the invitation");
            }

            // Send a 486 Busy response
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;
        }

        TerminatingOneToOneChatSession session = new TerminatingOneToOneChatSession(this, invite,
                remote, mRcsSettings, mMessagingLog, firstMsg.getTimestamp(), mContactManager);

        mCore.getListener().handleOneOneChatSessionInvitation(session);

        session.startSession();
    }

    /**
     * Initiate an ad-hoc group chat session
     * 
     * @param contacts List of contact identifiers
     * @param subject Subject
     * @param timestamp Local timestamp
     * @return GroupChatSession
     */
    public GroupChatSession initiateAdhocGroupChatSession(Set<ContactId> contacts, String subject,
            long timestamp) {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate an ad-hoc group chat session");
        }

        assertAvailableChatSession("Max number of chat sessions reached");

        Map<ContactId, ParticipantStatus> participants = ChatUtils.getParticipants(contacts,
                ParticipantStatus.INVITING);

        OriginatingAdhocGroupChatSession session = new OriginatingAdhocGroupChatSession(this,
                ImsModule.IMS_USER_PROFILE.getImConferenceUri(), subject, participants,
                mRcsSettings, mMessagingLog, timestamp, mContactManager);

        return session;
    }

    /**
     * Receive ad-hoc group chat session invitation
     * 
     * @param invite Initial invite
     * @param timestamp Local timestamp when got SipRequest
     * @throws SipPayloadException
     */
    public void receiveAdhocGroupChatSession(SipRequest invite, long timestamp)
            throws SipPayloadException {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Receive an ad-hoc group chat session invitation");
        }
        ContactId contact = ChatUtils.getReferredIdentityAsContactId(invite);
        if (contact != null && mContactManager.isBlockedForContact(contact)) {
            if (logActivated) {
                sLogger.debug("Contact " + contact
                        + " is blocked: automatically reject the chat invitation");
            }

            handleGroupChatInvitationRejected(invite, contact, GroupChat.ReasonCode.REJECTED_SPAM,
                    timestamp);

            // Send a 486 Busy response
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;
        }

        // Test number of sessions
        if (!isChatSessionAvailable()) {
            if (logActivated) {
                sLogger.debug("The max number of chat sessions is achieved: reject the invitation");
            }

            handleGroupChatInvitationRejected(invite, contact,
                    GroupChat.ReasonCode.REJECTED_MAX_CHATS, timestamp);

            // Send a 486 Busy response
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;
        }

        /*
         * Get the list of participants from the invite, give them the initial status INVITED as the
         * actual status was not included in the invite.
         */
        Map<ContactId, ParticipantStatus> inviteParticipants = ChatUtils.getParticipants(invite,
                ParticipantStatus.INVITED);

        String remoteUri = ChatUtils.getReferredIdentityAsContactUri(invite);

        TerminatingAdhocGroupChatSession session = new TerminatingAdhocGroupChatSession(this,
                invite, contact, inviteParticipants, remoteUri, mRcsSettings, mMessagingLog,
                timestamp, mContactManager);

        /*--
         * 6.3.3.1 Leaving a Group Chat that is idle
         * In case the user expresses their desire to leave the Group Chat while it is inactive, the device will not offer the user
         * the possibility any more to enter new messages and restart the chat and automatically decline the first incoming INVITE
         * request for the chat with a SIP 603 DECLINE response. Subsequent INVITE requests should not be rejected as they may be
         * received when the user is added again to the Chat by one of the participants.
         */
        boolean reject = mMessagingLog.isGroupChatNextInviteRejected(session.getContributionID());
        if (reject) {
            if (logActivated) {
                sLogger.debug("Chat Id " + session.getContributionID()
                        + " is declined since previously terminated by user while disconnected");
            }
            // Send a 603 Decline response
            sendErrorResponse(invite, Response.DECLINE);
            mMessagingLog.acceptGroupChatNextInvitation(session.getContributionID());
            return;
        }

        mCore.getListener().handleAdhocGroupChatSessionInvitation(session);

        session.startSession();
    }

    /**
     * Rejoin a group chat session
     * 
     * @param chatId Chat ID
     * @return IM session
     */
    public ChatSession rejoinGroupChatSession(String chatId) {
        if (sLogger.isActivated()) {
            sLogger.info("Rejoin group chat session");
        }

        assertAvailableChatSession("Max chat sessions reached");

        // Get the group chat info from database
        GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(chatId);
        if (groupChat == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("Group chat " + chatId + " can't be rejoined: conversation not found");
            }
            throw new ServerApiPersistentStorageException(
                    "Group chat conversation not found in database");
        }
        if (groupChat.getRejoinId() == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("Group chat " + chatId + " can't be rejoined: rejoin ID not found");
            }
            throw new ServerApiPersistentStorageException("Rejoin ID not found in database");
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Rejoin group chat: " + groupChat.toString());
        }
        long timestamp = groupChat.getTimestamp();
        return new RejoinGroupChatSession(this, groupChat, mRcsSettings, mMessagingLog, timestamp,
                mContactManager);
    }

    /**
     * Restart a group chat session
     * 
     * @param chatId Chat ID
     * @return IM session
     */
    public GroupChatSession restartGroupChatSession(String chatId) {
        if (sLogger.isActivated()) {
            sLogger.info("Restart group chat session");
        }

        assertAvailableChatSession("Max chat sessions reached");

        // Get the group chat info from database
        GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(chatId);
        if (groupChat == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("Group chat " + chatId + " can't be restarted: conversation not found");
            }
            throw new ServerApiPersistentStorageException(
                    "Group chat conversation not found in database");
        }
        if (sLogger.isActivated()) {
            sLogger.debug("Restart group chat: " + groupChat.toString());
        }

        Map<ContactId, ParticipantStatus> storedParticipants = groupChat.getParticipants();

        if (storedParticipants.isEmpty()) {
            if (sLogger.isActivated()) {
                sLogger.warn("Group chat " + chatId + " can't be restarted: participants not found");
            }
            throw new ServerApiPersistentStorageException(
                    "No connected group chat participants found in database");
        }
        long timestamp = groupChat.getTimestamp();
        return new RestartGroupChatSession(this, ImsModule.IMS_USER_PROFILE.getImConferenceUri(),
                groupChat.getSubject(), chatId, storedParticipants, mRcsSettings, mMessagingLog,
                timestamp, mContactManager);
    }

    /**
     * Receive a conference notification
     * 
     * @param notify Received notify
     * @param timestamp Local timestamp when got SipRequest
     */
    public void receiveConferenceNotification(SipRequest notify, long timestamp) {
        GroupChatSession session = getGroupChatSessionOfConferenceSubscriber(notify.getCallId());
        if (session != null) {
            session.getConferenceEventSubscriber().receiveNotification(notify, timestamp);
        }
    }

    /**
     * Receive a message delivery status
     * 
     * @param message Received message
     */
    public void receiveMessageDeliveryStatus(SipRequest message) {
        try {
            /*
             * Begin by sending 200 OK, a failure before doing that may cause the sender to re-send
             * the report and if reception fails again we are stuck in a loop.
             */
            SipResponse response = SipMessageFactory.createResponse(message,
                    IdGenerator.getIdentifier(), Response.OK);

            getImsModule().getSipManager().sendSipResponse(response);

            ImdnDocument imdn = ChatUtils.parseCpimDeliveryReport(message.getContent());
            String assertedId = SipUtils.getAssertedIdentity(message);
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(assertedId);

            if (number == null) {
                sLogger.error("Invalid remote ID " + assertedId);
                return;
            }

            ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
            String msgId = imdn.getMsgId();

            String chatId = mMessagingLog.getMessageChatId(msgId);
            if (chatId != null) {
                if (chatId.equals(contact.toString())) {
                    mCore.getListener().handleOneToOneMessageDeliveryStatus(contact, imdn);
                    return;
                }

                mCore.getListener().handleGroupMessageDeliveryStatus(chatId, contact, imdn);
                return;
            }

            chatId = mMessagingLog.getFileTransferChatId(msgId);
            if (chatId != null) {
                if (chatId.equals(contact.toString())) {
                    receiveOneToOneFileDeliveryStatus(contact, imdn);
                    return;
                }

                receiveGroupFileDeliveryStatus(chatId, contact, imdn);
                return;
            }

            sLogger.error(new StringBuilder(
                    "SIP imdn delivery report received referencing a message that was ")
                    .append("not found in our database. Message id ").append(msgId)
                    .append(", ignoring.").toString());

        } catch (SipException e) {
            sLogger.error("Failed to send 200 OK response", e);

        } catch (Exception e) {
            // TODO: This will be changed when ChatUtils.parseCpimDeliveryReport
            // is changed to throw a less generic exception.
            sLogger.error("Failed to parse imdn delivery report.", e);
        }
    }

    /**
     * Receive 1-1 file delivery status
     * 
     * @param contact Contact identifier
     * @param imdn Imdn document
     */
    public void receiveOneToOneFileDeliveryStatus(ContactId contact, ImdnDocument imdn) {
        // Notify the file delivery outside of the chat session
        mCore.getListener().handleOneToOneFileDeliveryStatus(contact, imdn);
    }

    /**
     * Receive group file delivery status
     * 
     * @param chatId Chat Id
     * @param contact Contact identifier
     * @param imdn IM delivery notification document
     */
    public void receiveGroupFileDeliveryStatus(String chatId, ContactId contact, ImdnDocument imdn) {
        mCore.getListener().handleGroupFileDeliveryStatus(chatId, contact, imdn);
    }

    /**
     * Receive S&F push messages
     * 
     * @param invite Received invite
     * @param timestamp Local timestamp when got SipRequest
     * @throws SipPayloadException
     */
    public void receiveStoredAndForwardPushMessages(SipRequest invite, long timestamp)
            throws SipPayloadException {
        boolean logActivated = sLogger.isActivated();
        String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
        ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
        if (remote == null) {
            if (logActivated) {
                sLogger.error("Discard S&F PushMessages: invalid remote ID '" + referredId + "'");
            }
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;
        }
        if (logActivated) {
            sLogger.debug("Receive S&F push messages invitation");
        }
        ChatMessage firstMsg = ChatUtils.getFirstMessage(invite, timestamp);

        // Test if the contact is blocked
        if (mContactManager.isBlockedForContact(remote)) {
            if (logActivated) {
                sLogger.debug("Contact " + remote
                        + " is blocked: automatically reject the S&F invitation");
            }

            // Send a 486 Busy response
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;
        }

        /*
         * Save the message if it was not already persisted in the DB. We don't have to reject the
         * session if the message was a duplicate one as the session rejection/keeping will be
         * handled in TerminatingOneToOneChatSession.startSession() in an uniform way as according
         * to the defined race conditions in the specification document.
         */
        if (firstMsg != null && !mMessagingLog.isMessagePersisted(firstMsg.getMessageId())) {
            boolean imdnDisplayRequested = mImdnManager
                    .isSendOneToOneDeliveryDisplayedReportsEnabled()
                    && ChatUtils.isImdnDisplayedRequested(invite);
            mMessagingLog.addIncomingOneToOneChatMessage(firstMsg, imdnDisplayRequested);
        }

        getStoreAndForwardManager().receiveStoredMessages(invite, remote, timestamp);

    }

    /**
     * Receive S&F push notifications
     * 
     * @param invite Received invite
     * @param timestamp Local timestamp when got SipRequest
     */
    public void receiveStoredAndForwardPushNotifications(SipRequest invite, long timestamp) {
        String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
        ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
        if (remote == null) {
            if (sLogger.isActivated()) {
                sLogger.error("Discard S&F PushNotifications: invalid remote ID '" + referredId
                        + "'");
            }
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;

        }
        if (sLogger.isActivated()) {
            sLogger.debug("Receive S&F push notifications invitation");
        }
        // Test if the contact is blocked
        if (mContactManager.isBlockedForContact(remote)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Contact " + remote
                        + " is blocked: automatically reject the S&F invitation");
            }

            // Send a 486 Busy response
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;
        }

        getStoreAndForwardManager().receiveStoredNotifications(invite, remote, timestamp);
    }

    /**
     * Receive HTTP file transfer invitation
     * 
     * @param invite Received invite
     * @param ftinfo File transfer info document
     * @param timestamp Local timestamp when got SipRequest
     * @throws SipPayloadException
     */
    public void receiveOneToOneHttpFileTranferInvitation(SipRequest invite,
            FileTransferHttpInfoDocument ftinfo, long timestamp) throws SipPayloadException {
        boolean logActivated = sLogger.isActivated();
        String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
        ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
        if (remote == null) {
            if (logActivated) {
                sLogger.error("Discard OneToOne HttpFileTranferInvitation: invalid remote ID '"
                        + referredId + "'");
            }
            sendErrorResponse(invite, Response.DECLINE);
            return;

        }
        if (logActivated) {
            sLogger.info("Receive a single HTTP file transfer invitation");
        }
        CpimMessage cpimMessage = ChatUtils.extractCpimMessage(invite);
        long timestampSent = cpimMessage.getTimestampSent();
        // Test if the contact is blocked
        if (mContactManager.isBlockedForContact(remote)) {
            if (logActivated) {
                sLogger.debug(new StringBuilder("Contact ").append(remote)
                        .append(" is blocked, automatically reject the HTTP File transfer")
                        .toString());
            }
            sendErrorResponse(invite, Response.DECLINE);
            handleHttpFileTransferInvitationRejected(invite, ftinfo, remote,
                    FileTransfer.ReasonCode.REJECTED_SPAM, timestamp, timestampSent);
            return;

        }
        /* Test number of sessions */
        if (!isFileTransferSessionAvailable()) {
            if (logActivated) {
                sLogger.debug("The max number of FT sessions is achieved, reject the HTTP File transfer");
            }
            // Send a 603 Decline response
            sendErrorResponse(invite, Response.DECLINE);
            handleHttpFileTransferInvitationRejected(invite, ftinfo, remote,
                    FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS, timestamp, timestampSent);
            return;
        }

        // Reject if file is too big or size exceeds device storage capacity. This control
        // should be done
        // on UI. It is done after end user accepts invitation to enable prior handling by the
        // application.
        FileSharingError error = FileSharingSession.isFileCapacityAcceptable(ftinfo.getSize(),
                mRcsSettings);
        if (error != null) {
            // Send a 603 Decline response
            sendErrorResponse(invite, Response.DECLINE);
            int errorCode = error.getErrorCode();
            switch (errorCode) {
                case FileSharingError.MEDIA_SIZE_TOO_BIG:
                    handleHttpFileTransferInvitationRejected(invite, ftinfo, remote,
                            FileTransfer.ReasonCode.REJECTED_MAX_SIZE, timestamp, timestampSent);
                    break;
                case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
                    handleHttpFileTransferInvitationRejected(invite, ftinfo, remote,
                            FileTransfer.ReasonCode.REJECTED_LOW_SPACE, timestamp, timestampSent);
                    break;
                default:
                    if (logActivated) {
                        sLogger.error("Unexpected error while receiving HTTP file transfer invitation"
                                .concat(Integer.toString(errorCode)));
                    }
            }
            return;
        }

        // Create and start a chat session
        TerminatingOneToOneChatSession oneToOneChatSession = new TerminatingOneToOneChatSession(
                this, invite, remote, mRcsSettings, mMessagingLog, timestamp, mContactManager);

        // Create and start a new HTTP file transfer session
        DownloadFromInviteFileSharingSession fileSharingSession = new DownloadFromInviteFileSharingSession(
                this, oneToOneChatSession, ftinfo, ChatUtils.getMessageId(invite),
                oneToOneChatSession.getRemoteContact(), oneToOneChatSession.getRemoteDisplayName(),
                mRcsSettings, mMessagingLog, timestamp, timestampSent, mContactManager);
        if (fileSharingSession.getFileicon() != null) {
            try {
                fileSharingSession.downloadFileIcon();
            } catch (IOException e) {
                sLogger.error("Failed to download file icon", e);
                sendErrorResponse(invite, Response.DECLINE);
                handleHttpFileTransferInvitationRejected(invite, ftinfo, remote,
                        FileTransfer.ReasonCode.REJECTED_MEDIA_FAILED, timestamp, timestampSent);
                return;

            }
        }
        CoreListener listener = mCore.getListener();
        listener.handleOneOneChatSessionInitiation(oneToOneChatSession);
        listener.handleOneToOneFileTransferInvitation(fileSharingSession, oneToOneChatSession,
                ftinfo.getExpiration());
        oneToOneChatSession.startSession();
        fileSharingSession.startSession();
    }

    private void handleHttpFileTransferInvitationRejected(SipRequest invite,
            FileTransferHttpInfoDocument ftinfo, ContactId contact, FileTransfer.ReasonCode reason,
            long timestamp, long timestampSent) throws SipPayloadException {
        String fileTransferId = ChatUtils.getMessageId(invite);
        MmContent fileContent = ftinfo.getLocalMmContent();
        FileTransferHttpThumbnail thumbnail = ftinfo.getFileThumbnail();
        MmContent fileIconContent = thumbnail == null ? null : thumbnail
                .getLocalMmContent(fileTransferId);
        mCore.getListener().handleFileTransferInvitationRejected(contact, fileContent,
                fileIconContent, reason, timestamp, timestampSent);
    }

    /**
     * Receive S&F HTTP file transfer invitation
     * 
     * @param invite Received invite
     * @param ftinfo File transfer info document
     * @param timestamp Local timestamp when got SipRequest
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    public void receiveStoredAndForwardOneToOneHttpFileTranferInvitation(SipRequest invite,
            FileTransferHttpInfoDocument ftinfo, long timestamp) throws SipPayloadException,
            SipNetworkException {
        boolean logActivated = sLogger.isActivated();
        String referredId = ChatUtils.getReferredIdentityAsContactUri(invite);
        ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
        if (remote == null) {
            if (logActivated) {
                sLogger.error("Discard S&F OneToOne HttpFileTranfer Invitation. Invalid remote ID "
                        .concat(referredId));
            }
            /* We cannot refuse a S&F File transfer invitation */
            // TODO normally send a deliver to enable transmission of awaiting messages
            return;

        }
        if (logActivated) {
            sLogger.info("Receive a single S&F HTTP file transfer invitation");
        }
        CpimMessage cpimMessage = ChatUtils.extractCpimMessage(invite);
        long timestampSent = cpimMessage.getTimestampSent();
        // Create and start a chat session
        TerminatingStoreAndForwardOneToOneChatMessageSession oneToOneChatSession = new TerminatingStoreAndForwardOneToOneChatMessageSession(
                this, invite, remote, mRcsSettings, mMessagingLog, timestamp, mContactManager);
        CoreListener listener = mCore.getListener();
        listener.handleOneOneChatSessionInitiation(oneToOneChatSession);
        oneToOneChatSession.startSession();

        /* Auto reject if file too big */
        if (isFileSizeExceeded(ftinfo.getSize())) {
            if (logActivated) {
                sLogger.debug("File is too big, reject file transfer invitation");
            }

            // TODO add warning header "xxx Size exceeded"
            oneToOneChatSession.sendErrorResponse(invite, oneToOneChatSession.getDialogPath()
                    .getLocalTag(), InvitationStatus.INVITATION_REJECTED_FORBIDDEN);

            // Close session
            oneToOneChatSession.handleError(new FileSharingError(
                    FileSharingError.MEDIA_SIZE_TOO_BIG));
            return;
        }

        /* Create and start a new HTTP file transfer session from INVITE */
        DownloadFromInviteFileSharingSession filetransferSession = new DownloadFromInviteFileSharingSession(
                this, oneToOneChatSession, ftinfo, ChatUtils.getMessageId(invite),
                oneToOneChatSession.getRemoteContact(), oneToOneChatSession.getRemoteDisplayName(),
                mRcsSettings, mMessagingLog, timestamp, timestampSent, mContactManager);
        if (filetransferSession.getFileicon() != null) {
            try {
                filetransferSession.downloadFileIcon();
            } catch (IOException e) {
                sLogger.error("Failed to download file icon", e);
                oneToOneChatSession.sendErrorResponse(invite, oneToOneChatSession.getDialogPath()
                        .getLocalTag(), InvitationStatus.INVITATION_REJECTED_DECLINE);

                /* Close session */
                oneToOneChatSession.handleError(new FileSharingError(
                        FileSharingError.MEDIA_DOWNLOAD_FAILED, e));
                return;
            }

        }
        listener.handleOneToOneFileTransferInvitation(filetransferSession, oneToOneChatSession,
                ftinfo.getExpiration());
        filetransferSession.startSession();
    }

    /**
     * Check whether file size exceeds the limit
     * 
     * @param size of file
     * @return {@code true} if file size limit is exceeded, otherwise {@code false}
     */
    public boolean isFileSizeExceeded(long size) {
        // Auto reject if file too big
        long maxSize = mRcsSettings.getMaxFileTransferSize();
        if (maxSize > 0 && size > maxSize) {
            return true;
        }

        return false;
    }

    /**
     * Check if the capabilities are valid based on msgCapValidity paramter
     * 
     * @param capabilities
     * @return {@code true} if valid, otherwise {@code false}
     */
    public boolean isCapabilitiesValid(Capabilities capabilities) {
        long msgCapValidityPeriod = mRcsSettings.getMsgCapValidityPeriod();
        if (System.currentTimeMillis() > capabilities.getTimestampOfLastResponse()
                + msgCapValidityPeriod) {
            return false;
        }
        return true;
    }

    /**
     * Removes the group chat composing status from the map
     * 
     * @param chatId
     */
    public void removeGroupChatComposingStatus(final String chatId) {
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mGroupChatComposingStatusToNotify.remove(chatId);
                }
            }
        }.start();
    }

    /**
     * Adds the group chat composing status to the map to enable re-sending upon media session
     * restart
     * 
     * @param chatId the group chat identifier
     * @param status the composing status which failed to be notified
     */
    public void addGroupChatComposingStatus(String chatId, boolean status) {
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatComposingStatusToNotify.put(chatId, status);
        }
    }

    /**
     * Gets the group chat composing status
     * 
     * @param chatId
     * @return the group chat composing status if previous sending failed or null if network is
     *         aligned with client composing status
     */
    public Boolean getGroupChatComposingStatus(String chatId) {
        synchronized (getImsServiceSessionOperationLock()) {
            return mGroupChatComposingStatusToNotify.get(chatId);
        }
    }

    /**
     * Removes the one-to-one chat composing status from the map
     * 
     * @param contact the remote contact
     */
    public void removeOneToOneChatComposingStatus(final ContactId contact) {
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mOneToOneChatComposingStatusToNotify.remove(contact);
                }
            }
        }.start();
    }

    /**
     * Adds the one-to-one chat composing status to the map to enable re-sending upon media session
     * restart
     * 
     * @param contact the remote contact
     * @param status the composing status which failed to be notified
     */
    public void addOneToOneChatComposingStatus(ContactId contact, boolean status) {
        synchronized (getImsServiceSessionOperationLock()) {
            mOneToOneChatComposingStatusToNotify.put(contact, status);
        }
    }

    /**
     * Gets the one-to-one chat composing status
     * 
     * @param contact the remote contact
     * @return the one-to-one chat composing status if previous sending failed or null if network is
     *         aligned with client composing status
     */
    public Boolean getOneToOneChatComposingStatus(ContactId contact) {
        synchronized (getImsServiceSessionOperationLock()) {
            return mOneToOneChatComposingStatusToNotify.get(contact);
        }
    }
}
