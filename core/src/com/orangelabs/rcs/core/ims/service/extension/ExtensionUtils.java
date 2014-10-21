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
package com.orangelabs.rcs.core.ims.service.extension;

import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;

/**
 * Extensions utils
 *  
 * @author jexa7410
 */
public class ExtensionUtils {
	/**
	 * Is a valid extension
	 * 
	 * @return Boolean
	 */
	public static boolean isValidExt(String ext) {
		return isMnoExt(ext) || isThirdPartyExt(ext);
	}
	
	/**
	 * Is a MNO extension
	 * 
	 * @return Boolean
	 */
	public static boolean isMnoExt(String ext) {
		if (ext.startsWith(FeatureTags.FEATURE_RCSE_EXTENSION + ".mnc")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Is a third party extension
	 * 
	 * @return Boolean
	 */
	public static boolean isThirdPartyExt(String ext) {
		if (ext.startsWith(FeatureTags.FEATURE_RCSE_EXTENSION + ".ext")) {
			return true;
		} else {
			return false;
		}
	}
}
