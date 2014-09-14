package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.vsh.VideoDescriptor;

/**
 * Callback methods for video sharing events
 */
interface IVideoSharingListener {

	void onVideoSharingStateChanged(in ContactId contact, in String sharingId, int state);

	void onVideoDescriptorChanged(in ContactId contact, in String sharingId, in VideoDescriptor descriptor);
}