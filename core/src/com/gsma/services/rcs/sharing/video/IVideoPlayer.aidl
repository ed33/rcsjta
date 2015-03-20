package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.sharing.video.VideoCodec;
import com.gsma.services.rcs.sharing.video.IVideoPlayerListener;

/**
 * Video player interface
 */
interface IVideoPlayer {
	void open(in VideoCodec codec, in String remoteHost, in int remotePort);
	
	void close();

	void start();

	void stop();

	int getLocalRtpPort();

	VideoCodec getCodec();

	VideoCodec[] getSupportedCodecs();

	void addEventListener(in IVideoPlayerListener listener);

	void removeEventListener(in IVideoPlayerListener listener);	
}
