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

package com.orangelabs.rcs.ri.sharing.video;

import java.lang.reflect.Method;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.gsma.services.rcs.vsh.VideoDescriptor;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharingListener;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.sharing.video.media.OriginatingVideoPlayer;
import com.orangelabs.rcs.ri.sharing.video.media.VideoPlayerListener;
import com.orangelabs.rcs.ri.sharing.video.media.VideoSurfaceView;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Initiate video sharing.
 *
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class InitiateVideoSharing extends Activity implements VideoPlayerListener {

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();
	
	/**
	 * Video sharing
	 */
	private VideoSharing mVideoSharing;
	
	/**
	 * Video sharing Id
	 */
	private String mSharingId;

    /**
     * Video player
     */
    private OriginatingVideoPlayer mVideoPlayer;

    /**
     * Camera of the device
     */
    private Camera mCamera;
    
    /**
     * Opened camera id
     */
    private CameraOptions mOpenedCameraId = CameraOptions.FRONT;

    /**
     * Camera preview started flag
     */
    private boolean mCameraPreviewRunning = false;

    /**
     * Video width
     */
    private int mVideoWidth = H264Config.QCIF_WIDTH;
    
    /**
     * Video height
     */
    private int mVideoHeight = H264Config.QCIF_HEIGHT;

    /**
     * Number of cameras
     */
    private int mNbfCameras = 1;    

    /**
     * Live video preview
     */
    private VideoSurfaceView mVideoView;
    
    /**
     * Video surface holder
     */
    private SurfaceHolder mSurface;
    
    /**
     * Progress dialog
     */
    private Dialog mProgressDialog;
    
	/**
	 * A locker to exit only once
	 */
	private LockAccess mExitOnce = new LockAccess();
    
   	/**
	 * API connection manager
	 */
	private ApiConnectionManager mCnxManager;
	
	/**
	 * Spinner for contact selection
	 */
	private Spinner mSpinner;
	
    /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(InitiateVideoSharing.class.getSimpleName());
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always on window
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Set layout
        setContentView(R.layout.video_sharing_initiate);

        // Set the contact selector
        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createRcsContactListAdapter(this));

        // Set button callback
        Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
        Button dialBtn = (Button)findViewById(R.id.dial_btn);
        dialBtn.setOnClickListener(btnDialListener);
        Button switchCamBtn = (Button)findViewById(R.id.switch_cam_btn);
        switchCamBtn.setEnabled(false);

        // Disable button if no contact available
        if (mSpinner.getAdapter().getCount() == 0) {
        	dialBtn.setEnabled(false);
        	inviteBtn.setEnabled(false);
        }

        // Get camera info
        mNbfCameras = getNumberOfCameras();
        
        if (mNbfCameras > 1) {
            boolean backAvailable = checkCameraSize(CameraOptions.BACK);
            boolean frontAvailable = checkCameraSize(CameraOptions.FRONT);
            if (frontAvailable && backAvailable) {
                switchCamBtn.setOnClickListener(btnSwitchCamListener);
            } else if (frontAvailable) {
                mOpenedCameraId = CameraOptions.FRONT;
                switchCamBtn.setVisibility(View.INVISIBLE);
            } else if (backAvailable) {
                mOpenedCameraId = CameraOptions.BACK;
                switchCamBtn.setVisibility(View.INVISIBLE);
            } else {
    			if (LogUtils.isActive) {
    				Log.d(LOGTAG, "No camera available for encoding");
    			}
            }
        } else {
            if (checkCameraSize(CameraOptions.FRONT)) {
                switchCamBtn.setVisibility(View.INVISIBLE);
            } else {
    			if (LogUtils.isActive) {
    				Log.d(LOGTAG, "No camera available for encoding");
    			}
            }
        }

        // Create the live video view
        mVideoView = (VideoSurfaceView)findViewById(R.id.video_preview);
        mVideoView.setAspectRatio(mVideoWidth, mVideoHeight);
        mVideoView.setVisibility(View.GONE);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
        	mVideoView.setAspectRatio(mVideoWidth, mVideoHeight);
        } else {
        	mVideoView.setAspectRatio(mVideoHeight, mVideoWidth);
        }
        mSurface = mVideoView.getHolder();
        mSurface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurface.setKeepScreenOn(true);
        
		// Register to API connection manager
		mCnxManager = ApiConnectionManager.getInstance(this);
		if (mCnxManager == null || !mCnxManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), mExitOnce);
			return;
			
		}
        mCnxManager.startMonitorServices(this, mExitOnce, RcsServiceName.VIDEO_SHARING);

		// Add service listener
		try {
			mCnxManager.getVideoSharingApi().addEventListener(vshListener);
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onCreate initiate video sharing");
			}
		} catch (RcsServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to add listener", e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), mExitOnce);
		}
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mCnxManager == null) {
			return;
			
		}
		mCnxManager.stopMonitorServices(this);
		if (!mCnxManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
			return;
			
		}
		// Remove video sharing listener
		try {
			mCnxManager.getVideoSharingApi().removeEventListener(vshListener);
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to remove listener", e);
			}
		}
	}

    /**
     * Dial button listener
     */
    private OnClickListener btnDialListener = new OnClickListener() {
        public void onClick(View v) {
        	// get selected phone number
    		ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
    		String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());

            // Initiate a GSM call before to be able to share content
            Intent intent = new Intent(Intent.ACTION_CALL);
        	intent.setData(Uri.parse("tel:".concat(phoneNumber)));
            startActivity(intent);
        }
    };

    /**
     * Invite button listener
     */
    private OnClickListener btnInviteListener = new OnClickListener() {
        public void onClick(View v) {
            // Check if the service is available
        	boolean registered = false;
        	try {
        		registered = mCnxManager.getVideoSharingApi().isServiceRegistered();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
            if (!registered) {
    	    	Utils.showMessage(InitiateVideoSharing.this, getString(R.string.label_service_not_available));
    	    	return;
    	    	
            } 
            
            // Get the remote contact
            ContactListAdapter adapter = (ContactListAdapter) mSpinner.getAdapter();
            String phoneNumber = adapter.getSelectedNumber(mSpinner.getSelectedView());

            ContactUtils contactUtils = ContactUtils.getInstance(InitiateVideoSharing.this);
            final ContactId remote;
    		try {
    			remote = contactUtils.formatContact(phoneNumber);
    		} catch (RcsContactFormatException e1) {
    			Utils.showMessage(InitiateVideoSharing.this, getString(R.string.label_invalid_contact,phoneNumber));
    	    	return;
    	    	
    		}
    		
            new Thread() {
            	public void run() {
		        	try {
		                // Create the video player
		        		mVideoPlayer = new OriginatingVideoPlayer(InitiateVideoSharing.this);
		        		
		                // Start the camera
		        		openCamera();
		
		        		// Initiate sharing
		        		mVideoSharing = mCnxManager.getVideoSharingApi().shareVideo(remote, mVideoPlayer);
		        		mSharingId = mVideoSharing.getSharingId();
		        	} catch(Exception e) {
		        		e.printStackTrace();
		        		
		        		// Free the camera
		            	closeCamera();
		        		
	            		handler.post(new Runnable() { 
	    					public void run() {
								hideProgressDialog();
								Utils.showMessageAndExit(InitiateVideoSharing.this, getString(R.string.label_invitation_failed), mExitOnce);
	    		        	}
		    			});
		        	}
		    	}
		    }.start();

            // Display a progress dialog
            mProgressDialog = Utils.showProgressDialog(InitiateVideoSharing.this, getString(R.string.label_command_in_progress));            
            mProgressDialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Toast.makeText(InitiateVideoSharing.this, getString(R.string.label_sharing_cancelled), Toast.LENGTH_SHORT).show();
					quitSession();
				}
			});
            
            // Disable UI
            mSpinner.setEnabled(false);

            // Display video view
            mVideoView.setVisibility(View.VISIBLE);
            
            // Hide buttons
            Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        	inviteBtn.setVisibility(View.GONE);
            Button dialBtn = (Button)findViewById(R.id.dial_btn);
            dialBtn.setVisibility(View.GONE);
        }
    };

    /**
     * Switch camera button listener
     */
    private View.OnClickListener btnSwitchCamListener = new View.OnClickListener() {
        public void onClick(View v) {
		    // Release camera
		    closeCamera();

		    // Switch camera
            switchCamera();
        }
    };    
    
	/**
	 * Hide progress dialog
	 */
    public void hideProgressDialog() {
    	if (mProgressDialog == null) {
    		return;
    		
    	}
		if (mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		mProgressDialog = null;
    }     
    
    /**
     * Quit the session
     */
    private void quitSession() {
		// Release the camera
    	closeCamera();

    	// Stop the sharing
    	try {
            if (mVideoSharing != null) {
            	mVideoSharing.abortSharing();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	mVideoSharing = null;
		
	    // Exit activity
		finish();
    }    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            	// Quit the session
            	quitSession();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_video_sharing, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_close_session:
				// Quit the session
				quitSession();
				break;
		}
		return true;
	}
    
    /*-------------------------- Camera methods ------------------*/
    
    /**
     * Open the camera
     */
    private synchronized void openCamera() {
        if (mCamera != null) {
        	return;
        	
        }
		// Open camera
		openCamera(mOpenedCameraId);
		mVideoView.setAspectRatio(mVideoWidth, mVideoHeight);

		// Start camera
		mCamera.setPreviewCallback(mVideoPlayer);
		startCameraPreview();
    }    
    
    /**
     * Close the camera
     */
    private synchronized void closeCamera() {
	    if (mCamera == null) {
	    	return;
	    	
	    }
		mCamera.setPreviewCallback(null);
		if (mCameraPreviewRunning) {
			mCameraPreviewRunning = false;
			mCamera.stopPreview();
		}
		mCamera.release();
		mCamera = null;
    }
    
    /**
     * Switch the camera
     */
    private synchronized void switchCamera() {
        if (mCamera != null) {
        	return;
        	
        }
		// Open the other camera
		if (mOpenedCameraId.getValue() == CameraOptions.BACK.getValue()) {
			openCamera(CameraOptions.FRONT);
		} else {
			openCamera(CameraOptions.BACK);
		}

		// Restart the preview
		mCamera.setPreviewCallback(mVideoPlayer);
		startCameraPreview();  
    }
    
    /**
     * Check if good camera sizes are available for encoder.
     * Must be used only before open camera.
     * 
     * @param cameraId
     * @return false if the camera don't have the good preview size for the encoder
     */
    private boolean checkCameraSize(CameraOptions cameraId) {
        boolean sizeAvailable = false;

        // Open the camera
        openCamera(cameraId);

        // Check common sizes
        Parameters param = mCamera.getParameters();
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        for (Camera.Size size:sizes) {
            if (    (size.width == H264Config.QVGA_WIDTH && size.height == H264Config.QVGA_HEIGHT) ||
                    (size.width == H264Config.CIF_WIDTH && size.height == H264Config.CIF_HEIGHT) ||
                    (size.width == H264Config.VGA_WIDTH && size.height == H264Config.VGA_HEIGHT)) {
                sizeAvailable = true;
                break;
            }
        }

        // Release camera
        closeCamera();

        return sizeAvailable;
    }

    /**
     * Start the camera preview
     */
    private void startCameraPreview() {
        if (mCamera != null) {
            // Camera settings
            Camera.Parameters p = mCamera.getParameters();
            p.setPreviewFormat(PixelFormat.YCbCr_420_SP);

            // Orientation
			Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			switch (display.getRotation()) {
			case Surface.ROTATION_0:
				if (mOpenedCameraId == CameraOptions.FRONT) {
					mVideoPlayer.setOrientation(Orientation.ROTATE_90_CCW);
				} else {
					mVideoPlayer.setOrientation(Orientation.ROTATE_90_CW);
				}
				mCamera.setDisplayOrientation(90);
				break;
				
			case Surface.ROTATION_90:
				mVideoPlayer.setOrientation(Orientation.NONE);
				break;
				
			case Surface.ROTATION_180:
				if (mOpenedCameraId == CameraOptions.FRONT) {
					mVideoPlayer.setOrientation(Orientation.ROTATE_90_CW);
				} else {
					mVideoPlayer.setOrientation(Orientation.ROTATE_90_CCW);
				}
				mCamera.setDisplayOrientation(270);
				break;
				
			case Surface.ROTATION_270:
				if (mOpenedCameraId == CameraOptions.FRONT) {
					mVideoPlayer.setOrientation(Orientation.ROTATE_180);
				} else {
					mVideoPlayer.setOrientation(Orientation.ROTATE_180);
				}
				mCamera.setDisplayOrientation(180);
				break;
			}
            
			// Check if preview size is supported
			if (isPreviewSizeSupported(p, mVideoWidth, mVideoHeight)) {
				// Use the existing size without resizing
				p.setPreviewSize(mVideoWidth, mVideoHeight);
				// TODO videoPlayer.activateResizing(videoWidth, videoHeight); // same size = no
				// resizing
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "Camera preview initialized with size " + mVideoWidth + "x" + mVideoHeight);
				}
			} else {
				// Check if can use a other known size (QVGA, CIF or VGA)
				int w = 0;
				int h = 0;
				for (Camera.Size size : p.getSupportedPreviewSizes()) {
					w = size.width;
					h = size.height;
					if ((w == H264Config.QVGA_WIDTH && h == H264Config.QVGA_HEIGHT) || (w == H264Config.CIF_WIDTH && h == H264Config.CIF_HEIGHT)
							|| (w == H264Config.VGA_WIDTH && h == H264Config.VGA_HEIGHT)) {
						break;
					}
				}

				if (w != 0) {
					p.setPreviewSize(w, h);
					// TODO does not work if default sizes are not supported like for Samsung S5 mini
					// mVideoPlayer.activateResizing(w, h);
					if (LogUtils.isActive) {
						Log.d(LOGTAG, "Camera preview initialized with size " + w + "x" + h + " with a resizing to " + mVideoWidth + "x" + mVideoHeight);
					}
				} else {
					// The camera don't have known size, we can't use it
					if (LogUtils.isActive) {
						Log.d(LOGTAG, "Camera preview can't be initialized with size " + mVideoWidth + "x" + mVideoHeight);
					}
					Toast.makeText(this, getString(R.string.label_session_failed,"Camera is not compatible"), Toast.LENGTH_SHORT).show();
					quitSession();
					return;
					
				}
			}

            // Set camera parameters
            mCamera.setParameters(p);
            try {
                mCamera.setPreviewDisplay(mVideoView.getHolder());
                mCamera.startPreview();
                mCameraPreviewRunning = true;
            } catch (Exception e) {
                mCamera = null;
            }
        }
    }

    /**
     * Get Camera "open" Method
     *
     * @return Method
     */
    private Method getCameraOpenMethod() {
        ClassLoader classLoader = InitiateVideoSharing.class.getClassLoader();
        try {
        	Class<?> cameraClass = classLoader.loadClass("android.hardware.Camera");
            try {
                return cameraClass.getMethod("open", new Class[] {
                    int.class
                });
            } catch (NoSuchMethodException e) {
            }
        } catch (ClassNotFoundException e) {
        }
        return null;
    }
    
    /**
     * Open the camera
     *
     * @param cameraId Camera ID
     */
    private void openCamera(CameraOptions cameraId) {
        Method method = getCameraOpenMethod();
        if (mNbfCameras > 1 && method != null) {
            try {
                mCamera = (Camera)method.invoke(mCamera, new Object[] {
                    cameraId.getValue()
                });
                mOpenedCameraId = cameraId;
            } catch (Exception e) {
                mCamera = Camera.open();
                mOpenedCameraId = CameraOptions.BACK;
            }
        } else {
            mCamera = Camera.open();
        }
        if (mVideoPlayer != null) {
        	mVideoPlayer.setCameraId(mOpenedCameraId.getValue());
        }
    }
    
    /**
     * Get Camera "numberOfCameras" Method
     *
     * @return Method
     */
    private Method getCameraNumberOfCamerasMethod() {
        ClassLoader classLoader = InitiateVideoSharing.class.getClassLoader();
        try {
        	Class<?> cameraClass = classLoader.loadClass("android.hardware.Camera");
            try {
                return cameraClass.getMethod("getNumberOfCameras", (Class[])null);
            } catch (NoSuchMethodException e) {
            }
        } catch (ClassNotFoundException e) {
        }
        return null;
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

    /*-------------------------- Session callbacks ------------------*/
    
   	/**
     * Video sharing listener
     */
	private VideoSharingListener vshListener = new VideoSharingListener() {
		@Override
		public void onStateChanged(ContactId contact, String sharingId, final int state, final int reasonCode) {
			// Discard event if not for current sharingId
			if (mSharingId == null || !mSharingId.equals(sharingId)) {
				return;
				
			}
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onVideoSharingStateChanged contact=" + contact + " sharingId=" + sharingId + " state=" + state
						+ " reason=" + reasonCode);
			}
			if (state > RiApplication.VSH_STATES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onVideoSharingStateChanged unhandled state=" + state);
				}
				return;
				
			}
			if (reasonCode > RiApplication.VSH_REASON_CODES.length) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "onVideoSharingStateChanged unhandled reason=" + reasonCode);
				}
				return;
				
			}
			final String _reasonCode = RiApplication.VSH_REASON_CODES[reasonCode];
			handler.post(new Runnable() {
				public void run() {
					switch (state) {
					case VideoSharing.State.STARTED:
						// Display video format
						try {
							VideoDescriptor videoDescriptor = mVideoSharing.getVideoDescriptor();
							String format = mVideoSharing.getVideoEncoding() + " " +
									videoDescriptor.getVideoWidth() + "x" + videoDescriptor.getVideoHeight();
							TextView fmtView = (TextView) findViewById(R.id.video_format);
							fmtView.setVisibility(View.VISIBLE);
							fmtView.setText(getString(R.string.label_video_format, format));
						} catch(Exception e) {
							e.printStackTrace();
						}

						// Start the player
						mVideoPlayer.open();
						mVideoPlayer.start();

						// Update camera button
				        Button switchCamBtn = (Button)findViewById(R.id.switch_cam_btn);
				        switchCamBtn.setEnabled(true);
						
						// Session is established : hide progress dialog
						hideProgressDialog();
						break;

					case VideoSharing.State.ABORTED:
						// Stop the player
						mVideoPlayer.stop();
						mVideoPlayer.close();
						
						// Release the camera
						closeCamera();
						
						// Hide progress dialog
						hideProgressDialog();
						
						// Display message info and exit
						Utils.showMessageAndExit(InitiateVideoSharing.this, getString(R.string.label_sharing_aborted, _reasonCode), mExitOnce);
						break;

					case VideoSharing.State.REJECTED:
						// Release the camera
						closeCamera();
						
						// Hide progress dialog
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateVideoSharing.this,
								getString(R.string.label_sharing_rejected, _reasonCode), mExitOnce);
						break;

					case VideoSharing.State.FAILED:
						// Stop the player
						mVideoPlayer.stop();
						mVideoPlayer.close();
						
						// Release the camera
						closeCamera();

						// Hide progress dialog
						hideProgressDialog();
						
						// Display error info and exit
						Utils.showMessageAndExit(InitiateVideoSharing.this, getString(R.string.label_sharing_failed, _reasonCode), mExitOnce);
						break;

					default:
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onVideoSharingStateChanged " + getString(R.string.label_vsh_state_changed, RiApplication.VSH_STATES[state], reasonCode));
						}
					}
				}
			});
		}
	};
	
    /*-------------------------- Video player callbacks ------------------*/
    
	/**
	 * Callback called when the player is opened
	 */
	public void onPlayerOpened() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onPlayerOpened");
		}		
	}

	/**
	 * Callback called when the player is started
	 */
	public void onPlayerStarted() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onPlayerStarted");
		}		
	}

	/**
	 * Callback called when the player is stopped
	 */
	public void onPlayerStopped() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onPlayerStopped");
		}
	}

	/**
	 * Callback called when the player is closed
	 */
	public void onPlayerClosed() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onPlayerClosed");
		}
	}

	/**
	 * Callback called when the player has failed
	 */
	public void onPlayerError() {
		// TODO
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onPlayerError");
		}
	}
	
	/**
	 * Callback called when the player has been resized
	 */
	public void onPlayerResized(int width, int height) {
		// TODO
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onPlayerResized");
		}
	}
	
	/**
	 * Check if preview size is supported
	 * 
	 * @param parameters
	 *            Camera parameters
	 * @param width
	 * @param height
	 * @return True if supported
	 */
    private boolean isPreviewSizeSupported(Parameters parameters, int width, int height) {
    	List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
    	for (Size size : sizes) {
    		 if (size.width == width && size.height == height) {
                 return true;
                 
             }
		}
        return false;
    }
    
}


