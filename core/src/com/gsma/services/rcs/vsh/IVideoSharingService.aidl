package com.gsma.services.rcs.vsh;

import android.view.Surface;
import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.vsh.IVideoSharing;
import com.gsma.services.rcs.vsh.IVideoSharingListener;
import com.gsma.services.rcs.vsh.IVideoPlayer;
import com.gsma.services.rcs.vsh.VideoDescriptor;
import com.gsma.services.rcs.vsh.VideoSharingServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Video sharing service API
 */
interface IVideoSharingService {

	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	VideoSharingServiceConfiguration getConfiguration();

	List<IBinder> getVideoSharings();
	
	IVideoSharing getVideoSharing(in String sharingId);

	IVideoSharing shareVideo(in ContactId contact, in IVideoPlayer player);

	IVideoSharing shareVideo2(in ContactId contact, in VideoDescriptor descriptor, in Surface surface);

	void addEventListener(in IVideoSharingListener listener);

	void removeEventListener(in IVideoSharingListener listener);

	int getServiceVersion();
}