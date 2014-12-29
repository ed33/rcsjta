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
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.text.TextUtils;

import com.gsma.iariauth.validator.IARIAuthDocument;
import com.gsma.iariauth.validator.PackageProcessor;
import com.gsma.iariauth.validator.ProcessingResult;
import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.core.ims.service.capability.ExternalCapabilityMonitoring;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.security.AuthorizationData;
import com.orangelabs.rcs.provider.security.SecurityLog;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.ServerApiException;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Service extension manager which adds supported extension after having verified some authorization rules.
 * 
 * @author Jean-Marc AUFFRET
 * @author P.LEMORDANT
 * @author F.ABOT
 */
public class ExtensionManager {

	private final static String LEADING_ZERO = "0";

	private final static String EXTENSION_SEPARATOR = ";";

	private final static String IARI_DOC_NAME_TYPE = ".xml";

	public final static String ALL_EXTENSIONS_MIME_TYPE = CapabilityService.EXTENSION_MIME_TYPE.concat("/*");

	/**
	 * Singleton of ServiceExtensionManager
	 */
	private static volatile ExtensionManager sInstance;

	private final static Logger logger = Logger.getLogger(ExtensionManager.class.getSimpleName());

	final private BKSTrustStore mTrustStore;

	final private RcsSettings mRcsSettings;

	final private SecurityLog mSecurityLog;

	final private Context mContext;

	private ExternalCapabilityMonitoring mCapabilityMonitoring;

	private final static Executor sUpdateSupportedExtensionProcessor = Executors.newSingleThreadExecutor();

