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

import java.util.Map;

import android.content.ContentResolver;
import android.test.AndroidTestCase;

import com.gsma.iariauth.validator.IARIAuthDocument.AuthType;
import com.orangelabs.rcs.provider.security.AuthorizationData;
import com.orangelabs.rcs.provider.security.CertificateData;
import com.orangelabs.rcs.provider.security.SecurityLog;

public class SecurityProviderTest extends AndroidTestCase {

	private String cert1 = "certificate1";

	private String cert2 = "certificate2";

	private String iari1 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo1";
	private String range1 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.*";

	private String iari2 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc000.mcc000.demo2";
	private String range2 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc000.mcc000.*";

	private SecurityLog mSecurityInfos;

	private ContentResolver mContentResolver;

	private CertificateData mIariRange1Cert1;
	private CertificateData mIariRange1Cert2;
	private CertificateData mIariRange2Cert1;
	private CertificateData mIariRange2Cert2;

	private AuthorizationData mAuth1;
	private AuthorizationData mAuth2;
	private AuthorizationData mAuth3;
	private AuthorizationData mAuth4;

	private SecurityLibTest mSecurityInfosTest;

	protected void setUp() throws Exception {
		super.setUp();

		mContentResolver = getContext().getContentResolver();
		SecurityLog.createInstance(mContentResolver);
		mSecurityInfos = SecurityLog.getInstance();
		mIariRange1Cert1 = new CertificateData(iari1, cert1);
		mIariRange1Cert2 = new CertificateData(iari1, cert2);
		mIariRange2Cert1 = new CertificateData(iari2, cert1);
		mIariRange2Cert2 = new CertificateData(iari2, cert2);
		mAuth1 = new AuthorizationData("com.orangelabs.package1", "demo1",
				iari1, AuthType.RANGE, range1, "99:99:99");
		mAuth2 = new AuthorizationData("com.orangelabs.package2", "demo2",
				iari2, AuthType.RANGE, range2, "00:00:00");
		mAuth3 = new AuthorizationData("com.orangelabs.package3", "demo3");
		mAuth4 = new AuthorizationData("com.orangelabs.package3", "demo4");
		mSecurityInfosTest = new SecurityLibTest();
		mSecurityInfosTest.removeAllCertificates(mContentResolver);
		mSecurityInfosTest.removeAllAuthorizations(mContentResolver);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		mSecurityInfosTest.removeAllCertificates(mContentResolver);
		mSecurityInfosTest.removeAllAuthorizations(mContentResolver);
	}

	public void testAddCertificate() {
		Map<CertificateData, Integer> map = mSecurityInfos.getAllCertificates();
		assertEquals(0, map.size());

		mSecurityInfos.addCertificate(mIariRange1Cert1);
		Integer id = mSecurityInfosTest.getIdForIariAndCertificate(
				mContentResolver, mIariRange1Cert1);
		assertNotSame(id, SecurityLibTest.INVALID_ID);
		map = mSecurityInfos.getAllCertificates();
		assertEquals(1, map.size());

		assertTrue(map.containsKey(mIariRange1Cert1));

		assertTrue(map.get(mIariRange1Cert1).equals(id));

		mSecurityInfos.addCertificate(mIariRange1Cert1);
		Integer new_id = mSecurityInfosTest.getIdForIariAndCertificate(
				mContentResolver, mIariRange1Cert1);
		assertEquals(id, new_id);

		map = mSecurityInfos.getAllCertificates();
		assertEquals(1, map.size());
		assertTrue(map.containsKey(mIariRange1Cert1));
		assertEquals(map.get(mIariRange1Cert1), id);
	}

	public void testRemoveCertificate() {
		mSecurityInfos.addCertificate(mIariRange1Cert1);
		int id = mSecurityInfosTest.getIdForIariAndCertificate(
				mContentResolver, mIariRange1Cert1);
		assertNotSame(id, SecurityLibTest.INVALID_ID);
		int count = mSecurityInfos.removeCertificate(id);
		assertEquals(1, count);
		Map<CertificateData, Integer> map = mSecurityInfos.getAllCertificates();
		assertEquals(0, map.size());
	}

