package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.sharing.video.IVideoSharing;
import com.gsma.services.rcs.sharing.video.IVideoSharingListener;
import com.gsma.services.rcs.sharing.video.IVideoPlayer;
import com.gsma.services.rcs.sharing.video.VideoSharingServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Video sharing service API
 */
interface IVideoSharingService {

	boolean isServiceRegistered();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	VideoSharingServiceConfiguration getConfiguration();

	List<IBinder> getVideoSharings();
	
	IVideoSharing getVideoSharing(in String sharingId);

	IVideoSharing shareVideo(in ContactId contact, in IVideoPlayer player);

	void addEventListener2(in IVideoSharingListener listener);

	void removeEventListener2(in IVideoSharingListener listener);

	int getServiceVersion();
}