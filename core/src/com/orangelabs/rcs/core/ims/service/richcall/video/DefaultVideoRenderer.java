/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.core.ims.service.richcall.video;

import java.io.IOException;
import java.lang.reflect.Method;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.SystemClock;
import android.view.Surface;

import com.gsma.services.rcs.vsh.VideoCodec;
import com.gsma.services.rcs.vsh.VideoPlayer;
import com.orangelabs.rcs.core.ims.protocol.rtp.DummyPacketGenerator;
import com.orangelabs.rcs.core.ims.protocol.rtp.VideoRtpReceiver;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.decoder.NativeH264Decoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1b;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaOutput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.VideoSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.orangelabs.rcs.platform.network.DatagramConnection;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Live video RTP renderer based on H264 QCIF format
 *
 * @author Jean-Marc AUFFRET
 */
public class DefaultVideoRenderer extends VideoPlayer implements RtpStreamListener {
	/**
	 * The logger
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	/**
     * Camera of the device
     */
    private Camera camera;

    /**
     * Number of cameras
     */
    private int numberOfCameras = 1;
    
    /**
     * Opened camera id
     */
    private CameraOptions openedCameraId = CameraOptions.FRONT;

    /**
     * Camera preview started flag
     */
    private boolean cameraPreviewRunning = false;

    /**
     * Video width
     */
    private int videoWidth = H264Config.QCIF_WIDTH;
    
    /**
     * Video height
     */
    private int videoHeight = H264Config.QCIF_HEIGHT;
	
    /**
     * Default video codec
     */
    private VideoCodec defaultVideoCodec;

    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * RTP receiver session
     */
    private VideoRtpReceiver rtpReceiver = null;

    /**
     * RTP dummy packet generator
     */
    private DummyPacketGenerator rtpDummySender = null;

    /**
     * RTP media output
     */
    private MediaRtpOutput rtpOutput = null;

    /**
     * Is player opened
     */
    private boolean opened = false;

    /**
     * Is player started
     */
    private boolean started = false;

    /**
     * Video start time
     */
    private long videoStartTime = 0L;

    /**
     * Video surface
     */
    private Surface surface = null;

    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection temporaryConnection = null;

    /**
     * Orientation header id.
     */
    private int orientationHeaderId = -1;

    /**
     * Remote host
     */
    private String remoteHost;
    
    /**
     * Remote port
     */
    private int remotePort;
    