	public void testGetAllCertificates() {
		mSecurityInfos.addCertificate(mIariRange1Cert1);
		Map<CertificateData, Integer> map = mSecurityInfos.getAllCertificates();
		assertEquals(1, map.size());

		mSecurityInfos.addCertificate(mIariRange1Cert2);
		map = mSecurityInfos.getAllCertificates();
		assertEquals(2, map.size());
		assertTrue(map.containsKey(mIariRange1Cert1));
		assertTrue(map.containsKey(mIariRange1Cert2));

		mSecurityInfos.addCertificate(mIariRange2Cert1);
		map = mSecurityInfos.getAllCertificates();
		assertEquals(3, map.size());
		assertTrue(map.containsKey(mIariRange1Cert1));
		assertTrue(map.containsKey(mIariRange1Cert2));
		assertTrue(map.containsKey(mIariRange2Cert1));

		mSecurityInfos.addCertificate(mIariRange2Cert2);
		map = mSecurityInfos.getAllCertificates();
		assertEquals(4, map.size());
		assertTrue(map.containsKey(mIariRange1Cert1));
		assertTrue(map.containsKey(mIariRange1Cert2));
		assertTrue(map.containsKey(mIariRange2Cert1));
		assertTrue(map.containsKey(mIariRange2Cert2));
	}

	public void testGetAllAuthorizations() {
		Map<AuthorizationData, Integer> authorizationDatas = mSecurityInfos
				.getAllAuthorizations();
		assertEquals(0, authorizationDatas.size());

		mSecurityInfos.addAuthorization(mAuth1);
		authorizationDatas = mSecurityInfos.getAllAuthorizations();
		assertEquals(1, authorizationDatas.size());
		assertTrue(authorizationDatas.containsKey(mAuth1));

		mSecurityInfos.addAuthorization(mAuth2);
		authorizationDatas = mSecurityInfos.getAllAuthorizations();
		assertEquals(2, authorizationDatas.size());
		assertTrue(authorizationDatas.containsKey(mAuth1));
		assertTrue(authorizationDatas.containsKey(mAuth2));

		mSecurityInfos.addAuthorization(mAuth3);
		authorizationDatas = mSecurityInfos.getAllAuthorizations();
		assertEquals(3, authorizationDatas.size());
		assertTrue(authorizationDatas.containsKey(mAuth1));
		assertTrue(authorizationDatas.containsKey(mAuth2));
		assertTrue(authorizationDatas.containsKey(mAuth3));

		mSecurityInfos.addAuthorization(mAuth4);
		authorizationDatas = mSecurityInfos.getAllAuthorizations();
		assertEquals(4, authorizationDatas.size());
		assertTrue(authorizationDatas.containsKey(mAuth1));
		assertTrue(authorizationDatas.containsKey(mAuth2));
		assertTrue(authorizationDatas.containsKey(mAuth3));
		assertTrue(authorizationDatas.containsKey(mAuth4));
	}

	public void testAddAuthorization() {
		mSecurityInfos.addAuthorization(mAuth1);
		Integer id = mSecurityInfos.getIdForPackageNameAndExtension(
				"com.orangelabs.package1", "demo1");
		assertNotSame(id, SecurityLibTest.INVALID_ID);

		Map<AuthorizationData, Integer> authorizationDatas = mSecurityInfos
				.getAllAuthorizations();
		assertEquals(1, authorizationDatas.size());

		assertTrue(authorizationDatas.containsKey(mAuth1));

		assertEquals(id, authorizationDatas.get(mAuth1));

		mSecurityInfos.addAuthorization(mAuth1);
		Integer new_id = mSecurityInfos.getIdForPackageNameAndExtension(
				"com.orangelabs.package1", "demo1");
		assertEquals(id, new_id);

		authorizationDatas = mSecurityInfos.getAllAuthorizations();
		assertEquals(1, authorizationDatas.size());
		assertTrue(authorizationDatas.containsKey(mAuth1));
		assertEquals(authorizationDatas.get(mAuth1), id);
	}

	public void testRemoveAuthorization() {
		mSecurityInfos.addAuthorization(mAuth1);
		int id = mSecurityInfos.getIdForPackageNameAndExtension(
				"com.orangelabs.package1", "demo1");
		assertNotSame(id, SecurityLibTest.INVALID_ID);
		int count = mSecurityInfos.removeAuthorization(id);
		assertEquals(1, count);
		Map<AuthorizationData, Integer> map = mSecurityInfos
				.getAllAuthorizations();
		assertEquals(0, map.size());
	}
}
