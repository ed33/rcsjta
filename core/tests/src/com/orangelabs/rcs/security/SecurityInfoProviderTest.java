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
import java.util.Set;

import android.content.ContentResolver;
import android.test.AndroidTestCase;
import android.util.Log;

import com.orangelabs.rcs.core.ims.service.extension.IARICertificate;
import com.orangelabs.rcs.provider.security.SecurityInfos;

public class SecurityInfoProviderTest extends AndroidTestCase {
	
	private String cert1 = "MIIDEzCCAfugAwIBAgIERnLjKTANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDEw1t"
			+ "Y2MwOTkubW5jMDk5MB4XDTE0MDUxNTA5MTA1NVoXDTE1MDUxMDA5MTA1NVowGDEW"
			+ "MBQGA1UEAxMNbWNjMDk5Lm1uYzA5OTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC"
			+ "AQoCggEBAIz4aFl0L6yQpBOyPmmcvq96hYoFIMbY6Qi8Fm9foqn6mx4vc2bwXeG3"
			+ "7TP4GN9Lq2aUqA6YuLYkVXDo4PSAkZrb41QLwU8bA5KJU6W6uF39bPcBHhfyC4Zp"
			+ "dK8tcq3WU2mw7MjPbBnRnlpvgB3pqNowcZ86iDhH1AIQtALlJtiUo9J7eQxiKmG1"
			+ "F9IkvpOyugvGALhRQ4YYlzQAqLfuVHHoOq/xJ52wHSLEFY9E6qyT86J36yTexK7K"
			+ "/AwXrlRnTAjHLISjY2HQoV/S7aKaz7u1/GWky5lzgqRYXeF60/LEzLziv+LoTL11"
			+ "UErs0Eex2MnwCidC4cszddxQvzLJBoMCAwEAAaNlMGMwQgYDVR0RBDswOYY3dXJu"
			+ "OnVybi03OjNncHAtYXBwbGljYXRpb24uaW1zLmlhcmkucmNzLm1uYzA5OS5tY2Mw"
			+ "OTkuKjAdBgNVHQ4EFgQUAVQqkpd2uNcptB52JiyokJnVgzowDQYJKoZIhvcNAQEL"
			+ "BQADggEBAGeSYxLm/wz1j80OmJYq+R99DcTfc2tEoPxzDuKq4dUDLIRNdi1tGKGk"
			+ "XLEiSXFZ9Y3zcjYnAGMdE6sgKVcrHlnSk+pGiAwJRapppT5hSuAjzdopUuQ7sbmG"
			+ "yAG5b2mPVtPr3EMCYZJVzkMLR1k3XPed0X0TiF+vg11wZjlx8pfe1Z49UTtwWYeI"
			+ "qSHjr41kg6dFWwIzT61p0rOmVXX7kd0YSB1xSmIjfkrb7u5s+P090UE2eaWQGKOD"
			+ "ELnBHhj4m9we8GHTwcumej0PzBEGUjdTD/Ou/Kl95xg+n6sPE9CLhh27TUNL4Qb5"
			+ "EXrnr3X5o6f/OaiWNkOMkQ2hQOsr69E=";

	private String cert2 = "MIIDEzCCAfugAwIBAgIERnLjKTANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDEw1t"
			+ "Y2MwOTkubW5jMDk5MB4XDTE0MDUxNTA5MTA1NVoXDTE1MDUxMDA5MTA1NVowGDEW"
			+ "MBQGA1UEAxMNbWNjMDk5Lm1uYzA5OTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC"
			+ "AQoCggEBAIz4aFl0L6yQpBOyPmmcvq96hYoFIMbY6Qi8Fm9foqn6mx4vc2bwXeG3"
			+ "7TP4GN9Lq2aUqA6YuLYkVXDo4PSAkZrb41QLwU8bA5KJU6W6uF39bPcBHhfyC4Zp"
			+ "dK8tcq3WU2mw7MjPbBnRnlpvgB3pqNowcZ86iDhH1AIQtALlJtiUo9J7eQxiKmG1"
			+ "F9IkvpOyugvGALhRQ4YYlzQAqLfuVHHoOq/xJ52wHSLEFY9E6qyT86J36yTexK7K"
			+ "/AwXrlRnTAjHLISjY2HQoV/S7aKaz7u1/GWky5lzgqRYXeF60/LEzLziv+LoTL11"
			+ "UErs0Eex2MnwCidC4cszddxQvzLJBoMCAwEAAaNlMGMwQgYDVR0RBDswOYY3dXJu";
	
	
	private String iari1 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo1";

