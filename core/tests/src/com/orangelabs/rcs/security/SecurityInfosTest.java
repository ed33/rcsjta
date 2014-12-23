package com.orangelabs.rcs.security;

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
import android.content.ContentResolver;
import android.database.Cursor;
import android.test.AndroidTestCase;

import com.orangelabs.rcs.core.ims.service.extension.IARICertificate;
import com.orangelabs.rcs.provider.security.SecurityInfoData;

public class SecurityInfosTest extends AndroidTestCase {

	private final String WHERE_IARI_CERTIFICATE_CLAUSE = new StringBuilder(SecurityInfoData.KEY_IARI).append("=? AND ")
			.append(SecurityInfoData.KEY_CERT).append("=?").toString();

	private final String[] PROJECTION_ID = new String[] { SecurityInfoData.KEY_ID };

	public static final int INVALID_ID = -1;

	/**
	 * Get row ID for certificate and IARI
	 * 
	 * @param contentResolver
	 * @param iariCertificate
	 *            the IARI and associated certificate
	 * @return id or INVALID_ID if not found
	 */
	int getIdForIariAndCertificate(ContentResolver contentResolver, IARICertificate iariCertificate) {
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(SecurityInfoData.CONTENT_URI, PROJECTION_ID,
					WHERE_IARI_CERTIFICATE_CLAUSE, new String[] { iariCertificate.getIARI(), iariCertificate.getCertificate() },
					null);
			if (cursor.moveToFirst()) {
				return cursor.getInt(cursor.getColumnIndexOrThrow(SecurityInfoData.KEY_ID));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return INVALID_ID;
	}

	/**
	 * Remove all IARI authorizations
	 * 
	 * @param contentResolver
	 * @return The number of rows deleted.
	 */
	int removeAll(ContentResolver contentResolver) {
		return contentResolver.delete(SecurityInfoData.CONTENT_URI, null, null);
	}
}
