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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Service extension manager which adds supported extension after having verified some authorization rules.
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServiceExtensionManager {

	/**
	 * Singleton of ServiceExtensionManager
	 */
	private static volatile ServiceExtensionManager sInstance;

	private final static String LEADING_ZERO = "0";

	private final static String EXTENSION_SEPARATOR = ";";

	private final static String IARI_SEPARATOR = ";";

	private final static String IARI_DOC_NAME_TYPE = ".xml";

	private final static Logger logger = Logger.getLogger(ServiceExtensionManager.class.getSimpleName());

	/**
	 * the trust store
	 */
	private BKSTrustStore ks;

	/**
	 * Empty constructor : prevent caller from creating multiple instances
	 */
	private ServiceExtensionManager() {
		String ksPath = "/sdcard/range-root-truststore.bks"; // TODO: get from provisioning
		
		try {
			ks = new BKSTrustStore(new FileInputStream(ksPath));
		} catch (FileNotFoundException e) {
			if (logger.isActivated()) {
				logger.warn(new StringBuilder("File not found: ").append(ksPath).toString());
			}
			e.printStackTrace();
		}
	}

	/**
	 * Get an instance of ServiceExtensionManager.
	 *
	 * @return the singleton instance.
	 */
	public static ServiceExtensionManager getInstance() {
		if (sInstance != null) {
			return sInstance;
			// ---
		}
		synchronized (ServiceExtensionManager.class) {
			if (sInstance == null) {
				sInstance = new ServiceExtensionManager();
			}
		}
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
		RcsSettings.getInstance().setSupportedRcsExtensions(supportedExts);
	}

	/**
	 * Check if the extensions are valid. Each valid extension is saved in the cache.
	 *
	 * @param context
	 *            Context
	 * @param supportedExts
	 *            Set of supported extensions
	 * @param newExts
	 *            Set of new extensions to be checked
	 */
	private void checkExtensions(Context context, PackageManager pm, List<ResolveInfo> resolveInfos, Set<String> supportedExts,
			Set<String> newExts) {
		
		boolean isLogActivated = logger.isActivated();
		
		// Check each new extension
		for (String extension : newExts) {
			
			if (isExtensionAuthorized(context, extension)) {
				
				if (supportedExts.contains(extension)) {
					if (isLogActivated) {
						logger.debug("Extension " + extension + " is already in the list");
					}
				} else {
					
					for (ResolveInfo resolveInfo : resolveInfos) {
						
						// get all Authorization paths for the app (possible to get many for one App)
						String[] authDocumentResourceNames = getAuthDocumentResourceNames(pm,
								resolveInfo.activityInfo.packageName);
						
						if (authDocumentResourceNames.length > 0) {
							
							if (isExtensionAuthorizedBySecurity(pm, resolveInfo, extension, authDocumentResourceNames)) {
								// Add the extension in the supported list if authorized and not yet in the list
								supportedExts.add(extension);
								if (isLogActivated) {
									logger.debug("Extension " + extension + " is added to the list");
								}
							} else {
								if (isLogActivated) {
									logger.debug("Extension " + extension + " CANNOT be added to the list");
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Update supported extensions at boot
	 *
	 * @param context
	 *            Context
	 */
	public void updateSupportedExtensions(Context context, boolean addExtensions) {
		
		boolean isLogActivated = logger.isActivated();
		
		// TODO managed addExtensions
		if (context == null) {
			if (isLogActivated) {
				logger.warn("Cannot update supported extension: context is null");
			}
			return;
			// ---
		}
		
		try {
			if (isLogActivated) {
				logger.debug("Update supported extensions addExtensions=" + addExtensions);
			}
			
			// Intent query on current installed activities
			PackageManager packageManager = context.getPackageManager();

			Intent intent = new Intent(CapabilityService.INTENT_EXTENSIONS);
			String mime = CapabilityService.EXTENSION_MIME_TYPE + "/*";
			intent.setType(mime);

			List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);

			Set<String> supportedExts = new HashSet<String>();

			// Intent query on current installed activities
			List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
			for (ApplicationInfo appInfo : apps) {
				Bundle appMeta = appInfo.metaData;
				if (appMeta != null) {
					String exts = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
					if (!TextUtils.isEmpty(exts)) {
						if (isLogActivated) {
							logger.debug("Update supported extensions " + exts);
						}
						// Check extensions
						checkExtensions(context, packageManager, resolveInfos, supportedExts, getExtensions(exts));
					}
				}
			}

			// Update supported extensions in database
			saveSupportedExtensions(supportedExts);
		} catch (Exception e) {
			if (isLogActivated) {
				logger.error("Unexpected error", e);
			}
		}
	}

	/**
	 * Is extension authorized (i.e. allowed by configuration)
	 *
	 * @param context
	 *            Context
	 * @param ext
	 *            Extension ID
	 * @return Boolean
	 */
	public boolean isExtensionAuthorized(Context context, String ext) {
		
		boolean res = RcsSettings.getInstance().isExtensionsAllowed();
		if (logger.isActivated()) {
			logger.debug("isExtensionAuthorized> " + res);
		}
		
		return res;
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
		updateSupportedExtensions(context, false);
	}

	/**
	 * Extract set of extensions from String
	 *
	 * @param extensions
	 *            String where extensions are concatenated with a ";" separator
	 * @return the set of extensions
	 */
	public Set<String> getExtensions(String extensions) {
		
		Set<String> result = new HashSet<String>();
		
		if (!TextUtils.isEmpty(extensions)) {
			
			String[] extensionList = extensions.split(ServiceExtensionManager.EXTENSION_SEPARATOR);
			
			for (String extension : extensionList) {
				if (!TextUtils.isEmpty(extension) && extension.trim().length() > 0) {
					result.add(extension);
				}
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
	
	public String getExtensions(Set<String> extensions) {
		
		if (extensions != null && !extensions.isEmpty()) {
			
			StringBuilder result = new StringBuilder();
			for (String extension : extensions) {
				if (extension.trim().length() > 0) {
					result.append(EXTENSION_SEPARATOR);
				}
				result.append(extension);
			}
			return result.toString();
			// ---
		}
		return "";
	}

	/**
	 * Is extension authorized. 
	 * NB: there can be at most one IARI for a given extension by app
	 * 
	 * @param pm
	 *            the app's package manager
	 * @param resolveInfo
	 *            Application info
	 * @param ext
	 *            Extension ID
	 * @param authDocumentResourceNames
	 *            the array of IARI document resource names
	 * @return Boolean
	 */
	public boolean isExtensionAuthorizedBySecurity(PackageManager pm, ResolveInfo resolveInfo, String ext,
			String[] authDocumentResourceNames) {

		if ((resolveInfo == null) || (resolveInfo.activityInfo == null)) {
			return false;
			// ---
		}

		boolean islogActivated = logger.isActivated();

		try {
			if (!RcsSettings.getInstance().isExtensionsAllowed()) {
				if (islogActivated) {
					logger.debug("Extensions are NOT allowed");
				}
				return false;
				// ---
			}

			if (!RcsSettings.getInstance().isExtensionsControlled()) {
				if (islogActivated) {
					logger.debug("No control on extensions");
				}
				return true;
				// ---
			}

			String pkgName = resolveInfo.activityInfo.packageName;
			if (islogActivated) {
				logger.debug("Check extension " + ext + " for package " + pkgName);
			}

			if (!ExtensionUtils.isValidExt(ext)) {
				if (islogActivated) {
					logger.debug(ext + " is NOT a valid extension (not a 2nd party nor 3dr party extension )");
				}
				// TODO return false;
				// ---
			}

			if (ExtensionUtils.isThirdPartyExt(ext) && (RcsSettings.getInstance().getExtensionspolicy() == 1)) {
				if (islogActivated) {
					logger.debug("Third party extensions are not allowed");
				}
				return false;
				// ---
			}

			PackageInfo pkg = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
			Signature[] signs = pkg.signatures;

			if (signs.length > 0) {
				String sha1Sign = getFingerprint(signs[0].toByteArray());
				if (islogActivated) {
					logger.debug("Check application fingerprint: " + sha1Sign);
				}

				PackageProcessor processor = new PackageProcessor(ks, pkgName, sha1Sign);

				ProcessingResult result = null;
				// search all IARI authorizations for configuration
				for (String iariResourceName : authDocumentResourceNames) {
					// com.iari-authorization string from app pkgName

					InputStream iariDocument = getIariDocumentFromAssets(pm, pkgName, iariResourceName);

					// Is resource found ?
					if (iariDocument != null) {
						if (islogActivated) {
							logger.info("IARI document " + iariResourceName + " is found");
						}
						
						try {
							 result = processor.processIARIauthorization(iariDocument);
							//saveIARIdoc(iariDocument);
						} catch (Exception e) {
							if (islogActivated) {
								logger.error("Exception raised when processing IARI doc=" + iariResourceName, e);
							}
							return false;
							// ---
						} finally {
							iariDocument.close();
						}
						if (ProcessingResult.STATUS_OK == result.getStatus()) {
							if (islogActivated) {
								logger.debug("Extension is authorized");
							}
							return true;
							// ---
						}
					} else {
						if (islogActivated) {
							logger.warn("Failed to find IARI document " + iariResourceName);
						}
					}
				}

				if (islogActivated) {
					logger.debug("Extension " + ext + " is not authorized: " + result.getStatus() + " "
							+ result.getError().toString());
				}

			} else {
				if (islogActivated) {
					logger.debug("Extension is not authorized: no signature found");
				}
			}
		} catch (Exception e) {
			if (islogActivated) {
				logger.error("Internal exception", e);
			}
		}
		return false;

	}

	/**
	 * get the array of IARI authorization document resource names
	 * 
	 * @param pm
	 *            the package manager
	 * @param pkgName
	 *            the app name
	 * @return
	 */
	private String[] getAuthDocumentResourceNames(PackageManager pm, String pkgName) {
		String iariList = "";
		
		try {
			ApplicationInfo ai = pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA);
			
			if (ai.metaData != null) {
				
				iariList = ai.metaData.getString(CapabilityService.INTENT_EXTENSIONS);
				if (logger.isActivated()) {
					logger.debug("List of IARIs: " + iariList);
				}
				if (iariList == null) {
					iariList = "";
				}
			}
		} catch (NameNotFoundException e) {
			if (logger.isActivated()) {
				logger.error("Cannot get IARI list", e);
			}
		}
		return iariList.split(IARI_SEPARATOR);
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
			return am.open(new StringBuilder(iariResourceName).append(IARI_DOC_NAME_TYPE).toString());
			// ---
			
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
}
