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

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;

import com.gsma.iariauth.validator.PackageProcessor;
import com.gsma.iariauth.validator.ProcessingResult;
import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.security.SecurityInfos;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.ServerApiException;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Service extension manager which adds supported extension after having verified some authorization rules.
 * 
 * @author Jean-Marc AUFFRET
 * @author Ph. LEMORDANT
 * @author F. ABOT
 */
public class ExtensionManager {

	private final static String LEADING_ZERO = "0";

	private final static String EXTENSION_SEPARATOR = ";";

	private final static String IARI_DOC_NAME_TYPE = ".xml";
	
	private final static String ALL_EXTENSIONS_MIME_TYPE = CapabilityService.EXTENSION_MIME_TYPE.concat("/*");

	/**
	 * Singleton of ServiceExtensionManager
	 */
	private static volatile ExtensionManager sInstance;

	private final static Logger logger = Logger.getLogger(ExtensionManager.class.getSimpleName());

	private BKSTrustStore mTrustStore;
	
	private RcsSettings mRcsSettings;
	
	/**
	 * Empty constructor to prevent default instantiation
	 */
	private ExtensionManager() {
	}

	/**
	 * Constructor
	 * 
	 * @param rcsSettings
	 * @param securityInfos
	 * @throws CertificateException 
	 * @throws NoSuchProviderException
	 */
	private ExtensionManager(RcsSettings rcsSettings, SecurityInfos securityInfos) throws NoSuchProviderException,
			CertificateException {
		try {
			mTrustStore = new BKSTrustStore(securityInfos);
			mRcsSettings = rcsSettings;
		} catch (NoSuchProviderException e1) {
			if (logger.isActivated()) {
				logger.error("Failed to instantiate ServiceExtensionManager", e1);
			}
			throw e1;
		} catch (CertificateException e2) {
			if (logger.isActivated()) {
				logger.error("Failed to instantiate ServiceExtensionManager", e2);
			}
			throw e2;
		}
	}

	/**
	 * Create an instance of ServiceExtensionManager.
	 *
	 * @param rcsSettings
	 * @param securityInfos
	 * @return the singleton instance.
	 * @throws CertificateException 
	 * @throws NoSuchProviderException
	 */
	public static ExtensionManager createInstance(RcsSettings rcsSettings, SecurityInfos securityInfos) throws NoSuchProviderException, CertificateException {
		if (sInstance != null) {
			return sInstance;
			// ---
		}
		synchronized (ExtensionManager.class) {
			if (sInstance == null) {
				sInstance = new ExtensionManager(rcsSettings, securityInfos);
			}
		}
		return sInstance;
	}
	
	/**
	 * Get the instance of ServiceExtensionManager.
	 *
	 * @return the singleton instance.
	 */
	public static ExtensionManager getInstance() {
		return sInstance;
	}
	
	/**
	 * Save supported extensions in database
	 *
	 * @param supportedExts
	 *            List of supported extensions
	 */
	private void saveSupportedExtensions(Set<String> supportedExts) {
		// Update supported extensions in database
		mRcsSettings.setSupportedRcsExtensions(supportedExts);
	}

	/**
	 * Check if the extensions are valid. Each valid extension is saved in the cache.
	 *
	 * @param packageManager
	 * @param resolvedInfos
	 * @return Set of new extensions
	 */
	private Set<String> checkExtensions(PackageManager packageManager, List<ResolveInfo> resolveInfos, Set<String> extensions) {
		Set<String> result = new HashSet<String>();
		boolean isLogActivated = logger.isActivated();
		// Check each new extension
		for (String extension : extensions) {
			for (ResolveInfo resolveInfo : resolveInfos) {
				String packageName = resolveInfo.activityInfo.packageName;
				if (isExtensionAuthorizedBySecurity(packageManager, packageName, extension)) {
					// Add the extension in the supported list if authorized and not yet in the list
					result.add(extension);
					if (isLogActivated) {
						logger.debug("Extension '" + extension + "' is added to the list");
					}
				} else {
					if (isLogActivated) {
						logger.debug("Extension '" + extension + "' CANNOT be added to the list");
					}
				}
			}
		}
		return result;
	}

