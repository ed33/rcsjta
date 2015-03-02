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

package com.orangelabs.rcs.service.api;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Binder;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.service.extension.ExtensionManager;
import com.orangelabs.rcs.platform.AndroidFactory;

/**
 * Server API utils
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServerApiUtils {
	/**
	 * Two ways for checking an extension :
	 * 1. : Check with the binded process and the extension.
	 * 2. : Check with the IARI
	 */
	public static enum ExtensionCheckType {WITH_PROCESS_BINDING, WITHOUT_PROCESS_BINDING};
	
	/**
	 * Test core
	 * 
	 * @throws ServerApiException
	 */
	public static void testCore() throws ServerApiException {
		if (Core.getInstance() == null) {
			throw new ServerApiException("Core is not instanciated");
		}
	}
	
	/**
	 * Test IMS connection
	 * 
	 * @throws ServerApiException
	 */
	public static void testIms() throws ServerApiException {
		if (!isImsConnected()) { 
			throw new ServerApiException("Core is not connected to IMS"); 
		}
	}
	
	/**
	 * Is connected to IMS
	 * 
	 * @return Boolean
	 */
	public static boolean isImsConnected(){
		return ((Core.getInstance() != null) &&
				(Core.getInstance().getImsModule().getCurrentNetworkInterface() != null) &&
				(Core.getInstance().getImsModule().getCurrentNetworkInterface().isRegistered()));
	}
	
	/**
	 * Get running application process information
	 * 
	 * @return RunningAppProcessInfo or null
	 */
	private static RunningAppProcessInfo getRunningAppProcessInfo() {
		// Check extension authorization
		int pid = Binder.getCallingPid();
		ActivityManager manager = (ActivityManager) AndroidFactory.getApplicationContext().getSystemService(
				Context.ACTIVITY_SERVICE);
		for (RunningAppProcessInfo info : manager.getRunningAppProcesses()) {
			if (info.pid == pid) {
				return info;
			}
		}
		return null;
	}
	
	/**
	 * Checks if extension is authorized
	 * @param serviceExtensionManager 
	 * @param serviceId 
	 * @param extensionCheckType 
	 * @throws ServerPermissionDeniedException 
	 */
	public static void assertExtensionIsAuthorized(ExtensionManager serviceExtensionManager, String serviceId, ExtensionCheckType extensionCheckType) throws ServerPermissionDeniedException {
		RunningAppProcessInfo appInfo = null;
		if(extensionCheckType == ExtensionCheckType.WITH_PROCESS_BINDING){
			appInfo = ServerApiUtils.getRunningAppProcessInfo();
			if (appInfo == null) {
				throw new ServerPermissionDeniedException("Cannot get RunningAppProcessInfo"); 
			}			
		}
		serviceExtensionManager.testApiExtensionPermission(serviceId, appInfo);									
	}
}
