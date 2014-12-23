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

package com.orangelabs.rcs.core.ims.service.capability;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.core.ims.service.extension.ExtensionManager;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.security.SecurityInfos;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * External capability monitoring
 * 
 * @author jexa7410
 */
public class ExternalCapabilityMonitoring extends BroadcastReceiver {
	/**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(ExternalCapabilityMonitoring.class.getSimpleName());
	
    @Override
	public void onReceive(Context context, Intent intent) {
    	boolean isLoggerActive = logger.isActivated();
    	Context appContext = AndroidFactory.getApplicationContext();
    	try {
    		if (appContext == null) {
    			// TODO check
    			if (isLoggerActive) {
    				logger.warn("Discard event context is null");
    			}
    			return;
    		}
	    	// Instantiate the settings manager
	    	RcsSettings.createInstance(context);
	    	RcsSettings rcsSettings = RcsSettings.getInstance();
	    	
	    	// Instantiate the security info manager
			SecurityInfos.createInstance(context.getContentResolver());
			SecurityInfos securityInfos = SecurityInfos.getInstance();
			
			// Instantiate the service extension manager
			ExtensionManager.createInstance(rcsSettings, securityInfos);
			ExtensionManager extensionManager = ExtensionManager.getInstance();
			
	    	// Get Intent parameters
	        String action = intent.getAction();
	    	Integer uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
	    	if (uid == -1) {
	    		return;
	    	}
	    	
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            	// Get extensions associated to the new application
    	        PackageManager pm = context.getPackageManager();
    	        String packageName = intent.getData().getSchemeSpecificPart();
    	        ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
    	        if (appInfo == null) {
    	        	// No app info
    	        	return;
    	        }
    	        Bundle appMeta = appInfo.metaData;
    	        if (appMeta == null) {
    	        	// No app meta
    	        	return;
    	        }
    	        
    	        String exts = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
    	        if (exts == null) {
    	        	// No RCS extension
    	        	return;
    	        }
    	        
            	if (isLoggerActive) {
            		logger.debug("Add extensions " + exts + " for application " + uid+ " context="+appContext);
            	}
            	
				// Add the new extension in the supported RCS extensions
				try {
					extensionManager.addNewSupportedExtensions(appContext);
				} catch (Exception e) {
					if (isLoggerActive) {
						logger.error("Failed to add nex supported extensions", e);
					}
				}
			} else {
//				if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
//					if (logger.isActivated()) {
//						logger.debug("Remove extensions for application " + uid);
//					}
//
//					// Remove the extensions in the supported RCS extensions
//					ServiceExtensionManager.getInstance().removeSupportedExtensions(appContext);
//				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
    }
}