	private String iari2 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo2";

	private SecurityInfos mSecurityInfos;

	private ContentResolver mContentResolver;

	private IARICertificate mIari1Cert1;
	private IARICertificate mIari1Cert2;
	private IARICertificate mIari2Cert1;
	private IARICertificate mIari2Cert2;

	private SecurityInfosTest mSecurityInfosTest;

	protected void setUp() throws Exception {
		super.setUp();

		mContentResolver = getContext().getContentResolver();
		SecurityInfos.createInstance(mContentResolver);
		mSecurityInfos = SecurityInfos.getInstance();
		mIari1Cert1 = new IARICertificate(iari1, cert1);
		mIari1Cert2 = new IARICertificate(iari1, cert2);
		mIari2Cert1 = new IARICertificate(iari2, cert1);
		mIari2Cert2 = new IARICertificate(iari2, cert2);
		mSecurityInfosTest = new SecurityInfosTest();
		mSecurityInfosTest.removeAll(mContentResolver);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		mSecurityInfosTest.removeAll(mContentResolver);
	}

	public void testAddAuthorizations() {
		Map<IARICertificate, Integer> map = mSecurityInfos.getAll();
		assertEquals(0, map.size());

		Log.w("[TEST]", "before "+mIari1Cert1.toString());
		
		mSecurityInfos.addCertificateForIARI(mIari1Cert1);
		Integer id = mSecurityInfosTest.getIdForIariAndCertificate(mContentResolver, mIari1Cert1);
		assertNotSame(id, SecurityInfosTest.INVALID_ID);
		Log.w("[TEST]", "ID="+id);
		map = mSecurityInfos.getAll();
		assertEquals(1, map.size());
		Set<IARICertificate> certificates = map.keySet();
		for (IARICertificate iariCertificate : certificates) {
			Log.w("[TEST]", "between "+iariCertificate.toString());
		}
		Log.w("[TEST]", "after "+mIari1Cert1.toString());
		assertTrue(map.containsKey(mIari1Cert1));

		assertTrue(map.get(mIari1Cert1).equals(id));
		

		mSecurityInfos.addCertificateForIARI(mIari1Cert1);
		Integer new_id = mSecurityInfosTest.getIdForIariAndCertificate(mContentResolver, mIari1Cert1);
		assertEquals(id, new_id);
		assertNotSame(id, SecurityInfosTest.INVALID_ID);
		map = mSecurityInfos.getAll();
		assertEquals(1, map.size());
		assertTrue(map.containsKey(mIari1Cert1));
		assertEquals(map.get(mIari1Cert1), id);
	}

	public void testRemoveAuthorizations() {
		mSecurityInfos.addCertificateForIARI(mIari1Cert1);
		int id = mSecurityInfosTest.getIdForIariAndCertificate(mContentResolver, mIari1Cert1);
		assertNotSame(id, SecurityInfosTest.INVALID_ID);
		int count = mSecurityInfos.removeCertificate(id);
		assertEquals(1, count);
		Map<IARICertificate, Integer> map = mSecurityInfos.getAll();
		assertEquals(0, map.size());
	}

	public void testGetAllAuthorizations() {
		mSecurityInfos.addCertificateForIARI(mIari1Cert1);
		Map<IARICertificate, Integer> map = mSecurityInfos.getAll();
		assertEquals(1, map.size());

		mSecurityInfos.addCertificateForIARI(mIari1Cert2);
		map = mSecurityInfos.getAll();
		assertEquals(2, map.size());
		assertTrue(map.containsKey(mIari1Cert1));
		assertTrue(map.containsKey(mIari1Cert2));

		mSecurityInfos.addCertificateForIARI(mIari2Cert1);
		map = mSecurityInfos.getAll();
		assertEquals(3, map.size());
		assertTrue(map.containsKey(mIari1Cert1));
		assertTrue(map.containsKey(mIari1Cert2));
		assertTrue(map.containsKey(mIari2Cert1));

		mSecurityInfos.addCertificateForIARI(mIari2Cert2);
		map = mSecurityInfos.getAll();
		assertEquals(4, map.size());
		assertTrue(map.containsKey(mIari1Cert1));
		assertTrue(map.containsKey(mIari1Cert2));
		assertTrue(map.containsKey(mIari2Cert1));
		assertTrue(map.containsKey(mIari2Cert2));
	}
}