    /**
     * Constructor
     * 
     * @param surface Surface
     */
    public DefaultVideoRenderer(Surface surface) {
    	// Set surface view
    	this.surface = surface;
    	
        // Get camera info
        numberOfCameras = getNumberOfCameras();
        if (logger.isActivated()) {
        	logger.debug("Number of camera: " + numberOfCameras);
        }

		// Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);
        
        // Set the default media codec
    	defaultVideoCodec = new VideoCodec(H264Config.CODEC_NAME,
    			H264VideoFormat.PAYLOAD,
                H264Config.CLOCK_RATE,
                15,
                96000,
                H264Config.QCIF_WIDTH, 
                H264Config.QCIF_HEIGHT,
    			H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1b.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=" + JavaPacketizer.H264_ENABLED_PACKETIZATION_MODE);
    }
    
    /**
	 * Set the remote info
	 * 
	 * @param codec Video codec
	 * @param remoteHost Remote RTP host
	 * @param remotePort Remote RTP port
	 */
	public void setRemoteInfo(VideoCodec codec, String remoteHost, int remotePort) {
        // Set the video codec
        defaultVideoCodec = codec;		
        
        // Set remote host and port
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;        
	}
    
	/**
	 * Returns the list of codecs supported by the player
	 * 
	 * @return List of codecs
	 */
	public VideoCodec[] getSupportedCodecs() {
		VideoCodec[] list = new VideoCodec[1];
		list[0] = defaultVideoCodec;
		return list;
	}    
    
	/**
	 * Returns the current codec
	 * 
	 * @return Codec
	 */
	public VideoCodec getCodec() {
		return defaultVideoCodec;
	}
	
	/**
	 * Opens the player and prepares resources
	 */
	public synchronized void open() {
		if (opened) {
            // Already opened
            return;
        }

        try {
            // Start the camera
    		openCamera();
        	
            // Init the video decoder
            int result = NativeH264Decoder.InitDecoder();
            if (result != 0) {
            	// Decoder init failed
        		// TODO eventListener.onPlayerError();
                return;
            }

            // Init the RTP layer
            releasePort();
            rtpReceiver = new VideoRtpReceiver(localRtpPort);
            rtpDummySender = new DummyPacketGenerator();
            rtpOutput = new MediaRtpOutput();
            rtpOutput.open();
            rtpReceiver.prepareSession(remoteHost, remotePort, orientationHeaderId, rtpOutput, new H264VideoFormat(), this);
            rtpDummySender.prepareSession(remoteHost, remotePort, rtpReceiver.getInputStream());
            rtpDummySender.startSession();
        } catch (Exception e) {
        	// RTP failed
        	// TODO eventListener.onPlayerError();
            return;
        }

        // Player is opened
        opened = true;
        // TODO eventListener.onPlayerOpened();
    }

	/**
	 * Closes the player and deallocates resources
	 */
	public synchronized void close() {
        if (!opened) {
            // Already closed
            return;
        }

        // Close the RTP layer
        rtpOutput.close();
        rtpReceiver.stopSession();
        rtpDummySender.stopSession();

        try {
            // Close the video decoder
            NativeH264Decoder.DeinitDecoder();
        } catch (UnsatisfiedLinkError e) {
        	e.printStackTrace();
        }

        // Player is closed
        opened = false;
        // TODO eventListener.onPlayerClosed();
    }

	/**
	 * Starts the player
	 */
	public synchronized void start() {
		if (!opened) {
            // Player not opened
            return;
        }

        if (started) {
            // Already started
            return;
        }
        
        // Start RTP layer
        rtpReceiver.startSession();

        // Player is started
        videoStartTime = SystemClock.uptimeMillis();
        started = true;
        // TODO eventListener.onPlayerStarted();
    }

	/**
	 * Stops the player
	 */
	public synchronized void stop() {
		if (!started) {
            return;
        }

        // Stop RTP layer
        if (rtpReceiver != null) {
            rtpReceiver.stopSession();
        }
        if (rtpDummySender != null) {
            rtpDummySender.stopSession();
        }
        if (rtpOutput != null) {
            rtpOutput.close();
        }

        // Player is stopped
        started = false;
        videoStartTime = 0L;
        // TODO eventListener.onPlayerStopped();
    }
    
    /*---------------------------------------------------------------------*/
    
    /**
     * Return the video start time
     *
     * @return Milliseconds
     */
    public long getVideoStartTime() {
        return videoStartTime;
    }

    /**
     * Returns the local RTP port
     *
     * @return Port
     */
    public int getLocalRtpPort() {
        return localRtpPort;
    }

    /**
     * Reserve a port
     *
     * @param port Port to reserve
     */
    private void reservePort(int port) {
        if (temporaryConnection == null) {
            try {
                temporaryConnection = NetworkFactory.getFactory().createDatagramConnection();
                temporaryConnection.open(port);
            } catch (IOException e) {
                temporaryConnection = null;
            }
        }
    }

    /**
     * Release the reserved port.
     */
    private void releasePort() {
        if (temporaryConnection != null) {
            try {
                temporaryConnection.close();
            } catch (IOException e) {
                temporaryConnection = null;
            }
        }
    }

    /**
     * Is player opened
     *
     * @return Boolean
     */
    public boolean isOpened() {
        return opened;
    }

    /**
     * Is player started
     *
     * @return Boolean
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Notify RTP aborted
     */
    public void rtpStreamAborted() {
    	// TODO eventListener.onPlayerError();
    }

    /**
     * Set extension header orientation id
     *
     * @param headerId extension header orientation id
     */
    public void setOrientationHeaderId(int headerId) {
        this.orientationHeaderId = headerId;
    }
    
    /**
     * Media RTP output
     */
    private class MediaRtpOutput implements MediaOutput {
        /**
         * Bitmap frame
         */
        private Bitmap rgbFrame = null;

        /**
         * Video orientation
         */
        private VideoOrientation videoOrientation = new VideoOrientation(CameraOptions.BACK, Orientation.NONE);

        /**
         * Frame dimensions
         * Just 2 - width and height
         */
        private int decodedFrameDimensions[] = new int[2];

        /**
         * Constructor
         */
        public MediaRtpOutput() {
            // Init rgbFrame with a default size
            rgbFrame = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        }

        /**
         * Open the renderer
         */
        public void open() {
            // Nothing to do
        }

        /**
         * Close the renderer
         */
        public void close() {
        }

        /**
         * Write a media sample
         *
         * @param sample Sample
         */
        public void writeSample(MediaSample sample) {
            rtpDummySender.incomingStarted();

            // Init orientation
            VideoOrientation orientation = ((VideoSample)sample).getVideoOrientation();
            if (orientation != null) {
                this.videoOrientation = orientation;
            }

            int[] decodedFrame = NativeH264Decoder.DecodeAndConvert(sample.getData(), videoOrientation.getOrientation().getValue(), decodedFrameDimensions);
            if (NativeH264Decoder.getLastDecodeStatus() == 0) {
                if ((surface != null) && (decodedFrame.length > 0)) {
                    // Init RGB frame with the decoder dimensions
                	if ((rgbFrame.getWidth() != decodedFrameDimensions[0]) || (rgbFrame.getHeight() != decodedFrameDimensions[1])) {
                        rgbFrame = Bitmap.createBitmap(decodedFrameDimensions[0], decodedFrameDimensions[1], Bitmap.Config.RGB_565);
                        // TODO eventListener.onPlayerResized(decodedFrameDimensions[0], decodedFrameDimensions[1]);
                    }

                	// Set data in image
                    rgbFrame.setPixels(decodedFrame, 0, decodedFrameDimensions[0], 0, 0,
                            decodedFrameDimensions[0], decodedFrameDimensions[1]);
                    // TODO surface.setImage(rgbFrame);
            	}
            }
        }
    }
    
    /*---------------------------------------------------------------------*/
    
    /**
     * Get Camera "open" Method
     *
     * @return Method
     */
    private Method getCameraOpenMethod() {
        ClassLoader classLoader = DefaultVideoRenderer.class.getClassLoader();
        Class cameraClass = null;
        try {
            cameraClass = classLoader.loadClass("android.hardware.Camera");
            try {
                return cameraClass.getMethod("open", new Class[] {
                    int.class
                });
            } catch (NoSuchMethodException e) {
                return null;
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    /**
     * Open the camera
     *
     * @param cameraId Camera ID
     */
    private void openCamera(CameraOptions cameraId) {
        Method method = getCameraOpenMethod();
        if (numberOfCameras > 1 && method != null) {
            try {
                camera = (Camera)method.invoke(camera, new Object[] {
                    cameraId.getValue()
                });
                openedCameraId = cameraId;
            } catch (Exception e) {
                camera = Camera.open();
                openedCameraId = CameraOptions.BACK;
            }
        } else {
            camera = Camera.open();
        }
    }
    
    /**
     * Get Camera "numberOfCameras" Method
     *
     * @return Method
     */
    private Method getCameraNumberOfCamerasMethod() {
        ClassLoader classLoader = DefaultVideoRenderer.class.getClassLoader();
        Class cameraClass = null;
        try {
            cameraClass = classLoader.loadClass("android.hardware.Camera");
            try {
                return cameraClass.getMethod("getNumberOfCameras", (Class[])null);
            } catch (NoSuchMethodException e) {
                return null;
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Get number of cameras
     *
     * @return number of cameras
     */
    private int getNumberOfCameras() {
        Method method = getCameraNumberOfCamerasMethod();
        if (method != null) {
            try {
                Integer ret = (Integer)method.invoke(null, (Object[])null);
                return ret.intValue();
            } catch (Exception e) {
                return 1;
            }
        } else {
            return 1;
        }
    }    

    /**
     * Open the camera
     */
    private synchronized void openCamera() {
        if (camera == null) {
            if (logger.isActivated()) {
            	logger.debug("Open the camera");
            }

            // Open camera
            openCamera(openedCameraId);

            // Start camera
            // TODO camera.setPreviewCallback(videoPlayer);
            // TODO startCameraPreview();
        }
    }    
    
    /**
     * Close the camera
     */
    private synchronized void closeCamera() {
	    if (camera != null) {
            if (logger.isActivated()) {
            	logger.debug("Close the camera");
            }

            camera.setPreviewCallback(null);
	        if (cameraPreviewRunning) {
	            cameraPreviewRunning = false;
	            camera.stopPreview();
	        }
	        camera.release();
	        camera = null;
	    }
    }

}
