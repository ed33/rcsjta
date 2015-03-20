package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.sharing.video.VideoCodec;
import com.gsma.services.rcs.sharing.video.IVideoRendererListener;

/**
 * Video renderer interface
 */
interface IVideoRenderer {
	void open(in VideoCodec codec, in String remoteHost, in int remotePort);

	void close();

	void start();

	void stop();

	int getLocalRtpPort();

	VideoCodec getCodec();

	VideoCodec[] getSupportedCodecs();
	
	void addEventListener(in IVideoRendererListener listener);

	void removeEventListener(in IVideoRendererListener listener);
}