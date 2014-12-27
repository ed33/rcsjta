/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.core.ims.service.extension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.core.ims.service.capability.ExternalCapabilityMonitoring;
import com.orangelabs.rcs.provider.security.AuthorizationData;
import com.orangelabs.rcs.provider.security.SecurityLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * A class to update the supported extensions in background.
 * 
 * @author LEMORDANT Philippe
 *
 */
public class SupportedExtensionUpdater implements Runnable {

	final private RcsSettings mRcsSettings;

	final private SecurityLog mSecurityLog;
	
	final private Context mContext;
	
	final private ExtensionManager mExtensionManager;

	private ExternalCapabilityMonitoring mCapabilityMonitoring;
	
	private final static Logger logger = Logger.getLogger(SupportedExtensionUpdater.class.getSimpleName());
	
	/**
	 * @param rcsSettings
	 * @param securityLog
	 * @param context
	 * @param extensionManager
	 * @param capabilityMonitoring
	 */
	public SupportedExtensionUpdater(RcsSettings rcsSettings, SecurityLog securityLog, Context context,
			ExtensionManager extensionManager, ExternalCapabilityMonitoring capabilityMonitoring) {
		mRcsSettings = rcsSettings;
		mSecurityLog = securityLog;
		mContext = context;
		mExtensionManager = extensionManager;
		mCapabilityMonitoring = capabilityMonitoring;
	}

	@Override
	public void run() {
		boolean isLogActivated = logger.isActivated();
		if (isLogActivated) {
			logger.debug("Update supported extensions addExtensions");
		}
		if (!mRcsSettings.isExtensionsAllowed()) {
			if (isLogActivated) {
				logger.debug("Extensions are NOT allowed");
			}
			return;
			// ---
		}
		try {
			// Save authorizations before update
			Map<AuthorizationData, Integer> authorizationsBeforeUpdate = mSecurityLog.getAllAuthorizations();

			Set<AuthorizationData> authorizationAfterUpdate = new HashSet<AuthorizationData>();

			PackageManager packageManager = mContext.getPackageManager();
			Map<String, Set<String>> packageNames = getPackagesManagingExtensions(packageManager);
			for (String packageName : packageNames.keySet()) {
				if (!mRcsSettings.isExtensionsControlled()) {
					if (isLogActivated) {
						logger.debug("No control on extensions");
					}
					for (String extension : packageNames.get(packageName)) {
						AuthorizationData authData = new AuthorizationData(packageName, extension);
						authorizationAfterUpdate.add(authData);
						continue;

					}
				}
				// Check if extensions are supported
				Set<AuthorizationData> supportedExts = mExtensionManager.checkExtensions(packageManager, packageName, packageNames.get(packageName));
				// Save IARI Authorization document in cache to avoid having to re-process the signature each time the
				// application is loaded
				authorizationAfterUpdate.addAll(supportedExts);
			}
			// Save new authorizations
			for (AuthorizationData authorizationData : authorizationAfterUpdate) {
				if (!authorizationsBeforeUpdate.containsKey(authorizationData)) {
					mSecurityLog.addAuthorization(authorizationData);
				}
			}
			// Remove invalid authorizations
			authorizationsBeforeUpdate.keySet().removeAll(authorizationAfterUpdate);
			for (AuthorizationData authorizationData : authorizationsBeforeUpdate.keySet()) {
				if (isLogActivated) {
					logger.debug("Remove authorization for package '" + authorizationData.getPackageName() + "' extension:"
							+ authorizationData.getExtension());
				}
				mSecurityLog.removeAuthorization(authorizationsBeforeUpdate.get(authorizationData));
			}
			if (isLogActivated) {
				logger.debug("Register for package installation/removal");
			}
			IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
			filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
			filter.addDataScheme("package");
			mContext.registerReceiver(mCapabilityMonitoring, filter);
		} catch (Exception e) {
			if (isLogActivated) {
				logger.error("Unexpected error", e);
			}
		}
	}

	/**
	 * Get packages names and associated extensions.
	 * 
	 * @param pkgManager
	 * @return a map with package names (key) and associated extensions (value)
	 */
	private Map<String, Set<String>> getPackagesManagingExtensions(PackageManager pkgManager) {
		Map<String, Set<String>> packagesWithMetaData = new HashMap<String, Set<String>>();
		// Get all applications having CapabilityService.INTENT_EXTENSIONS for meta data
		List<ApplicationInfo> apps = pkgManager.getInstalledApplications(PackageManager.GET_META_DATA);
		for (ApplicationInfo appInfo : apps) {
			Bundle appMeta = appInfo.metaData;
			if (appMeta != null) {
				String extensions = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
				Set<String> extensionSet = ExtensionManager.getExtensions(extensions);
				if (!extensionSet.isEmpty()) {
					// Save package name
					packagesWithMetaData.put(appInfo.packageName, extensionSet);
				}
			}
		}

		// Retrieve all activities that can be performed for the CapabilityService.INTENT_EXTENSIONS intent.
		Intent intent = new Intent(CapabilityService.INTENT_EXTENSIONS);
		intent.setType(ExtensionManager.ALL_EXTENSIONS_MIME_TYPE);
		List<ResolveInfo> resolveInfos = pkgManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
		Set<String> packagesWithActivitiesProcessingIntentExtension = new HashSet<String>();
		for (ResolveInfo resolveInfo : resolveInfos) {
			packagesWithActivitiesProcessingIntentExtension.add(resolveInfo.activityInfo.packageName);
		}

		// Only keep packages belonging to both sets
		packagesWithMetaData.keySet().retainAll(packagesWithActivitiesProcessingIntentExtension);
		return packagesWithMetaData;
	}
	
}
