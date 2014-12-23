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
package com.orangelabs.rcs.provider.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.iariauth.validator.IARIAuthDocument.AuthType;
import com.orangelabs.rcs.core.ims.service.extension.IARIRangeCertificate;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * SecurityLog class to manage certificates for IARI range or authorizations for IARI
 * 
 * @author jexa7410
 * @author yplo6403
 *
 */
public class SecurityLog {
	/**
	 * Current instance
	 */
	private static volatile SecurityLog sInstance;

	/**
	 * Content resolver
	 */
	private final ContentResolver mContentResolver;

	private final String[] PROJ_CERTIFICATE = new String[] { CertificateData.KEY_CERT };

	private static final String WHERE_IARI_RANGE = CertificateData.KEY_IARI_RANGE.concat("=?");

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(SecurityLog.class.getSimpleName());


	/**
	 * Create instance
	 * 
	 * @param ContentResolver
	 *            content resolver
	 */
	public static void createInstance(ContentResolver ContentResolver) {
		if (sInstance != null) {
			return;

		}
		synchronized (SecurityLog.class) {
			if (sInstance == null) {
				sInstance = new SecurityLog(ContentResolver);
			}
		}
	}

	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static SecurityLog getInstance() {
		return sInstance;
	}

	/**
	 * Empty constructor : prevent caller from creating multiple instances
	 */
	@SuppressWarnings("unused")
	private SecurityLog() {
		mContentResolver = null;
	}

	/**
	 * Constructor
	 * 
	 * @param ContentResolver
	 *            content resolver
	 */
	public SecurityLog(ContentResolver ContentResolver) {
		mContentResolver = ContentResolver;
	}

	/**
	 * Add a certificate for IARI range
	 * 
	 * @param iariCertificate
	 */
	public void addCertificateForIARIRange(IARIRangeCertificate iariCertificate) {
		String iari = iariCertificate.getIARIRange();
		if (logger.isActivated()) {
			logger.debug("Add certificate for IARI range ".concat(iari));
		}
		ContentValues values = new ContentValues();
		values.put(CertificateData.KEY_IARI_RANGE, iari);
		values.put(CertificateData.KEY_CERT, iariCertificate.getCertificate());
		mContentResolver.insert(CertificateData.CONTENT_URI, values);
	}

	/**
	 * Remove a certificate for IARI range
	 * 
	 * @param id
	 *            the row ID
	 * @return The number of rows deleted.
	 */
	public int removeCertificate(int id) {
		Uri uri = Uri.withAppendedPath(CertificateData.CONTENT_URI, Integer.toString(id));
		return mContentResolver.delete(uri, null, null);
	}