	/**
	 * Empty constructor to prevent default instantiation
	 */
	private ExtensionManager() {
		mTrustStore = null;
		mRcsSettings = null;
		mSecurityLog = null;
		mContext = null;
	}

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param rcsSettings
	 * @param securityLog
	 * @throws CertificateException
	 * @throws NoSuchProviderException
	 */
	private ExtensionManager(Context context, RcsSettings rcsSettings, SecurityLog securityLog) throws NoSuchProviderException,
			CertificateException {
		try {
			mTrustStore = new BKSTrustStore(securityLog);
			mRcsSettings = rcsSettings;
			mSecurityLog = securityLog;
			mContext = context;
			mCapabilityMonitoring = new ExternalCapabilityMonitoring(mContext, mRcsSettings, this);
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
	 * @param context
	 * @param rcsSettings
	 * @param securityLog
	 * @return the singleton instance.
	 * @throws CertificateException
	 * @throws NoSuchProviderException
	 */
	public static ExtensionManager createInstance(Context context, RcsSettings rcsSettings, SecurityLog securityLog)
			throws NoSuchProviderException, CertificateException {
		if (sInstance != null) {
			return sInstance;
			// ---
		}
		synchronized (ExtensionManager.class) {
			if (sInstance == null) {
				sInstance = new ExtensionManager(context, rcsSettings, securityLog);
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
	 * Check if the extensions are valid.
	 *
	 * @param pkgManager
	 * @param pkgName
	 * @param extensions
	 *            set of extensions to validate
	 * @return Set of authorization data
	 */
	public Set<AuthorizationData> checkExtensions(PackageManager pkgManager, String pkgName, Set<String> extensions) {
		Set<AuthorizationData> result = new HashSet<AuthorizationData>();
		boolean isLogActivated = logger.isActivated();
		// Check each new extension
		for (String extension : extensions) {
			IARIAuthDocument authDocument = getExtensionAuthorizedBySecurity(pkgManager, pkgName, extension);
			if (authDocument == null) {
				if (isLogActivated) {
					logger.debug("Extension '" + extension + "' CANNOT be added to the list");
				}
				continue;

			}
			// Add the extension in the supported list if authorized and not yet in the list
			AuthorizationData authData = new AuthorizationData(authDocument.packageName, extension, authDocument.iari,
					authDocument.authType, authDocument.range, authDocument.packageSigner);
			result.add(authData);
			if (isLogActivated) {
				logger.debug("Extension '" + extension + "' is added to the list");
			}
		}
		return result;
	}

	/**
	 * Stop monitoring for package installations and removals.
	 */
	public void stop() {
		if (mCapabilityMonitoring != null) {
			mContext.unregisterReceiver(mCapabilityMonitoring);
			mCapabilityMonitoring = null;
		}
	}

	/**
	 * Save authorizations in authorization table for caching
	 * 
	 * @param authorizationDatas
	 *            collection of authorizations
	 */
	private void saveAuthorizations(Collection<AuthorizationData> authorizationDatas) {
		for (AuthorizationData authData : authorizationDatas) {
			mSecurityLog.addAuthorization(authData);
		}
	}

	/**
	 * Save authorizations in authorization table for caching.<br>
	 * This method is used when authorization data are not controlled.
	 * 
	 * @param pkgName
	 * @param extensions
	 *            set of extensions
	 */
	private void saveAuthorizations(String pkgName, Set<String> extensions) {
		for (String extension : extensions) {
			// Save supported extension in database
			AuthorizationData authData = new AuthorizationData(pkgName, extension);
			mSecurityLog.addAuthorization(authData);
		}
	}

	/**
	 * Remove supported extensions for package
	 *
	 * @param pkgName
	 */
	public void removeExtensionsForPackage(String pkgName) {
		Set<Integer> rowIDds = mSecurityLog.getAuthorizationIDsForPackageName(pkgName);
		if (rowIDds.isEmpty()) {
			return;
		}
		if (logger.isActivated()) {
			logger.info("Remove authorizations for package ".concat(pkgName));
		}
		for (Integer rowId : rowIDds) {
			mSecurityLog.removeAuthorization(rowId);
		}
	}

	/**
	 * Add extensions if supported
	 * 
	 * @param pkgManager
	 * @param pkgName
	 * @param extensions
	 *            set of extensions
	 */
	public void addSupportedExtensions(PackageManager pkgManager, String pkgName, Set<String> extensions) {
		if (!mRcsSettings.isExtensionsControlled()) {
			if (logger.isActivated()) {
				logger.debug("No control on extensions");
			}
			saveAuthorizations(pkgName, extensions);
			return;
			// ---
		}
		// Check if extensions are supported
		Set<AuthorizationData> supportedExts = checkExtensions(pkgManager, pkgName, extensions);
		// Save IARI Authorization document in cache to avoid having to re-process the signature each time the
		// application is loaded
		saveAuthorizations(supportedExts);
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
	 * Get authorized extensions.<br>
	 * NB: there can be at most one IARI for a given extension by app
	 * 
	 * @param pkgManager
	 *            the app's package manager
	 * @param pkgName Package name
	 * @param extension
	 *            Extension ID
	 * @return IARIAuthDocument or null if not authorized
	 */
	private IARIAuthDocument getExtensionAuthorizedBySecurity(PackageManager pkgManager, String pkgName, String extension) {
		boolean isLogActivated = logger.isActivated();
		try {
			if (isLogActivated) {
				logger.debug("Check extension " + extension + " for package " + pkgName);
			}

			if (!ExtensionUtils.isValidExt(extension)) {
				if (isLogActivated) {
					logger.debug(extension.concat(" is NOT a valid extension (not a 2nd party nor 3dr party extension )"));
				}
				// TODO return false;
				// ---
			}
			// TODO following test is wrong
			if (ExtensionUtils.isThirdPartyExt(extension) && (mRcsSettings.getExtensionspolicy() == 1)) {
				if (isLogActivated) {
					logger.debug("Third party extensions are not allowed");
				}
				return null;
				// ---
			}

			PackageInfo pkg = pkgManager.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
			Signature[] signs = pkg.signatures;

			if (signs.length == 0) {
				if (isLogActivated) {
					logger.debug("Extension is not authorized: no signature found");
				}
				return null;

			}
			if (isLogActivated) {
				logger.debug("Signature: length "+signs[0].toByteArray().length);
			}
			String sha1Sign = getFingerprint(signs[0].toByteArray());
			if (isLogActivated) {
				logger.debug("Check application fingerprint: ".concat(sha1Sign));
			}

			PackageProcessor processor = new PackageProcessor(mTrustStore, pkgName, sha1Sign);

			// search all IARI authorizations for configuration
			// com.iari-authorization string from app pkgName

			InputStream iariDocument = getIariDocumentFromAssets(pkgManager, pkgName, extension);
			// Is IARI document resource found ?
			if (iariDocument == null) {
				if (isLogActivated) {
					logger.warn("Failed to find IARI document for ".concat(extension));
				}
				return null;

			}
			if (isLogActivated) {
				logger.debug("IARI document found for ".concat(extension));
			}

			try {
				ProcessingResult result = processor.processIARIauthorization(iariDocument);
				if (ProcessingResult.STATUS_OK == result.getStatus()) {
					if (isLogActivated) {
						logger.debug("Extension is authorized: ".concat(extension));
					}
					return result.getAuthDocument();
					// ---
				}
				if (isLogActivated) {
					logger.debug("Extension " + extension + " is not authorized: " + result.getStatus() + " "
							+ result.getError().toString());
				}
			} catch (Exception e) {
				if (isLogActivated) {
					logger.error("Exception raised when processing IARI doc=".concat(extension), e);
				}
				// ---
			} finally {
				iariDocument.close();
			}
		} catch (Exception e) {
			if (isLogActivated) {
				logger.error("Internal exception", e);
			}
		}
		return null;
	}

	/**
	 * Get IARI authorization document from assets
	 * @param pkgManager
	 * @param pkgName
	 * @param iariResourceName
	 * @return InputStream or null if not found
	 */
	private InputStream getIariDocumentFromAssets(PackageManager pkgManager, String pkgName, String iariResourceName) {
		try {
			Resources res = pkgManager.getResourcesForApplication(pkgName);
			AssetManager am = res.getAssets();
			return am.open(iariResourceName.concat(IARI_DOC_NAME_TYPE));

		} catch (IOException e) {
			if (logger.isActivated()) {
				logger.error("Cannot get IARI document from assets", e);
			}
		} catch (NameNotFoundException e) {
			if (logger.isActivated()) {
				logger.error("IARI authorization doc no found", e);
			}
		}
		return null;
	}

	/**
	 * Returns the fingerprint of a certificate
	 * 
	 * @param cert
	 *            Certificate
	 * @return String as xx:yy:zz
	 * @throws NoSuchAlgorithmException
	 */
	private String getFingerprint(byte[] cert) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(cert);
		byte[] digest = md.digest();

		String toRet = "";
		for (int i = 0; i < digest.length; i++) {
			if (i != 0)
				toRet = toRet.concat(":");
			int b = digest[i] & 0xff;
			String hex = Integer.toHexString(b);
			if (hex.length() == 1)
				toRet = toRet.concat(LEADING_ZERO);
			toRet = toRet.concat(hex);
		}
		return toRet.toUpperCase();
	}
	
	/**
	 * Test API extension permission
	 * 
	 * @param extension
	 *            Extension ID
	 * @param processInfo
	 * @throws ServerApiException
	 */
	public void testApiExtensionPermission(String extension, RunningAppProcessInfo processInfo) throws ServerApiException {
		if (!mRcsSettings.isExtensionsControlled()) {
			if (logger.isActivated()) {
				logger.debug("No control on extensions");
			}
			return;

		}
		PackageManager pm = AndroidFactory.getApplicationContext().getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		for (ResolveInfo info : pm.queryIntentActivities(intent, 0)) {
			if (processInfo.processName.equals(info.activityInfo.packageName)) {
				if (getExtensionAuthorizedBySecurity(pm, info.activityInfo.packageName, extension) != null) {
					return;

				}
			}
		}

		throw new ServerApiException("Extension " + extension + " is not authorized");
	}

	/**
	 * Update supported extensions<br>
	 * Updates are queued in order to be serialized.
	 */
	public void updateSupportedExtensions() {
		sUpdateSupportedExtensionProcessor.execute(new SupportedExtensionUpdater(mRcsSettings, mSecurityLog, mContext,
				this, mCapabilityMonitoring));
	}

}
