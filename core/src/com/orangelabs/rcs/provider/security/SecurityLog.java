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
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * SecurityLog class to manage certificates for IARI range or authorizations for IARI
 * 
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

	private static final String[] PROJ_CERTIFICATE = new String[] { CertificateData.KEY_CERT };

	private static final String WHERE_IARI_RANGE = CertificateData.KEY_IARI_RANGE.concat("=?");

	private static final String[] PROJ_EXTENSION = new String[] { AuthorizationData.KEY_EXT };

	private static final String WHERE_PACKAGE = AuthorizationData.KEY_PACK_NAME.concat("=?");

	private final String WHERE_PACK_EXT_CLAUSE = new StringBuilder(AuthorizationData.KEY_PACK_NAME).append("=? AND ")
			.append(AuthorizationData.KEY_EXT).append("=?").toString();

	private final String[] AUTH_PROJECTION_ID = new String[] { AuthorizationData.KEY_ID };

	public static final int INVALID_ID = -1;

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
	 * @param certificateData
	 */
	public void addCertificate(CertificateData certificateData) {
		String iari = certificateData.getIARIRange();
		if (logger.isActivated()) {
			logger.debug("Add certificate for IARI range ".concat(iari));
		}
		ContentValues values = new ContentValues();
		values.put(CertificateData.KEY_IARI_RANGE, iari);
		values.put(CertificateData.KEY_CERT, certificateData.getCertificate());
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
	 * @return map which key set is the CertificateData instance and the value set is the row IDs
	 */
	public Map<CertificateData, Integer> getAllCertificates() {
		Map<CertificateData, Integer> result = new HashMap<CertificateData, Integer>();
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
				CertificateData ic = new CertificateData(iari, cert);
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
	 * Get certificates for a IARI range
	 * 
	 * @param iariRange
	 * @return set of certificates
	 */
	public Set<String> getCertificatesForIariRange(String iariRange) {
		Set<String> result = new HashSet<String>();
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(CertificateData.CONTENT_URI, PROJ_CERTIFICATE, WHERE_IARI_RANGE,
					new String[] { iariRange }, null);
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
	 * Add authorization
	 * 
	 * @param authData
	 */
	public void addAuthorization(AuthorizationData authData) {
		String extension = authData.getExtension();
		String packageName = authData.getPackageName();

		ContentValues values = new ContentValues();
		values.put(AuthorizationData.KEY_AUTH_TYPE, authData.getAuthType().toInt());
		values.put(AuthorizationData.KEY_SIGNER, authData.getPackageSigner());
		values.put(AuthorizationData.KEY_RANGE, authData.getRange());
		values.put(AuthorizationData.KEY_IARI, authData.getIari());
		Integer id = getIdForPackageNameAndExtension(packageName, extension);
		if (INVALID_ID == id) {
			if (logger.isActivated()) {
				logger.debug("Add authorization for package '" + authData.getPackageName() + "' extension:" + extension);
			}
			values.put(AuthorizationData.KEY_PACK_NAME, packageName);
			values.put(AuthorizationData.KEY_EXT, extension);
			mContentResolver.insert(AuthorizationData.CONTENT_URI, values);
			return;

		}
		if (logger.isActivated()) {
			logger.debug("Update authorization for package '" + authData.getPackageName() + "' extension:" + extension);
		}
		Uri uri = Uri.withAppendedPath(AuthorizationData.CONTENT_URI, id.toString());
		mContentResolver.update(uri, values, null, null);
	}

	/**
	 * Remove a authorization
	 * 
	 * @param id
	 *            the row ID
	 * @return The number of rows deleted.
	 */
	public int removeAuthorization(int id) {
		Uri uri = Uri.withAppendedPath(AuthorizationData.CONTENT_URI, Integer.toString(id));
		return mContentResolver.delete(uri, null, null);
	}

	/**
	 * Get all authorizations
	 * 
	 * @return a map which key set is the AuthorizationData instance and the value set is the row IDs
	 */
	public Map<AuthorizationData, Integer> getAllAuthorizations() {
		Map<AuthorizationData, Integer> result = new HashMap<AuthorizationData, Integer>();
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(AuthorizationData.CONTENT_URI, null, null, null, null);
			if (!cursor.moveToFirst()) {
				return result;

			}
			int idColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_ID);
			int packageColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_PACK_NAME);
			int extensionColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_EXT);
			int iariColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_IARI);
			int authTypeColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_AUTH_TYPE);
			int rangeColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_RANGE);
			int signerColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_SIGNER);

			String iari = null;
			Integer authType = null;
			String range = null;
			String packageName = null;
			String signer = null;
			String extension = null;
			Integer id = null;
			do {
				iari = cursor.getString(iariColumnIdx);
				authType = cursor.getInt(authTypeColumnIdx);
				range = cursor.getString(rangeColumnIdx);
				packageName = cursor.getString(packageColumnIdx);
				signer = cursor.getString(signerColumnIdx);
				extension = cursor.getString(extensionColumnIdx);
				AuthType enumAuthType = AuthType.UNSPECIFIED;
				try {
					enumAuthType = AuthType.valueOf(authType);
				} catch (Exception e) {
					if (logger.isActivated()) {
						logger.error("Invalid authorization type:".concat(Integer.toString(authType)), e);
					}
				}
				id = cursor.getInt(idColumnIdx);
				AuthorizationData ad = new AuthorizationData(packageName, extension, iari, enumAuthType, range, signer);
				result.put(ad, id);
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
	 * Get authorization IDs for a package name
	 * 
	 * @param packageName
	 * @return set of authorization IDs
	 */
	public Set<Integer> getAuthorizationIDsForPackageName(String packageName) {
		Set<Integer> result = new HashSet<Integer>();
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(AuthorizationData.CONTENT_URI, AUTH_PROJECTION_ID, WHERE_PACKAGE,
					new String[] { packageName }, null);
			if (!cursor.moveToFirst()) {
				return result;

			}
			int idColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_ID);
			Integer id = null;
			do {
				id = cursor.getInt(idColumnIdx);
				result.add(id);
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
	 * Get all supported extensions
	 * 
	 * @return set of supported extensions
	 */
	public Set<String> getSupportedExtensions() {
		Set<String> result = new HashSet<String>();
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(AuthorizationData.CONTENT_URI, PROJ_EXTENSION, null, null, null);
			if (!cursor.moveToFirst()) {
				return result;

			}
			int extensionColumnIdx = cursor.getColumnIndexOrThrow(AuthorizationData.KEY_EXT);
			String extension = null;
			do {
				extension = cursor.getString(extensionColumnIdx);
				result.add(extension);
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
	 * Get row ID for authorization
	 * 
	 * @param packageName
	 * @param extension
	 * @return id or INVALID_ID if not found
	 */
	public int getIdForPackageNameAndExtension(String packageName, String extension) {
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(AuthorizationData.CONTENT_URI, AUTH_PROJECTION_ID, WHERE_PACK_EXT_CLAUSE, new String[] {
					packageName, extension }, null);
			if (cursor.moveToFirst()) {
				return cursor.getInt(cursor.getColumnIndexOrThrow(AuthorizationData.KEY_ID));
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Exception occurred", e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return INVALID_ID;
	}

}
