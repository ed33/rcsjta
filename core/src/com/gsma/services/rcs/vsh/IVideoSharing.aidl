package com.gsma.services.rcs.vsh;

import android.view.Surface;
import com.gsma.services.rcs.vsh.IVideoPlayer;
import com.gsma.services.rcs.vsh.VideoCodec;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Video sharing interface
 */
interface IVideoSharing {

	String getSharingId();

	ContactId getRemoteContact();

	VideoCodec getVideoCodec();

	int getState();

	int getDirection();
	
	void acceptInvitation(in IVideoPlayer player);

	void acceptInvitation2(in Surface surface);

	void rejectInvitation();

	void abortSharing();
}
