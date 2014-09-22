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

import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.SystemClock;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.vsh.VideoCodec;
import com.gsma.services.rcs.vsh.VideoDescriptor;
import com.gsma.services.rcs.vsh.VideoPlayer;
import com.orangelabs.rcs.core.ims.protocol.rtp.VideoRtpSender;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264Encoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264EncoderParams;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1b;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaException;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.VideoSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.orangelabs.rcs.platform.network.DatagramConnection;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.service.api.VideoSharingServiceImpl;
import com.orangelabs.rcs.utils.FifoBuffer;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Live RTP video player based on H264 QCIF format
 */
public class DefaultVideoPlayer extends VideoPlayer implements Camera.PreviewCallback, RtpStreamListener {
	
    /**
     * Camera of the device
     */
    private Camera camera;
    
    /**
     * Opened camera id
     */
    private CameraOptions openedCameraId = CameraOptions.FRONT;

    /**
     * Camera preview started flag
     */
    private boolean cameraPreviewRunning = false;

    /**
     * Number of cameras
     */
    private int numberOfCameras = 1;    
	
    /**
     * Default video codec
     */
    private VideoCodec defaultVideoCodec;

    /**
     * Is player opened
     */
    private boolean opened = false;

    /**
     * Is player started
     */
    private boolean started = false;    
    
    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * RTP sender session
     */
    private VideoRtpSender rtpSender = null;

    /**
     * RTP media input
     */
    private MediaRtpInput rtpInput = null;

    /**
     * Video start time
     */
    private long videoStartTime = 0L;

    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection temporaryConnection = null;

    /**
     * NAL SPS
     */
    private byte[] sps = new byte[0];
    
    /**
     * NAL PPS
     */
    private byte[] pps = new byte[0];

    /**
     * Timestamp increment
     */
    private int timestampInc;
    
    /***
     * Current time stamp
     */
    private long timeStamp = 0;
    
    /**
	 * NAL initialization
	 */
	private boolean nalInit = false;

    /**
     * NAL repeat
     */
    private int nalRepeat = 0;

    /**
     * NAL repeat MAX value
     */
    private static final int NALREPEATMAX = 20;

    /**
     * Scaling factor for encoding
     */
    private float scaleFactor = 1;
   
    /**
     * Mirroring (horizontal and vertical) for encoding
     */
    private boolean mirroring = false;

    /**
     * Video Orientation
     */
    private Orientation mOrientation = Orientation.NONE;    
    
    /**
     * Orientation header id.
     */
    private int orientationHeaderId = -1;

    /**
     * Camera ID
     */
    private int cameraId = CameraOptions.BACK.getValue();

    /**
     * Frame process
     */
    private FrameProcess frameProcess;

    /**
     * Frame buffer
     */
    private FrameBuffer frameBuffer = new FrameBuffer();
    
    /**
     * Remote host
     */
    private String remoteHost;
    
    /**
     * Remote port
     */
    private int remotePort;
    
    /**
     * Video descriptor
     */
    private VideoDescriptor descriptor;
    
    /**
     * Video surface
     */
    private Surface surface;
    
