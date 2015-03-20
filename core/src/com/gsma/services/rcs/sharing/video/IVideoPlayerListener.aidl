package com.gsma.services.rcs.sharing.video;

/**
 * Video player event listener interface
 */
interface IVideoPlayerListener {
	void onPlayerOpened();

	void onPlayerStarted();

	void onPlayerStopped();

	void onPlayerClosed();

	void onPlayerError(in int error);
}