	/**
	 * Get all IARI range certificates
	 * 
	 * @return a map which key set is the IARI range / Certificate pairs and the value set is the row IDs
	 */
	public Map<IARIRangeCertificate, Integer> getAllCertificates() {
		Map<IARIRangeCertificate, Integer> result = new HashMap<IARIRangeCertificate, Integer>();
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(CertificateData.CONTENT_URI, null, null, null, null);
			if (!cursor.moveToFirst()) {
				return result;

			}
			int certColumnIdx = cursor.getColumnIndexOrThrow(CertificateData.KEY_CERT);
			int idColumnIdx = cursor.getColumnIndexOrThrow(CertificateData.KEY_ID);
			int iariColumnIdx = cursor.getColumnIndexOrThrow(CertificateData.KEY_IARI_RANGE);
			String cert = null;
			Integer id = null;
			String iari = null;
			do {
				cert = cursor.getString(certColumnIdx);
				id = cursor.getInt(idColumnIdx);
				iari = cursor.getString(iariColumnIdx);
				IARIRangeCertificate ic = new IARIRangeCertificate(iari, cert);
				result.put(ic, id);
			} while (cursor.moveToNext());
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Exception occurred", e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	/**
	 * Get all certificates for a IARI range
	 * 
	 * @param iariRange
	 * @return set of certificates
	 */
	public Set<String> getAllCertificatesForIariRange(String iariRange) {
		Set<String> result = new HashSet<String>();
		Cursor cursor = null;
		try {
			cursor = mContentResolver
					.query(CertificateData.CONTENT_URI, PROJ_CERTIFICATE, WHERE_IARI_RANGE, new String[] { iariRange }, null);
			if (!cursor.moveToFirst()) {
				return result;

			}
			int certColumnIdx = cursor.getColumnIndexOrThrow(CertificateData.KEY_CERT);
			String cert = null;
			do {
				cert = cursor.getString(certColumnIdx);
				result.add(cert);
			} while (cursor.moveToNext());
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Exception occurred", e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}
	
	/**
	 * Set authorization for IARI<br>
	 * Add authorization if it does not exists else update
	 * @param authData
	 */
	public void setAuthorizationForIARI(AuthorizationData authData) {
		String iari = authData.getIari();
		ContentValues values = new ContentValues();
		values.put(AuthorizationData.KEY_PACK_NAME, authData.getPackageName());
		values.put(AuthorizationData.KEY_AUTH_TYPE, authData.getAuthType().toInt());
		values.put(AuthorizationData.KEY_SIGNER, authData.getPackageSigner());
		values.put(AuthorizationData.KEY_RANGE, authData.getRange());
		values.put(AuthorizationData.KEY_EXTENSION, authData.getExtension());
		if (!doesAuthroizationExistForIari(iari)) {
			if (logger.isActivated()) {
				logger.debug("Add authorization for IARI ".concat(authData.getIari()));
			}
			values.put(AuthorizationData.KEY_IARI, iari);
			mContentResolver.insert(AuthorizationData.CONTENT_URI, values);
			return;
		}
		if (logger.isActivated()) {
			logger.debug("Update authorization for IARI ".concat(authData.getIari()));
		}
		Uri uri = Uri.withAppendedPath(AuthorizationData.CONTENT_URI, iari);
		mContentResolver.update(uri, values, null, null);
	}
	
	/**
	 * Remove a authorization for IARI
	 * 
	 * @param iari
	 * @return The number of rows deleted.
	 */
	public int removeAuthorization(String iari) {
		Uri uri = Uri.withAppendedPath(AuthorizationData.CONTENT_URI, iari);
		return mContentResolver.delete(uri, null, null);
	}

	/**
	 * Get all IARI authorizations
	 * 
	 * @return a set of authorization data
	 */
	public Set<AuthorizationData> getAllAuthorizations() {
		Set<AuthorizationData> result = new HashSet<AuthorizationData>();
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(AuthorizationData.CONTENT_URI, null, null, null, null);
			if (!cursor.moveToFirst()) {
				return result;

			}
			int iariColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_IARI);
			int authTypeColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_AUTH_TYPE);
			int rangeColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_RANGE);
			int packageColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_NAME);
			int signerColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_SIGNER);
			int extensionColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_EXTENSION);
			String iari = null;
			Integer authType = null;
			String range = null;
			String packageName = null;
			String signer = null;
			String extension = null;
			do {
				iari = cursor.getString(iariColumnIdx);
				authType = cursor.getInt(authTypeColumnIdx);
				range = cursor.getString(rangeColumnIdx);
				packageName = cursor.getString(packageColumnIdx);
				signer = cursor.getString(signerColumnIdx);
				extension = cursor.getString(extensionColumnIdx);
				AuthType enumAuthType = AuthType.RANGE;
				try {
					enumAuthType = AuthType.valueOf(authType);
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Invalid authorization type:".concat(Integer.toString(authType)), e);
					}
				}

				AuthorizationData ad = new AuthorizationData(iari, enumAuthType, range, packageName, signer, extension);
				result.add(ad);
			} while (cursor.moveToNext());
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Exception occurred", e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}
	
	/**
	 * Check if authorization exists for IARI
	 * 
	 * @param iari
	 *            the IARI 
	 * @return True if authorization exists for IARI
	 */
	boolean doesAuthroizationExistForIari(String iari) {
		Uri uri = Uri.withAppendedPath(AuthorizationData.CONTENT_URI, iari);
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(uri, null, null, null, null);
			return cursor.moveToFirst();
		} catch (Exception e) {
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
