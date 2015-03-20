package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.sharing.video.IVideoRenderer;
import com.gsma.services.rcs.sharing.video.VideoCodec;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Video sharing interface
 */
interface IVideoSharing {

	String getSharingId();

	ContactId getRemoteContact();

	VideoCodec getVideoCodec();

	int getState();

	int getReasonCode();

	int getDirection();
	
	void acceptInvitation(IVideoRenderer renderer);

	void rejectInvitation();

	void abortSharing();
}
