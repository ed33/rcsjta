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
package com.orangelabs.rcs.ri.extension.messaging;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * Messaging service utils
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessagingSessionUtils {
	/**
	 * Service ID constant
	 */
	//public final static String SERVICE_ID = "ext.messaging";
	
	private final static String META_DATA_EXTENSION = "com.gsma.services.rcs.capability.EXTENSION";
	private final static String SEP = ";";
		
	private static String[] serviceIds = null;
	
	/**
	 * Get security extensions from the android manifest file. 
	 * @param context
	 * @return String[]
	 */
	public static String[] getServicesIds(Context context){
	
		if(serviceIds == null){
			try {
				PackageInfo info;
				info = context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_META_DATA);
				serviceIds = info.applicationInfo.metaData.getString(META_DATA_EXTENSION).split(SEP);
			}
			catch (NameNotFoundException e) {
				e.printStackTrace();
			} 							
		}
		return serviceIds;
	}
	
}
