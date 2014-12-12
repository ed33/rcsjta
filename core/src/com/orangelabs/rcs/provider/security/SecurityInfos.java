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

import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
//import android.net.Uri;




import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Security infos
 * 
 * @author jexa7410
 * @author yplo6403
 *
 */
public class SecurityInfos {
	/**
	 * Current instance
	 */
	private static volatile SecurityInfos sInstance;

	/**
	 * Content resolver
	 */
	private final ContentResolver mContentResolver;

	private final String WHERE_IARI_CLAUSE = SecurityInfoData.KEY_IARI.concat("=?");
	@SuppressWarnings("unused")
	private final String WHERE_IARI_CERTIFICATE_CLAUSE = new StringBuilder(SecurityInfoData.KEY_IARI).append("=? AND ")
			.append(SecurityInfoData.KEY_CERT).append("=?").toString();

	private final String[] PROJECTION_CERTIFICATE = new String[] { SecurityInfoData.KEY_CERT };

	@SuppressWarnings("unused")
	private final String[] PROJECTION_ID = new String[] { SecurityInfoData.KEY_ID };

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(SecurityInfos.class.getSimpleName());

	@SuppressWarnings("unused")
	private static final int INVALID_ID = -1;

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
		synchronized (SecurityInfos.class) {
			if (sInstance == null) {
				sInstance = new SecurityInfos(ContentResolver);
			}
		}
	}

	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static SecurityInfos getInstance() {
		return sInstance;
	}

	/**
	 * Empty constructor : prevent caller from creating multiple instances
	 */
	@SuppressWarnings("unused")
	private SecurityInfos() {
		mContentResolver = null;
	}

	/**
	 * Constructor
	 * 
	 * @param ContentResolver
	 *            content resolver
	 */
	public SecurityInfos(ContentResolver ContentResolver) {
		mContentResolver = ContentResolver;
	}

	/**
	 * Add a certificate for IARI
	 * 
	 * @param iari
	 *            IARI
	 * @param cert
	 *            Certificate
	 */
	public void addCertificateForIARI(String iari, String cert) {
		if (logger.isActivated()) {
			logger.debug("Add certificate for IARI ".concat(iari));
		}
		ContentValues values = new ContentValues();
		values.put(SecurityInfoData.KEY_IARI, iari);
		values.put(SecurityInfoData.KEY_CERT, cert);
		mContentResolver.insert(SecurityInfoData.CONTENT_URI, values);
	}

	/**
	 * Remove a certificate for IARI
	 * 
	 * @param iari
	 *            IARI
	 * @param cert
	 *            Certificate
	 * @return The number of rows deleted.
	 */
	/*
	public int removeCertificateForIARI(String iari, String cert) {
		if (logger.isActivated()) {
			logger.debug("Remove certificate for IARI ".concat(iari));
		}
		// Check if certificate exists
		Integer id = getIdForIariAndCertificate(iari, cert);
		if (id == INVALID_ID) {
			return 0;

		}
		Uri uri = Uri.withAppendedPath(SecurityInfoData.CONTENT_URI, id.toString());
		return mContentResolver.delete(uri, null, null);
	}
	*/
	
	/**
	 * Remove all IARI authorizations
	 * 
	 * @return The number of rows deleted.
	 */
	public int removeIARIs() {
		if (logger.isActivated()) {
			logger.debug("Remove all IARIs");
		}
		return mContentResolver.delete(SecurityInfoData.CONTENT_URI, null, null);
	}

	/**
	 * get Id for IARI and certificate
	 * 
	 * @param iari
	 * @param cert
	 * @return id or INVALID_ID if it does not exists
	 */
	/*
	private int getIdForIariAndCertificate(String iari, String cert) {
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(SecurityInfoData.CONTENT_URI, PROJECTION_ID, WHERE_IARI_CERTIFICATE_CLAUSE,
					new String[] { iari, cert }, null);
			if (cursor.moveToFirst()) {
				return cursor.getInt(cursor.getColumnIndexOrThrow(SecurityInfoData.KEY_ID));
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
	*/

	
	/**
	 * Remove a IARI authorization
	 * 
	 * @param iari
	 *            IARI
	 * @return The number of certificates deleted for this IARI
	 */
	public int removeIARI(String iari) {
		if (logger.isActivated()) {
			logger.debug("Remove IARI ".concat(iari));
		}
		return mContentResolver.delete(SecurityInfoData.CONTENT_URI, WHERE_IARI_CLAUSE, new String[] { iari });
	}
	
	/**
	 * Returns a set of certificates associated to a IARI
	 * 
	 * @param iari
	 *            IARI
	 * @return Set of certificates
	 */
	public Set<String> getCertificatesForIARI(String iari) {
		boolean isLogAtive = logger.isActivated();
		if (isLogAtive) {
			logger.debug("Get certificates for IARI ".concat(iari));
		}

		Set<String> result = new HashSet<String>();
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(SecurityInfoData.CONTENT_URI, PROJECTION_CERTIFICATE, WHERE_IARI_CLAUSE,
					new String[] { iari }, null);
			if (!cursor.moveToFirst()) {
				return result;

			}
			int columnIdx = cursor.getColumnIndexOrThrow(SecurityInfoData.KEY_CERT);
			do {
				String cert = cursor.getString(columnIdx);
				result.add(cert);
			} while (cursor.moveToNext());
		} catch (Exception e) {
			if (isLogAtive) {
				logger.error("Exception occurred", e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}
}
