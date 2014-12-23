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
package com.orangelabs.rcs.security;

import android.test.AndroidTestCase;

import com.orangelabs.rcs.core.ims.service.extension.IARIRangeCertificate;

public class IariCertificateTest extends AndroidTestCase {

	private String formattedCertif = "-----BEGIN CERTIFICATE-----\r\nMIIDEzCCAfugAwIBAgIEcSw+/jANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDEw1t\r\n9VKv43N29kYEpKjmx1x1X8NPftPHGqI=\r\n-----END CERTIFICATE-----\r\n";

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFormatCertificateCanFormatTwice() {
		assertEquals(formattedCertif, IARIRangeCertificate.format(formattedCertif));
	}

	public void testFormatCertificateAddHeader() {
		assertEquals(
				formattedCertif,
				IARIRangeCertificate.format("MIIDEzCCAfugAwIBAgIEcSw+/jANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDEw1t\r\n9VKv43N29kYEpKjmx1x1X8NPftPHGqI=\r\n-----END CERTIFICATE-----\r\n"));
	}

	public void testFormatCertificateAddFooter() {
		assertEquals(
				formattedCertif,
				IARIRangeCertificate.format("-----BEGIN CERTIFICATE-----\r\nMIIDEzCCAfugAwIBAgIEcSw+/jANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDEw1t\r\n9VKv43N29kYEpKjmx1x1X8NPftPHGqI=\r\n"));
	}

	public void testFormatCertificateRemoveTabs() {
		assertEquals(
				formattedCertif,
				IARIRangeCertificate.format("-----BEGIN CERTIFICATE-----\r\nMIIDEzCCAfugAwIBAgIEcSw+/jANBgkqhkiG9w0BA\tQsFADAYMRYwFAYDVQQDEw1t\r\n9VKv43N29kYEpKjmx1x\t1X8NPftPHGqI=\r\n-----END CERTIFICATE-----\r\n"));
	}
	
	public void testFormatCertificateRemoveSpaces() {
		assertEquals(
				formattedCertif,
				IARIRangeCertificate.format("-----BEGIN CERTIFICATE-----\r\nMIIDEzCCAfugAwIBA  gIEcSw+/jANBgkqhkiG9w0BA\tQsFADAYM  RYwFAYDVQQDEw1t\r\n9VKv43N29k  YEpKjmx1x\t1X8NPftP  HGqI=\r\n-----END CERTIFICATE-----\r\n"));
	}

	public void testFormatCertificateRemoveCRLF() {
		assertEquals(
				formattedCertif,
				IARIRangeCertificate.format("-----BEGIN CERTIFICATE-----\r\nMIIDEzCCAfugAwIBA  gIEcSw+/jANBg\r\nkqhkiG9w0BA\tQsFADAYM  RYwFA\rYDVQQDEw1t\r\n9VKv43N29k  YEpKjmx1x\t1X8NPftP  HGqI=\r\n-----END CERTIFICATE-----\r\n"));
	}
}
