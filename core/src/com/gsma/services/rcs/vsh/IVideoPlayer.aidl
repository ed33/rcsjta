package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.vsh.VideoCodec;

/**
 * Video player interface
 */
interface IVideoPlayer {
	void setRemoteInfo(in VideoCodec codec, in String remoteHost, in int remotePort);
	
	int getLocalRtpPort();

	VideoCodec getCodec();

	VideoCodec[] getSupportedCodecs();
}