	/**
	 * The logger
	 */
	private static final  Logger logger = Logger.getLogger(VideoSharingServiceImpl.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param descriptor Video descriptor
     * @param surface Video surface
     */
    public DefaultVideoPlayer(VideoDescriptor descriptor, Surface surface) {
    	// Set the video inputs
    	this.descriptor = descriptor;
    	this.surface = surface;
    	// TODO: use descriptor and surface
    	
        // Get camera info
        numberOfCameras = getNumberOfCameras();    	
    	
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
     * Returns the local RTP port
     *
     * @return Port
     */
    public int getLocalRtpPort() {
        return localRtpPort;
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
	 * 
	 * @return Boolean
	 */
	public synchronized boolean open() {
        if (opened) {
            // Already opened
            return true;
        }
        
		if (logger.isActivated()) {
			logger.debug("Open the default player");
		}
        
        // Init video encoder
        try {
            timestampInc = (int)(90000 / defaultVideoCodec.getFrameRate());
            NativeH264EncoderParams nativeH264EncoderParams = new NativeH264EncoderParams();

            // Codec dimensions
            nativeH264EncoderParams.setFrameWidth(defaultVideoCodec.getVideoWidth());
            nativeH264EncoderParams.setFrameHeight(defaultVideoCodec.getVideoHeight());
            nativeH264EncoderParams.setFrameRate(defaultVideoCodec.getFrameRate());
            nativeH264EncoderParams.setBitRate(defaultVideoCodec.getBitRate());

            // Codec profile and level
            nativeH264EncoderParams.setProfilesAndLevel(defaultVideoCodec.getParameters());

            // Codec settings optimization
            nativeH264EncoderParams.setEncMode(NativeH264EncoderParams.ENCODING_MODE_STREAMING);
            nativeH264EncoderParams.setSceneDetection(false);
            nativeH264EncoderParams.setIFrameInterval(15);

            int result = NativeH264Encoder.InitEncoder(nativeH264EncoderParams);
            if (result != 0) {
            	// Encoder init has failed
        		if (logger.isActivated()) {
        			logger.error("Encoder init has failed");
        		}
               return false;
            }
        } catch (UnsatisfiedLinkError e) {
        	// Native encoder not found
    		if (logger.isActivated()) {
    			logger.error("Native encoder not found");
    		}
            return false;
        }

        // Init the RTP layer
        try {
            releasePort();
            rtpSender = new VideoRtpSender(new H264VideoFormat(), localRtpPort);
            rtpInput = new MediaRtpInput();
            rtpInput.open();
            rtpSender.prepareSession(rtpInput, remoteHost, remotePort, this);
        } catch (Exception e) {
        	// RTP failure
    		if (logger.isActivated()) {
    			logger.error("RTP failure", e);
    		}
            return false;
        }
        
        // Close the camera
        openCamera();        
		
        // Player is opened
        opened = true;
		if (logger.isActivated()) {
			logger.debug("Default player is open");
		}
		return opened;
    }

	/**
	 * Closes the player and deallocates resources
	 * 
	 * @throws JoynServiceException
	 */
	public synchronized void close() {
        if (!opened) {
            // Already closed
            return;
        }
        
        // Close the camera
        closeCamera();        
        
        // Close the RTP layer
        rtpInput.close();
        rtpSender.stopSession();

        try {
            // Close the video encoder
            NativeH264Encoder.DeinitEncoder();
        } catch (UnsatisfiedLinkError e) {
        	e.printStackTrace();
        }

        // Player is closed
        opened = false;
        
		if (logger.isActivated()) {
			logger.debug("Default player is closed");
		}
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

		if (logger.isActivated()) {
			logger.debug("Start the default player");
		}
        
        // Init NAL
        if (!initNAL()) {
            return;
        }
        nalInit = false;

        timeStamp = 0;
        nalInit = false;
        nalRepeat = 0;

        // Start RTP layer
        rtpSender.startSession();
        
        // Player is started
        videoStartTime = SystemClock.uptimeMillis();
        started = true;
        frameProcess = new FrameProcess((int)defaultVideoCodec.getFrameRate());
        frameProcess.start();
        
		if (logger.isActivated()) {
			logger.debug("Default player is started");
		}
    }

	/**
	 * Stops the player
	 */
	public synchronized void stop() {
        if (!opened) {
            // Player not opened
            return;
        }

        if (!started) {
            // Already stopped
            return;
        }

        // Player is stopped
        videoStartTime = 0L;
        started = false;
        try {
            frameProcess.interrupt();
        } catch (Exception e) {
            // Nothing to do
        }
        
		if (logger.isActivated()) {
			logger.debug("Default player is stopped");
		}
    }

    /*---------------------------------------------------------------------*/

    /**
     * Reserve a port.
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
     * Return the video start time
     *
     * @return Milliseconds
     */
    public long getVideoStartTime() {
        return videoStartTime;
    }

    /**
     * Init sps and pps
     *
     * @return true if done
     */
    private boolean initNAL() {
        boolean ret = initOneNAL();
        if (ret) {
            ret = initOneNAL();
        }
        return ret;
    }

    /**
     * Init sps or pps
     *
     * @return true if done
     */
    private boolean initOneNAL() {
        byte[] nal = NativeH264Encoder.getNAL();
        if ((nal != null) && (nal.length > 0)) {
            int type = (nal[0] & 0x1f);
            if (type == JavaPacketizer.AVC_NALTYPE_SPS) {
                sps = nal;
                return true;
            } else if (type == JavaPacketizer.AVC_NALTYPE_PPS) {
                pps = nal;
                return true;
            }
        }
        return false;
    }

    /**
     * Get video width
     *
     * @return Width
     */
    public int getVideoWidth() {
        if (defaultVideoCodec == null) {
            return H264Config.VIDEO_WIDTH;
        } else {
            return defaultVideoCodec.getVideoWidth();
        }
    }

    /**
     * Get video height
     *
     * @return Height
     */
    public int getVideoHeight() {
        if (defaultVideoCodec == null) {
            return H264Config.VIDEO_HEIGHT;
        } else {
            return defaultVideoCodec.getVideoHeight();
        }
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
     * Set camera ID
     *
     * @param cameraId Camera ID
     */
    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    /**
     * Set video orientation
     *
     * @param orientation
     */
    public void setOrientation(Orientation orientation) {
        mOrientation = orientation;
    }

    /**
     * Set the mirroring value
     *
     * @param mirroring New mirroring value
     */
    public void setMirroring(boolean mirroring) {
        this.mirroring = mirroring;
    }

    /**
     * Notify RTP aborted
     */
    public void rtpStreamAborted() {
    	// RTP failure
		// TODO
    }

    /**
     * Preview frame from the camera
     *
     * @param data Frame
     * @param camera Camera
     */
    public void onPreviewFrame(byte[] data, Camera camera) {
		if (logger.isActivated()) {
			logger.debug("onPreviewFrame callaback");
		}

		if (!started) {
			return;
		}
		
		if (logger.isActivated()) {
			logger.debug("Add frame to buffer " + data.length);
		}
    	
		frameBuffer.setData(data);
    };

    /**
     * encode a buffer and add in RTP input
     *
     * @param data
     */
    private void encode(byte[] data) {
        // Send SPS/PPS if necessary
        nalRepeat++;
        if (nalRepeat > NALREPEATMAX) {
            nalInit = false;
            nalRepeat = 0;
        }
        if (!nalInit) {
            rtpInput.addFrame(sps, timeStamp);
            timeStamp += timestampInc;

            rtpInput.addFrame(pps, timeStamp);
            timeStamp += timestampInc;
            
            nalInit = true;
        } 

        // Encode frame
        byte[] encoded;
        if (frameBuffer.dataSrcWidth != 0 && frameBuffer.dataSrcHeight != 0) {
            encoded = NativeH264Encoder.ResizeAndEncodeFrame(data, timeStamp, mirroring, frameBuffer.dataSrcWidth, frameBuffer.dataSrcHeight);
        } else {
            encoded = NativeH264Encoder.EncodeFrame(data, timeStamp, mirroring, frameBuffer.dataScaleFactor);
        }
        int encodeResult = NativeH264Encoder.getLastEncodeStatus();
        if ((encodeResult == 0) && (encoded.length > 0)) {
            VideoOrientation videoOrientation = null;
            if (orientationHeaderId > 0 ) {
                videoOrientation = new VideoOrientation(
                        orientationHeaderId,
                        CameraOptions.convert(cameraId),
                        mOrientation);
            }
            rtpInput.addFrame(encoded, timeStamp, videoOrientation);
            timeStamp += timestampInc;
        }
    }

    /**
     * Frame process
     */
    private class FrameProcess extends Thread {
        
        /**
         * Time between two frame
         */
        private int interframe = 1000 / 15;

        /**
         * Constructor
         *
         * @param framerate
         */
        public FrameProcess(int framerate) {
            super();
            interframe = 1000 / framerate;
        }

        @Override
        public void run() {
            byte[] frameData = null;
            while (started) {
                long time = System.currentTimeMillis();

                // Encode
                frameData = frameBuffer.getData();
                if (frameData != null) {
                    encode(frameData);
                }

                // Sleep between frames if necessary
                long delta = System.currentTimeMillis() - time;
                if (delta < interframe) {
                    try {
                        Thread.sleep((interframe - delta) - (((interframe - delta) * 10) / 100));
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    /**
     * Frame buffer
     */
    private class FrameBuffer {
        /**
         * Data
         */
        private byte[] data = null;

        /**
         * Scaling factor for encoding
         */
        public float dataScaleFactor = 1;

        /**
         * Source Width - used for resizing
         */
        public int dataSrcWidth = 0;

        /**
         * Source Height - used for resizing
         */
        public int dataSrcHeight = 0;

        /**
         * Get the data
         *
         * @return data
         */
        public synchronized byte[] getData() {
            return data;
        }

        /**
         * Set the data
         *
         * @param data
         */
        public synchronized void setData(byte[] data) {
            this.data = data;

            // Update resizing / scaling values
            this.dataScaleFactor = scaleFactor;
            this.dataSrcWidth = defaultVideoCodec.getVideoWidth();
            this.dataSrcHeight = defaultVideoCodec.getVideoHeight();
        }
    }

    /**
     * Media RTP input
     */
    private static class MediaRtpInput implements MediaInput {
        /**
         * Received frames
         */
        private FifoBuffer fifo = null;

        /**
         * Constructor
         */
        public MediaRtpInput() {
        }

        /**
         * Add a new video frame
         *
         * @param data Data
         * @param timestamp Timestamp
         * @param marker Marker bit 
         */
        public void addFrame(byte[] data, long timestamp, VideoOrientation videoOrientation) {
            if (fifo != null) {
                VideoSample sample = new VideoSample(data, timestamp, videoOrientation);
                fifo.addObject(sample);
            }
        }

        /**
         * Add a new video frame
         *
         * @param data Data
         * @param timestamp Timestamp
         * @param marker Marker bit 
         */
        public void addFrame(byte[] data, long timestamp) {
            addFrame(data, timestamp, null);
        }

        /**
         * Open the player
         */
        public void open() {
            fifo = new FifoBuffer();
        }

        /**
         * Close the player
         */
        public void close() {
            if (fifo != null) {
                fifo.close();
                fifo = null;
            }
        }

        /**
         * Read a media sample (blocking method)
         *
         * @return Media sample
         * @throws MediaException
         */
        public VideoSample readSample() throws MediaException {
            try {
                if (fifo != null) {
                    return (VideoSample)fifo.getObject();
                } else {
                    throw new MediaException("Media input not opened");
                }
            } catch (Exception e) {
                throw new MediaException("Can't read media sample");
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
     * Start the camera preview
     */
    private void startCameraPreview() {
    	if (camera != null) {
            // Camera settings
            Camera.Parameters p = camera.getParameters();
            p.setPreviewFormat(PixelFormat.YCbCr_420_SP);

            // Orientation
            p.setRotation(90);
            
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
                switch (display.getRotation()) {
                    case Surface.ROTATION_0:
                        if (openedCameraId == CameraOptions.FRONT) {
                            videoPlayer.setOrientation(Orientation.ROTATE_90_CCW);
                        } else {
                        	videoPlayer.setOrientation(Orientation.ROTATE_90_CW);
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                            camera.setDisplayOrientation(90);
                        } else {
                            p.setRotation(90);
                        }
                        break;
                    case Surface.ROTATION_90:
                    	videoPlayer.setOrientation(Orientation.NONE);
                        break;
                    case Surface.ROTATION_180:
                        if (openedCameraId == CameraOptions.FRONT) {
                        	videoPlayer.setOrientation(Orientation.ROTATE_90_CW);
                        } else {
                        	videoPlayer.setOrientation(Orientation.ROTATE_90_CCW);
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                            camera.setDisplayOrientation(270);
                        } else {
                            p.setRotation(270);
                        }
                        break;
                    case Surface.ROTATION_270:
                        if (openedCameraId == CameraOptions.FRONT) {
                        	videoPlayer.setOrientation(Orientation.ROTATE_180);
                        } else {
                        	videoPlayer.setOrientation(Orientation.ROTATE_180);
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                            camera.setDisplayOrientation(180);
                        } else {
                            p.setRotation(180);
                        }
                        break;
                }
            } else {
                // getRotation not managed under Froyo
            	videoPlayer.setOrientation(Orientation.NONE);
            }*/
            
            // Use the existing size without resizing
            p.setPreviewSize(descriptor.getVideoWidth(), descriptor.getVideoHeight());

            // Set camera parameters
            camera.setParameters(p);
            try {
            	SurfaceHolder holder = new SurfaceHolder();
                camera.setPreviewDisplay(videoView.getHolder());
                camera.startPreview();
                cameraPreviewRunning = true;
                if (logger.isActivated()) {
                	logger.debug("Preview started");
                }
            } catch (Exception e) {
                if (logger.isActivated()) {
                	logger.error("Preview error", e);
                }
            }
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
            camera.setPreviewCallback(this);
            startCameraPreview();
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
