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

package com.orangelabs.rcs.core.ims.service.richcall.video;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.VideoDescriptor;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.provider.sharing.RichCallHistory;

import android.net.Uri;

/**
 * VideoSharingPersistedStorageAccessor helps in retrieving persisted data
 * related to a video share from the persisted storage. It can utilize caching
 * for such data that will not be changed after creation of the video sharing to
 * speed up consecutive access.
 */
public class VideoSharingPersistedStorageAccessor {

	private final String mSharingId;

	private final RichCallHistory mRichCallLog;

	private ContactId mContact;

	/**
	 * TODO: Change type to enum in CR031 implementation
	 */
	private Integer mDirection;
	
	private String mVideoEncoding;
	
	private VideoDescriptor mVideoDescriptor;

	/**
	 * Constructor
	 * @param sharingId
	 * @param richCallLog
	 */
	public VideoSharingPersistedStorageAccessor(String sharingId, RichCallHistory richCallLog) {
		mSharingId = sharingId;
		mRichCallLog = richCallLog;
	}

	/**
	 * Constructor
	 * @param sharingId
	 * @param contact
	 * @param direction
	 * @param richCallLog
	 * @param videoEncoding 
	 * @param height 
	 * @param width 
	 */
	public VideoSharingPersistedStorageAccessor(String sharingId, ContactId contact, int direction,
			RichCallHistory richCallLog, String videoEncoding, int height, int width) {
		mSharingId = sharingId;
		mContact = contact;
		mDirection = direction;
		mRichCallLog = richCallLog;
		mVideoEncoding = videoEncoding;
		mVideoDescriptor = new VideoDescriptor(width, height);
	}

	/**
	 * Gets remote contact
	 * @return remote contact
	 */
	public ContactId getRemoteContact() {
		/*
		 * Utilizing cache here as contact can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mContact == null) {
			mContact = mRichCallLog.getVideoSharingRemoteContact(mSharingId);
		}
		return mContact;
	}

	/**
	 * Gets video sharing session state
	 * @return state
	 */
	public int getState() {
		return mRichCallLog.getVideoSharingState(mSharingId);
	}

	/**
	 * Gets video sharing reason code
	 * @return reason code
	 */
	public int getReasonCode() {
		return mRichCallLog.getVideoSharingReasonCode(mSharingId);
	}

	/**
	 * Gets direction
	 * @return direction
	 */
	public int getDirection() {
		/*
		 * Utilizing cache here as direction can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mDirection == null) {
			mDirection = mRichCallLog.getVideoSharingDirection(mSharingId);
		}
		return mDirection;
	}

	/**
	 * Sets state and reason code
	 * @param state
	 * @param reasonCode
	 */
	public void setStateAndReasonCode(int state, int reasonCode) {
		mRichCallLog.setVideoSharingStateAndReasonCode(mSharingId, state, reasonCode);
	}

	/**
	 * Sets duration
	 * @param duration
	 */
	public void setDuration(long duration) {
		mRichCallLog.setVideoSharingDuration(mSharingId, duration);
	}

	/**
	 * Add video sharing session
	 * @param contact
	 * @param direction
	 * @param content
	 * @param state
	 * @param reasonCode
	 * @return the URI of the newly inserted item
	 */
	public Uri addVideoSharing(ContactId contact, int direction, VideoContent content, int state,
			int reasonCode) {
		return mRichCallLog.addVideoSharing(mSharingId, contact, direction, content, state,
				reasonCode);
	}

	/**
	 * Gets video encoding
	 * @return video encoding
	 */
	public String getVideoEncoding() {
		/*
		 * Utilizing cache here as video encoding can't be changed in persistent
		 * storage after entry insertion anyway so no need to query for it
		 * multiple times.
		 */
		if (mVideoEncoding == null) {
			mVideoEncoding = mRichCallLog.getVideoSharingEncoding(mSharingId);
		}
		return mVideoEncoding;
	}

	/**
	 * Gets video descriptor
	 * @return descriptor
	 */
	public VideoDescriptor getVideoDescriptor() {
		if (mVideoDescriptor == null) {
			mVideoDescriptor = mRichCallLog.getVideoSharingDescriptor(mSharingId);
		}
		return mVideoDescriptor;
	}
}
