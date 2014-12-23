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

import com.orangelabs.rcs.core.ims.service.extension.IARIRangeCertificate;
import com.orangelabs.rcs.provider.security.AuthorizationData;
import com.orangelabs.rcs.provider.security.CertificateData;

public class SecurityLibTest extends AndroidTestCase {

	private final String WHERE_IARI_RANGE_CERT_CLAUSE = new StringBuilder(CertificateData.KEY_IARI_RANGE).append("=? AND ")
			.append(CertificateData.KEY_CERT).append("=?").toString();

	private final String[] PROJECTION_ID = new String[] { CertificateData.KEY_ID };

	public static final int INVALID_ID = -1;

	/**
	 * Get row ID for certificate and IARI
	 * 
	 * @param contentResolver
	 * @param iariCertificate
	 *            the IARI and associated certificate
	 * @return id or INVALID_ID if not found
	 */
	int getIdForIariAndCertificate(ContentResolver contentResolver, IARIRangeCertificate iariCertificate) {
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(CertificateData.CONTENT_URI, PROJECTION_ID,
					WHERE_IARI_RANGE_CERT_CLAUSE, new String[] { iariCertificate.getIARIRange(), iariCertificate.getCertificate() },
					null);
			if (cursor.moveToFirst()) {
				return cursor.getInt(cursor.getColumnIndexOrThrow(CertificateData.KEY_ID));
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
	 * Remove all IARI certificates
	 * 
	 * @param contentResolver
	 * @return The number of rows deleted.
	 */
	int removeAllCertificates(ContentResolver contentResolver) {
		return contentResolver.delete(CertificateData.CONTENT_URI, null, null);
	}
	
	/**
	 * Remove all IARI certificates
	 * 
	 * @param contentResolver
	 * @return The number of rows deleted.
	 */
	int removeAllAuthorizations(ContentResolver contentResolver) {
		return contentResolver.delete(AuthorizationData.CONTENT_URI, null, null);
	}
}