	/**
	 * Update supported extensions at boot
	 *
	 * @param context
	 *            Context
	 * @param addExtensions
	 */
	public void updateSupportedExtensions(Context context, boolean addExtensions) {
		boolean isLogActivated = logger.isActivated();
		try {
			if (isLogActivated) {
				logger.debug("Update supported extensions addExtensions=" + addExtensions);
			}
			// Intent query on current installed activities
			PackageManager packageManager = context.getPackageManager();
			Intent intent = new Intent(CapabilityService.INTENT_EXTENSIONS);
			intent.setType(ALL_EXTENSIONS_MIME_TYPE);

			List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);

			// Intent query on current installed activities
			List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
			for (ApplicationInfo appInfo : apps) {
				Bundle appMeta = appInfo.metaData;
				if (appMeta != null) {
					String extensions = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
					if (!TextUtils.isEmpty(extensions)) {
						if (isLogActivated) {
							logger.debug("Update supported extensions ".concat(extensions));
						}
						// Check extensions
						Set<String> supportedExts = checkExtensions(packageManager, resolveInfos, getExtensions(extensions));
						// Update supported extensions in database
						saveSupportedExtensions(supportedExts);
					}
				}
			}
		} catch (Exception e) {
			if (isLogActivated) {
				logger.error("Unexpected error", e);
			}
		}
	}

	/**
	 * Remove supported extensions
	 *
	 * @param context
	 *            Context
	 */
	public void removeSupportedExtensions(Context context) {
		updateSupportedExtensions(context, true);
	}

	/**
	 * Add supported extensions
	 *
	 * @param context
	 *            Context
	 */
	public void addNewSupportedExtensions(Context context) {
		updateSupportedExtensions(context, true);
	}

	/**
	 * Extract set of extensions from String
	 *
	 * @param extensions
	 *            String where extensions are concatenated with a ";" separator
	 * @return the set of extensions
	 */
	public static Set<String> getExtensions(String extensions) {
		Set<String> result = new HashSet<String>();
		if (TextUtils.isEmpty(extensions)) {
			return result;
			
		}
		String[] extensionList = extensions.split(ExtensionManager.EXTENSION_SEPARATOR);
		for (String extension : extensionList) {
			if (!TextUtils.isEmpty(extension) && extension.trim().length() > 0) {
				result.add(extension);
			}
		}
		return result;
	}

	/**
	 * Concatenate set of extensions into a string
	 *
	 * @param extensions
	 *            set of extensions
	 * @return String where extensions are concatenated with a ";" separator
	 */
	public static String getExtensions(Set<String> extensions) {
		if (extensions == null || extensions.isEmpty()) {
			return "";
			
		}
		StringBuilder result = new StringBuilder();
		int size = extensions.size();
		for (String extension : extensions) {
			if (extension.trim().length() == 0) {
				--size;
				continue;
				
			}
			result.append(extension);
			if (--size != 0) {
				// Not last item : add separator
				result.append(EXTENSION_SEPARATOR);
			}
		}
		return result.toString();
	}

	/**
	 * Is extension authorized. 
	 * NB: there can be at most one IARI for a given extension by app
	 * 
	 * @param pm
	 *            the app's package manager
	 * @param resolveInfo
	 *            Application info
	 * @param extension
	 *            Extension ID
	 * @return Boolean
	 */
	public boolean isExtensionAuthorizedBySecurity(PackageManager pm, String packageName, String extension) {
		boolean islogActivated = logger.isActivated();
		try {
			if (!mRcsSettings.isExtensionsAllowed()) {
				if (islogActivated) {
					logger.debug("Extensions are NOT allowed");
				}
				return false;
				// ---
			}
			if (!mRcsSettings.isExtensionsControlled()) {
				if (islogActivated) {
					logger.debug("No control on extensions");
				}
				return true;
				// ---
			}
			if (islogActivated) {
				logger.debug("Check extension " + extension + " for package " + packageName);
			}

			if (!ExtensionUtils.isValidExt(extension)) {
				if (islogActivated) {
					logger.debug(extension.concat(" is NOT a valid extension (not a 2nd party nor 3dr party extension )"));
				}
				// TODO return false;
				// ---
			}
			if (ExtensionUtils.isThirdPartyExt(extension) && (mRcsSettings.getExtensionspolicy() == 1)) {
				if (islogActivated) {
					logger.debug("Third party extensions are not allowed");
				}
				return false;
				// ---
			}

			PackageInfo pkg = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
			Signature[] signs = pkg.signatures;

			if (signs.length == 0) {
				if (islogActivated) {
					logger.debug("Extension is not authorized: no signature found");
				}
				return false;
				
			}
			String sha1Sign = getFingerprint(signs[0].toByteArray());
			if (islogActivated) {
				logger.debug("Check application fingerprint: " .concat(sha1Sign));
			}

			PackageProcessor processor = new PackageProcessor(mTrustStore, packageName, sha1Sign);

			// search all IARI authorizations for configuration
			// com.iari-authorization string from app pkgName

			InputStream iariDocument = getIariDocumentFromAssets(pm, packageName, extension);
			// Is IARI document resource found ?
			if (iariDocument == null) {
				if (islogActivated) {
					logger.warn("Failed to find IARI document for ".concat(extension));
				}
				return false;

			}
			if (islogActivated) {
				logger.info("IARI document found for ".concat(extension));
			}

			try {
				ProcessingResult result = processor.processIARIauthorization(iariDocument);
				if (ProcessingResult.STATUS_OK == result.getStatus()) {
					if (islogActivated) {
						logger.debug("Extension is authorized: ".concat(extension));
					}
					return true;
					// ---
				}
				if (islogActivated) {
					logger.debug("Extension " + extension + " is not authorized: " + result.getStatus() + " "
							+ result.getError().toString());
				}
			} catch (Exception e) {
				if (islogActivated) {
					logger.error("Exception raised when processing IARI doc=".concat(extension), e);
				}
				// ---
			} finally {
				iariDocument.close();
			}
		} catch (Exception e) {
			if (islogActivated) {
				logger.error("Internal exception", e);
			}
		}
		return false;
	}

	/**
	 * @param pm
	 * @param pkgName
	 * @param iariResourceName
	 * @return
	 */
	private InputStream getIariDocumentFromAssets(PackageManager pm, String pkgName, String iariResourceName) {
		try {
			Resources res = pm.getResourcesForApplication(pkgName);
			AssetManager am = res.getAssets();
			return am.open(iariResourceName.concat(IARI_DOC_NAME_TYPE));
			
		} catch (IOException e) {
			if (logger.isActivated()) {
				logger.error("Cannot get IARI document from assets", e);
			}
		} catch (NameNotFoundException e) {
			if (logger.isActivated()) {
				logger.error("Cannot get IARI document from assets", e);
			}
		}
		return null;
	}

	/**
	 * Returns the fingerprint of a certificate
	 * 
	 * @param cert
	 *            Certificate
	 * @param algorithm
	 *            hash algorithm to be used
	 * @return String as xx:yy:zz
	 */
	public String getFingerprint(byte[] cert) throws Exception {
		
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(cert);
		byte[] digest = md.digest();

		String toRet = "";
		for (int i = 0; i < digest.length; i++) {
			if (i != 0)
				toRet += ":";
			int b = digest[i] & 0xff;
			String hex = Integer.toHexString(b);
			if (hex.length() == 1)
				toRet += LEADING_ZERO;
			toRet += hex;
		}
		return toRet.toUpperCase();
	}
	
	/**
	 * Test API extension permission
	 * 
	 * @param extension Extension ID
	 * @param processInfo
	 * @throws ServerApiException
	 */
	public void testApiExtensionPermission(String extension, RunningAppProcessInfo processInfo) throws ServerApiException {
		PackageManager pm = AndroidFactory.getApplicationContext().getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
			if (processInfo.processName.equals(info.activityInfo.packageName)) {
				if (isExtensionAuthorizedBySecurity(pm, info.activityInfo.packageName, extension)) {
					return;
					
				}
			}
		}

		throw new ServerApiException("Extension " + extension + " is not authorized");
	}
}
