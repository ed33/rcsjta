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
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.orangelabs.rcs.core.ims.service.extension.IARICertificate;
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

	private final String WHERE_IARI_CERTIFICATE_CLAUSE = new StringBuilder(SecurityInfoData.KEY_IARI).append("=? AND ")
			.append(SecurityInfoData.KEY_CERT).append("=?").toString();

	private final String[] PROJECTION_ID = new String[] { SecurityInfoData.KEY_ID };

	public static final int INVALID_ID = -1;

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(SecurityInfos.class.getSimpleName());

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
	 * @param iariCertificate
	 */
	public void addCertificateForIARI(IARICertificate iariCertificate) {
		String iari = iariCertificate.getIARI();
		if (logger.isActivated()) {
			logger.debug("Add certificate for IARI ".concat(iari));
		}
		ContentValues values = new ContentValues();
		values.put(SecurityInfoData.KEY_IARI, iari);
		values.put(SecurityInfoData.KEY_CERT, iariCertificate.getCertificate());
		mContentResolver.insert(SecurityInfoData.CONTENT_URI, values);
	}

	/**
	 * Remove a certificate for IARI
	 * 
	 * @param id
	 *            the row ID
	 * @return The number of rows deleted.
	 */
	public int removeCertificate(int id) {
		Uri uri = Uri.withAppendedPath(SecurityInfoData.CONTENT_URI, Integer.toString(id));
		return mContentResolver.delete(uri, null, null);
	}

	/**
	 * Get row ID for certificate and IARI
	 * 
	 * @param iariCertificate
	 *            the IARI and associated certificate
	 * @return id or INVALID_ID if not found
	 */
	public int getIdForIariAndCertificate(IARICertificate iariCertificate) {
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(SecurityInfoData.CONTENT_URI, PROJECTION_ID, WHERE_IARI_CERTIFICATE_CLAUSE,
					new String[] { iariCertificate.getIARI(), iariCertificate.getCertificate() }, null);
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

	/**
	 * Get all IARI authorizations
	 * 
	 * @return a map which key set is the IARI / Certificate pairs and the value set is the row IDs
	 */
	public Map<IARICertificate, Integer> getAll() {
		Map<IARICertificate, Integer> result = new HashMap<IARICertificate, Integer>();
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(SecurityInfoData.CONTENT_URI, null, null, null, null);
			if (!cursor.moveToFirst()) {
				return result;

			}
			int certColumnIdx = cursor.getColumnIndexOrThrow(SecurityInfoData.KEY_CERT);
			int idColumnIdx = cursor.getColumnIndexOrThrow(SecurityInfoData.KEY_ID);
			int iariColumnIdx = cursor.getColumnIndexOrThrow(SecurityInfoData.KEY_IARI);
			String cert = null;
			Integer id = null;
			String iari = null;
			do {
				cert = cursor.getString(certColumnIdx);
				id = cursor.getInt(idColumnIdx);
				iari = cursor.getString(iariColumnIdx);
				IARICertificate ic = new IARICertificate(iari, cert);
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

}
