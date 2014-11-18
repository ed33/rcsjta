package com.orangelabs.rcs.test.security;

import java.util.List;

import android.test.AndroidTestCase;

import com.orangelabs.rcs.provider.security.SecurityInfos;

public class SecurityInfoProviderTest extends AndroidTestCase {
	private String cert1 = 
			"MIIDEzCCAfugAwIBAgIERnLjKTANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDEw1t" +
			"Y2MwOTkubW5jMDk5MB4XDTE0MDUxNTA5MTA1NVoXDTE1MDUxMDA5MTA1NVowGDEW" +
			"MBQGA1UEAxMNbWNjMDk5Lm1uYzA5OTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC" +
			"AQoCggEBAIz4aFl0L6yQpBOyPmmcvq96hYoFIMbY6Qi8Fm9foqn6mx4vc2bwXeG3" +
			"7TP4GN9Lq2aUqA6YuLYkVXDo4PSAkZrb41QLwU8bA5KJU6W6uF39bPcBHhfyC4Zp" +
			"dK8tcq3WU2mw7MjPbBnRnlpvgB3pqNowcZ86iDhH1AIQtALlJtiUo9J7eQxiKmG1" +
			"F9IkvpOyugvGALhRQ4YYlzQAqLfuVHHoOq/xJ52wHSLEFY9E6qyT86J36yTexK7K" +
			"/AwXrlRnTAjHLISjY2HQoV/S7aKaz7u1/GWky5lzgqRYXeF60/LEzLziv+LoTL11" +
			"UErs0Eex2MnwCidC4cszddxQvzLJBoMCAwEAAaNlMGMwQgYDVR0RBDswOYY3dXJu" +
			"OnVybi03OjNncHAtYXBwbGljYXRpb24uaW1zLmlhcmkucmNzLm1uYzA5OS5tY2Mw" +
			"OTkuKjAdBgNVHQ4EFgQUAVQqkpd2uNcptB52JiyokJnVgzowDQYJKoZIhvcNAQEL" +
			"BQADggEBAGeSYxLm/wz1j80OmJYq+R99DcTfc2tEoPxzDuKq4dUDLIRNdi1tGKGk" +
			"XLEiSXFZ9Y3zcjYnAGMdE6sgKVcrHlnSk+pGiAwJRapppT5hSuAjzdopUuQ7sbmG" +
			"yAG5b2mPVtPr3EMCYZJVzkMLR1k3XPed0X0TiF+vg11wZjlx8pfe1Z49UTtwWYeI" +
			"qSHjr41kg6dFWwIzT61p0rOmVXX7kd0YSB1xSmIjfkrb7u5s+P090UE2eaWQGKOD" +
			"ELnBHhj4m9we8GHTwcumej0PzBEGUjdTD/Ou/Kl95xg+n6sPE9CLhh27TUNL4Qb5" +
			"EXrnr3X5o6f/OaiWNkOMkQ2hQOsr69E=";				

	private String cert2 = 
			"MIIDEzCCAfugAwIBAgIERnLjKTANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDEw1t" +
			"Y2MwOTkubW5jMDk5MB4XDTE0MDUxNTA5MTA1NVoXDTE1MDUxMDA5MTA1NVowGDEW" +
			"MBQGA1UEAxMNbWNjMDk5Lm1uYzA5OTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC" +
			"AQoCggEBAIz4aFl0L6yQpBOyPmmcvq96hYoFIMbY6Qi8Fm9foqn6mx4vc2bwXeG3" +
			"7TP4GN9Lq2aUqA6YuLYkVXDo4PSAkZrb41QLwU8bA5KJU6W6uF39bPcBHhfyC4Zp" +
			"dK8tcq3WU2mw7MjPbBnRnlpvgB3pqNowcZ86iDhH1AIQtALlJtiUo9J7eQxiKmG1" +
			"F9IkvpOyugvGALhRQ4YYlzQAqLfuVHHoOq/xJ52wHSLEFY9E6qyT86J36yTexK7K" +
			"/AwXrlRnTAjHLISjY2HQoV/S7aKaz7u1/GWky5lzgqRYXeF60/LEzLziv+LoTL11" +
			"UErs0Eex2MnwCidC4cszddxQvzLJBoMCAwEAAaNlMGMwQgYDVR0RBDswOYY3dXJu";				
	
	private String iari1 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo1";
	
	private String iari2 = "urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo2";

	protected void setUp() throws Exception {
		super.setUp();
		
		SecurityInfos.createInstance(getContext());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testAddRemoveAuthorizations() {
		SecurityInfos.getInstance().removeAllIARI();
		
		List<String> result = SecurityInfos.getInstance().getIARICert(iari1);
		assertEquals(result.size(), 0);
		result = SecurityInfos.getInstance().getIARICert(iari2);
		assertEquals(result.size(), 0);
		
		SecurityInfos.getInstance().addIARI(iari1, cert1);
		result = SecurityInfos.getInstance().getIARICert(iari1);
		assertEquals(result.size(), 1);
		assertEquals(result.get(0), cert1);
		
		SecurityInfos.getInstance().addIARI(iari1, cert2);
		result = SecurityInfos.getInstance().getIARICert(iari1);
		assertEquals(result.size(), 2);
		assertEquals(result.get(0), cert1);
		assertEquals(result.get(1), cert2);
		
		SecurityInfos.getInstance().addIARI(iari2, cert1);
		result = SecurityInfos.getInstance().getIARICert(iari2);
		assertEquals(result.size(), 1);
		assertEquals(result.get(0), cert1);
		
		SecurityInfos.getInstance().removeIARI(iari2);
		result = SecurityInfos.getInstance().getIARICert(iari2);
		assertEquals(result.size(), 0);
		
		SecurityInfos.getInstance().removeIARI(iari1);
		result = SecurityInfos.getInstance().getIARICert(iari1);
		assertEquals(result.size(), 0);
	}
}
