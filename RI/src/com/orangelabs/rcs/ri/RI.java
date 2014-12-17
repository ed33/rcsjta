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

package com.orangelabs.rcs.ri;

import android.app.ListActivity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.orangelabs.rcs.ri.capabilities.TestCapabilitiesApi;
import com.orangelabs.rcs.ri.contacts.TestContactsApi;
import com.orangelabs.rcs.ri.extension.TestMultimediaSessionApi;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionView;
import com.orangelabs.rcs.ri.extension.streaming.StreamingSessionView;
import com.orangelabs.rcs.ri.intents.TestIntentsApi;
import com.orangelabs.rcs.ri.ipcall.IPCallView;
import com.orangelabs.rcs.ri.ipcall.TestIPCallApi;
import com.orangelabs.rcs.ri.messaging.TestMessagingApi;
import com.orangelabs.rcs.ri.messaging.ft.InitiateFileTransfer;
import com.orangelabs.rcs.ri.messaging.ft.ReceiveFileTransfer;
import com.orangelabs.rcs.ri.service.TestServiceApi;
import com.orangelabs.rcs.ri.sharing.TestSharingApi;
import com.orangelabs.rcs.ri.sharing.geoloc.ReceiveGeolocSharing;
import com.orangelabs.rcs.ri.sharing.image.ReceiveImageSharing;
import com.orangelabs.rcs.ri.sharing.video.ReceiveVideoSharing;
import com.orangelabs.rcs.ri.upload.InitiateFileUpload;
import com.orangelabs.rcs.ri.utils.LogUtils;

/**
 * RI application
 * 
 * @author Jean-Marc AUFFRET
 */
public class RI extends ListActivity {
	
	private static final int VERSION_CODES_KITKAT = 19;
	
	 /**
   	 * The log tag for this class
   	 */
   	private static final String LOGTAG = LogUtils.getTag(RI.class.getSimpleName());
   	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        cancelAllPendingIntents();
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        
		// Set items
        String[] items = {
    		getString(R.string.menu_contacts),
    		getString(R.string.menu_capabilities),
    		getString(R.string.menu_messaging),
    		getString(R.string.menu_sharing),
    		getString(R.string.menu_mm_session),
    		getString(R.string.menu_ipcall),
    		getString(R.string.menu_intents),
    		getString(R.string.menu_service),
    		getString(R.string.menu_upload),
    		getString(R.string.menu_about)
        };
    	setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    	// Create the API connection manager
    	ApiConnectionManager.getInstance(this);
    }		

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	switch(position) {
        	case 0:
        		startActivity(new Intent(this, TestContactsApi.class));
        		break;
        		
        	case 1:
        		startActivity(new Intent(this, TestCapabilitiesApi.class));
        		break;
        		
        	case 2:
        		startActivity(new Intent(this, TestMessagingApi.class));
        		break;
        		
        	case 3:
        		startActivity(new Intent(this, TestSharingApi.class));
        		break;
        		
        	case 4:
        		startActivity(new Intent(this, TestMultimediaSessionApi.class));
        		break;

        	case 5:
        		startActivity(new Intent(this, TestIPCallApi.class));
        		break;

        	case 6:
        		startActivity(new Intent(this, TestIntentsApi.class));
        		break;

        	case 7:
        		startActivity(new Intent(this, TestServiceApi.class));
        		break;

        	case 8:
        		startActivity(new Intent(this, InitiateFileUpload.class));
        		break;

        	case 9:
        		startActivity(new Intent(this, AboutRI.class));
        		break;
    	}
    }
    
    /**
     * Cancel all pending intents to take into account Kitkat problem. See ON-30325 and
     * https://code.google.com/p/android/issues/detail?id=61850
     */
    private void cancelAllPendingIntents() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES_KITKAT) {
            if (LogUtils.isActive) {
				Log.d(LOGTAG, "cancelPendingIntents for Kitkat");
			}
            // Cancel pending intent for ReceiveVideoSharing
            cancelPendingIntentForClass(ReceiveVideoSharing.class);
            
            // Cancel pending intent for MessagingSessionView
            cancelPendingIntentForClass(MessagingSessionView.class);
            
            // Cancel pending intent for StreamingSessionView
            cancelPendingIntentForClass(StreamingSessionView.class);
            
            // Cancel pending intent for IPCallView
            cancelPendingIntentForClass(IPCallView.class);

            // Cancel pending intent for ReceiveFileTransfer
            cancelPendingIntentForClass(ReceiveFileTransfer.class);

            // Cancel pending intent for InitiateFileTransfer
            cancelPendingIntentForClass(InitiateFileTransfer.class);

            // Cancel pending intent for ReceiveImageSharing
            cancelPendingIntentForClass(ReceiveImageSharing.class);

            // Cancel pending intent for ReceiveGeolocSharing
            cancelPendingIntentForClass(ReceiveGeolocSharing.class);

            // Note that if we create other activities that are not exported and that we want to
            // open using a pendingIntent, we have to add them here also
        }
    }

    /**
     * Cancel a pending intent for a given class
     * 
     * @param className
     */
    private void cancelPendingIntentForClass(Class<?> className) {
        Intent notificationIntent = new Intent(this, className);
        // Creating and canceling an intent allows next calls to the same class to work properly
        PendingIntent intent = PendingIntent.getActivity(this, 1, notificationIntent, 0);
        intent.cancel();
    }

}
