/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.orangelabs.rcs.core.ims.service.im.chat;

import com.gsma.services.rcs.RcsCommon.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.provider.messaging.MessagingLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.ContactUtils;

import android.database.Cursor;

import java.util.Set;

/**
 * GroupChatPersistedStorageAccessor helps in retrieving persisted data related
 * to a group chat from the persisted storage. It can utilize caching for such
 * data that will not be changed after creation of the group chat to speed up
 * consecutive access.
 */
public class GroupChatPersistedStorageAccessor {

	private final String mChatId;

	private final MessagingLog mMessagingLog;

	private String mSubject;

	/**
	 * TODO: Change type to enum in CR031 implementation
	 */
	private Integer mDirection;

	private ContactId mContact;

	/**
	 * Constructor
	 * @param chatId
	 * @param messagingLog
	 */
	public GroupChatPersistedStorageAccessor(String chatId, MessagingLog messagingLog) {
		mChatId = chatId;
		mMessagingLog = messagingLog;
	}

	/**
	 * Constructor
	 * @param chatId
	 * @param subject
	 * @param direction
	 * @param messagingLog
	 */
	public GroupChatPersistedStorageAccessor(String chatId, String subject, int direction,
			MessagingLog messagingLog) {
		mChatId = chatId;
		mSubject = subject;
		mDirection = direction;
		mMessagingLog = messagingLog;
	}

	private void cacheData() {
		Cursor cursor = null;
		try {
			cursor = mMessagingLog.getCacheableGroupChatData(mChatId);
			mSubject = cursor.getString(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.SUBJECT));
			mDirection = cursor.getInt(cursor.getColumnIndexOrThrow(ChatLog.GroupChat.DIRECTION));
			String contact = cursor.getString(cursor
					.getColumnIndexOrThrow(ChatLog.GroupChat.CONTACT));
			if (contact != null) {
				mContact = ContactUtils.createContactId(contact);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Get direction
	 * @return direction
	 */
	public int getDirection() {
		/*
		 * Utilizing cache here as direction can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mDirection == null) {
			cacheData();
		}
		return mDirection;
	}

	/**
	 * Get state
	 * @return state
	 */
	public int getState() {
		return mMessagingLog.getGroupChatState(mChatId);
	}

	/**
	 * Get reason code
	 * @return reason code
	 */
	public int getReasonCode() {
		return mMessagingLog.getGroupChatReasonCode(mChatId);
	}

	/**
	 * Get subject
	 * @return subject
	 */
	public String getSubject() {
		/*
		 * Utilizing cache here as subject can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mSubject == null) {
			cacheData();
		}
		return mSubject;
	}

	/**
	 * get remote contact
	 * @return contact
	 */
	public ContactId getRemoteContact() {
		/* Remote contact is null for outgoing group chat */
		if (Direction.OUTGOING == getDirection()) {
			return null;
		}
		/*
		 * Utilizing cache here as remote contact can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mContact == null) {
			cacheData();
		}
		return mContact;
	}

	/**
	 * Get set of participants
	 * @return set of participants
	 */
	public Set<ParticipantInfo> getParticipants() {
		return mMessagingLog.getGroupChatParticipants(mChatId);
	}

	/**
	 * Get maximum number of participants
	 * @return maximum number of participants
	 */
	public int getMaxParticipants() {
		return RcsSettings.getInstance().getMaxChatParticipants();
	}

	/**
	 * Set Group chat state and reason code
	 * @param state
	 * @param reasonCode
	 */
	public void setStateAndReasonCode(int state, int reasonCode) {
		mMessagingLog.setGroupChatStateAndReasonCode(mChatId, state, reasonCode);
	}

	/**
	 * Set status and reason code for message Id
	 * @param msgId
	 * @param status
	 * @param reasonCode
	 */
	public void setMessageStatusAndReasonCode(String msgId, int status, int reasonCode) {
		mMessagingLog.setChatMessageStatusAndReasonCode(msgId, status, reasonCode);
	}

	/**
	 * Set group chat delivery info status and reason code for message Id and contact
	 * @param msgId
	 * @param contact
	 * @param status
	 * @param reasonCode
	 */
	public void setDeliveryInfoStatusAndReasonCode(String msgId, ContactId contact, int status,
			int reasonCode) {
		mMessagingLog.setGroupChatDeliveryInfoStatusAndReasonCode(msgId, contact, status,
				reasonCode);
	}

	/**
	 * Check if message is delivered to all recipients
	 * @param msgId
	 * @return True if message is delivered to all recipients
	 */
	public boolean isDeliveredToAllRecipients(String msgId) {
		return mMessagingLog.isDeliveredToAllRecipients(msgId);
	}

	/**
	 * Check if message is displayed by all recipients
	 * @param msgId
	 * @return True if message is displayed by all recipients
	 */
	public boolean isDisplayedByAllRecipients(String msgId) {
		return mMessagingLog.isDisplayedByAllRecipients(msgId);
	}

	/**
	 * Set group chat rejoin ID
	 * @param rejoinId
	 */
	public void setRejoinId(String rejoinId) {
		mMessagingLog.setGroupChatRejoinId(mChatId, rejoinId);
	}

	/**
	 * Add group chat entry
	 * @param contact
	 * @param subject
	 * @param participants
	 * @param state
	 * @param reasonCode
	 * @param direction
	 */
	public void addGroupChat(ContactId contact, String subject, Set<ParticipantInfo> participants,
			int state, int reasonCode, int direction) {
		mMessagingLog.addGroupChat(mChatId, contact, subject, participants, state, reasonCode,
				direction);
	}

	/**
	 * Add group chat event
	 * @param chatId
	 * @param contact
	 * @param status
	 */
	public void addGroupChatEvent(String chatId, ContactId contact, int status) {
		mMessagingLog.addGroupChatEvent(mChatId,  contact,  status);
	}

	/**
	 * Add group chat message
	 * @param msg
	 * @param direction
	 * @param status
	 * @param reasonCode
	 */
	public void addGroupChatMessage(InstantMessage msg, int direction, int status, int reasonCode) {
		mMessagingLog.addGroupChatMessage(mChatId, msg, direction, status, reasonCode);
	}

	/**
	 * Set reject next group chat invitation
	 */
	public void setRejectNextGroupChatNextInvitation() {
		mMessagingLog.setRejectNextGroupChatNextInvitation(mChatId);
	}

	/**
	 * Set state and reason code for file transfer Id
	 * @param fileTransferId
	 * @param state
	 * @param reasonCode
	 */
	public void setFileTransferStateAndReasonCode(String fileTransferId, int state,
			int reasonCode) {
		mMessagingLog.setFileTransferStateAndReasonCode(fileTransferId, state, reasonCode);
	}
}
