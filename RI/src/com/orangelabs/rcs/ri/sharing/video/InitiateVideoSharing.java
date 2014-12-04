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
import android.database.MatrixCursor;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.os.Build;
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
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharingListener;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.sharing.video.media.OriginatingVideoPlayer;
import com.orangelabs.rcs.ri.sharing.video.media.VideoPlayerListener;
import com.orangelabs.rcs.ri.sharing.video.media.VideoSurfaceView;
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
	private VideoSharing videoSharing;
	
	/**
	 * Video sharing Id
	 */
	private String sharingId;

    /**
     * Video player
     */
    private OriginatingVideoPlayer videoPlayer;

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
     * Video width
     */
    private int videoWidth = H264Config.QCIF_WIDTH;
    
    /**
     * Video height
     */
    private int videoHeight = H264Config.QCIF_HEIGHT;

    /**
     * Number of cameras
     */
    private int numberOfCameras = 1;    

    /**
     * Live video preview
     */
    private VideoSurfaceView videoView;
    
    /**
     * Video surface holder
     */
    private SurfaceHolder surface;
    
    /**
     * Progress dialog
     */
    private Dialog progressDialog;
    
	/**
	 * A locker to exit only once
	 */
	private LockAccess exitOnce = new LockAccess();
    
   	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;
	
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

        // Set title
        setTitle(R.string.menu_initiate_video_sharing);

        // Set the contact selector
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        spinner.setAdapter(Utils.createRcsContactListAdapter(this));

        // Set button callback
        Button inviteBtn = (Button)findViewById(R.id.invite_btn);
        inviteBtn.setOnClickListener(btnInviteListener);
        Button dialBtn = (Button)findViewById(R.id.dial_btn);
        dialBtn.setOnClickListener(btnDialListener);
        Button switchCamBtn = (Button)findViewById(R.id.switch_cam_btn);
        switchCamBtn.setEnabled(false);

        // Disable button if no contact available
        if (spinner.getAdapter().getCount() == 0) {
        	dialBtn.setEnabled(false);
        	inviteBtn.setEnabled(false);
        }

        // Get camera info
        numberOfCameras = getNumberOfCameras();
        
        if (numberOfCameras > 1) {
            boolean backAvailable = checkCameraSize(CameraOptions.BACK);
            boolean frontAvailable = checkCameraSize(CameraOptions.FRONT);
            if (frontAvailable && backAvailable) {
                switchCamBtn.setOnClickListener(btnSwitchCamListener);
            } else if (frontAvailable) {
                openedCameraId = CameraOptions.FRONT;
                switchCamBtn.setVisibility(View.INVISIBLE);
            } else if (backAvailable) {
                openedCameraId = CameraOptions.BACK;
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
        videoView = (VideoSurfaceView)findViewById(R.id.video_preview);
        videoView.setAspectRatio(videoWidth, videoHeight);
        videoView.setVisibility(View.GONE);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
        	videoView.setAspectRatio(videoWidth, videoHeight);
        } else {
        	videoView.setAspectRatio(videoHeight, videoWidth);
        }
        surface = videoView.getHolder();
        surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.setKeepScreenOn(true);
        
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
        connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.VIDEO_SHARING);

		// Add service listener
		try {
			connectionManager.getVideoSharingApi().addEventListener(vshListener);
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onCreate initiate video sharing");
			}
		} catch (RcsServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Failed to add listener", e);
			}
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
		
		if (connectionManager == null) {
			return;
		}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
			// Remove video sharing listener
			try {
				connectionManager.getVideoSharingApi().removeEventListener(vshListener);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Failed to remove listener", e);
				}
			}
		}
	}

    /**
     * Dial button listener
     */
    private OnClickListener btnDialListener = new OnClickListener() {
        public void onClick(View v) {
        	// Get the remote contact
            Spinner spinner = (Spinner)findViewById(R.id.contact);
            MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();
            String remote = cursor.getString(1);

            // Initiate a GSM call before to be able to share content
            Intent intent = new Intent(Intent.ACTION_CALL);
        	intent.setData(Uri.parse("tel:"+remote));
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
        		registered = connectionManager.getVideoSharingApi().isServiceRegistered();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
            if (!registered) {
    	    	Utils.showMessage(InitiateVideoSharing.this, getString(R.string.label_service_not_available));
    	    	return;
            } 
            
            // Get the remote contact
            Spinner spinner = (Spinner)findViewById(R.id.contact);
            MatrixCursor cursor = (MatrixCursor)spinner.getSelectedItem();

            ContactUtils contactUtils = ContactUtils.getInstance(InitiateVideoSharing.this);
            final ContactId remote;
    		try {
    			remote = contactUtils.formatContactId(cursor.getString(1));
    		} catch (RcsContactFormatException e1) {
    			Utils.showMessage(InitiateVideoSharing.this, getString(R.string.label_invalid_contact,cursor.getString(1)));
    	    	return;
    		}
    		
            new Thread() {
            	public void run() {
		        	try {
		                // Create the video player
		        		videoPlayer = new OriginatingVideoPlayer(InitiateVideoSharing.this);
		        		
		                // Start the camera
		        		openCamera();
		
		        		// Initiate sharing
		        		videoSharing = connectionManager.getVideoSharingApi().shareVideo(remote, videoPlayer);
		        		sharingId = videoSharing.getSharingId();
		        	} catch(Exception e) {
		        		e.printStackTrace();
		        		
		        		// Free the camera
		            	closeCamera();
		        		
	            		handler.post(new Runnable() { 
	    					public void run() {
								hideProgressDialog();
								Utils.showMessageAndExit(InitiateVideoSharing.this, getString(R.string.label_invitation_failed), exitOnce);
	    		        	}
		    			});
		        	}
		    	}
		    }.start();

            // Display a progress dialog
            progressDialog = Utils.showProgressDialog(InitiateVideoSharing.this, getString(R.string.label_command_in_progress));            
            progressDialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Toast.makeText(InitiateVideoSharing.this, getString(R.string.label_sharing_cancelled), Toast.LENGTH_SHORT).show();
					quitSession();
				}
			});
            
            // Disable UI
            spinner.setEnabled(false);

            // Display video view
            videoView.setVisibility(View.VISIBLE);
            
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
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
			progressDialog = null;
		}
    }     
    
    /**
     * Quit the session
     */
    private void quitSession() {
		// Release the camera
    	closeCamera();

    	// Stop the sharing
    	try {
            if (videoSharing != null) {
            	videoSharing.abortSharing();
            }
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	videoSharing = null;
		
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
        if (camera == null) {
            // Open camera
            openCamera(openedCameraId);
            videoView.setAspectRatio(videoWidth, videoHeight);

            // Start camera
            camera.setPreviewCallback(videoPlayer);
            startCameraPreview();
        }
    }    
    
    /**
     * Close the camera
     */
    private synchronized void closeCamera() {
	    if (camera != null) {
	        camera.setPreviewCallback(null);
	        if (cameraPreviewRunning) {
	            cameraPreviewRunning = false;
	            camera.stopPreview();
	        }
	        camera.release();
	        camera = null;
	    }
    }
    
    /**
     * Switch the camera
     */
    private synchronized void switchCamera() {
        if (camera == null) {		
		    // Open the other camera
		    if (openedCameraId.getValue() == CameraOptions.BACK.getValue()) {
		    	openCamera(CameraOptions.FRONT);
		    } else {
		    	openCamera(CameraOptions.BACK);
		    }
		
		    // Restart the preview
		    camera.setPreviewCallback(videoPlayer);
		    startCameraPreview();    
        }
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
        Parameters param = camera.getParameters();
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
        if (camera != null) {
            // Camera settings
            Camera.Parameters p = camera.getParameters();
            p.setPreviewFormat(PixelFormat.YCbCr_420_SP);

            // Orientation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
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
            }
            
            // Use the existing size without resizing
            p.setPreviewSize(videoWidth, videoHeight);

            // Set camera parameters
            camera.setParameters(p);
            try {
                camera.setPreviewDisplay(videoView.getHolder());
                camera.startPreview();
                cameraPreviewRunning = true;
            } catch (Exception e) {
                camera = null;
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
        Class<?> cameraClass = null;
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
        if (videoPlayer != null) {
        	videoPlayer.setCameraId(openedCameraId.getValue());
        }
    }
    
    /**
     * Get Camera "numberOfCameras" Method
     *
     * @return Method
     */
    private Method getCameraNumberOfCamerasMethod() {
        ClassLoader classLoader = InitiateVideoSharing.class.getClassLoader();
        Class<?> cameraClass = null;
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

    /*-------------------------- Session callbacks ------------------*/
    
   	/**
     * Video sharing listener
     */
	private VideoSharingListener vshListener = new VideoSharingListener() {
		@Override
		public void onVideoSharingStateChanged(ContactId contact, String sharingId, final int state, final int reasonCode) {
			// Discard event if not for current sharingId
			if (InitiateVideoSharing.this.sharingId == null || !InitiateVideoSharing.this.sharingId.equals(sharingId)) {
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
							String format = videoSharing.getVideoEncoding() + " " +
									videoSharing.getVideoDescriptor().getVideoWidth() + "x" + videoSharing.getVideoDescriptor().getVideoHeight();
							TextView fmtView = (TextView)findViewById(R.id.video_format);
							fmtView.setVisibility(View.VISIBLE);
							fmtView.setText(getString(R.string.label_video_format, format));
						} catch(Exception e) {}

						// Start the player
						videoPlayer.open();
						videoPlayer.start();

						// Update camera button
				        Button switchCamBtn = (Button)findViewById(R.id.switch_cam_btn);
				        switchCamBtn.setEnabled(true);
						
						// Session is established : hide progress dialog
						hideProgressDialog();
						break;

					case VideoSharing.State.ABORTED:
						// Stop the player
						videoPlayer.stop();
						videoPlayer.close();
						
						// Release the camera
						closeCamera();
						
						// Hide progress dialog
						hideProgressDialog();
						
						// Display message info and exit
						Utils.showMessageAndExit(InitiateVideoSharing.this, getString(R.string.label_sharing_aborted, _reasonCode), exitOnce);
						break;

					case VideoSharing.State.REJECTED:
						// Release the camera
						closeCamera();
						
						// Hide progress dialog
						hideProgressDialog();
						Utils.showMessageAndExit(InitiateVideoSharing.this,
								getString(R.string.label_sharing_rejected, _reasonCode), exitOnce);
						break;

					case VideoSharing.State.FAILED:
						// Stop the player
						videoPlayer.stop();
						videoPlayer.close();
						
						// Release the camera
						closeCamera();

						// Hide progress dialog
						hideProgressDialog();
						
						// Display error info and exit
						Utils.showMessageAndExit(InitiateVideoSharing.this, getString(R.string.label_sharing_failed, _reasonCode), exitOnce);
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
}


