/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.core.ims.service.extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gsma.iariauth.validator.IARIAuthDocument.AuthType;
import com.orangelabs.rcs.provider.security.AuthorizationData;
import com.orangelabs.rcs.provider.security.CertificateData;
import com.orangelabs.rcs.provider.security.SecurityLog;

/**
 * A Class to update security provider only once provisioning is fully parsed
 * 
 * @author P.LEMORDANT
 *
 */
public class CertificateProvisioning implements ICertificateProvisioningListener {

	private SecurityLog mSecurityLog;

	private Map<CertificateData, Integer> mCertiticatesBeforeProvisioning;

	private Set<CertificateData> mCertificatesAfterProvisioning;

	/**
	 * Constructor
	 * @param securityLog
	 */
	public CertificateProvisioning(SecurityLog securityLog) {
		mSecurityLog = securityLog;
	}

	@Override
	public void start() {
		// Check if not already started
		if (mCertiticatesBeforeProvisioning != null) {
			return;

		}
		// Save certificates before provisioning
		mCertiticatesBeforeProvisioning = mSecurityLog.getAllCertificates();
		// No certificates yet newly provisioned
		mCertificatesAfterProvisioning = new HashSet<CertificateData>();
	}

	@Override
	public void stop() {
		boolean newCertificate = false;
		
		// Check if not already stopped or never started
		if (mCertiticatesBeforeProvisioning == null) {
			return;

		}
		// Check for new Certificates
		for (CertificateData iariRangeCertificate : mCertificatesAfterProvisioning) {
			if (!mCertiticatesBeforeProvisioning.containsKey(iariRangeCertificate)) {
				// new certificate: add to provider
				mSecurityLog.addCertificate(iariRangeCertificate);
				newCertificate = true;
			}
		}

		// Check for revoked certificates
		mCertiticatesBeforeProvisioning.entrySet().removeAll(mCertificatesAfterProvisioning);
		for (CertificateData iariRangeCertificate : mCertiticatesBeforeProvisioning.keySet()) {
			// revoked certificate: remove from provider
			mSecurityLog.removeCertificate(mCertiticatesBeforeProvisioning.get(iariRangeCertificate));
		}
		
		// Only stop provisioning once
		mCertiticatesBeforeProvisioning = null;
		
		// Compile set of IARI ranges
		Set<String> iariRanges = new HashSet<String>();
		for (CertificateData iariRangeCertificate : mCertificatesAfterProvisioning) {
			iariRanges.add(iariRangeCertificate.getIARIRange());
		}
		
		// Remove from authorizations for which IARI range is not provisioned
		Map<AuthorizationData, Integer> authorizationDatas = mSecurityLog.getAllAuthorizations();
		for (AuthorizationData authorizationData : authorizationDatas.keySet()) {
			// Only consider authorizations of type RANGE
			if (!AuthType.RANGE.equals(authorizationData.getAuthType())) {
				continue;

			}
			if (!iariRanges.contains(authorizationData.getRange())) {
				mSecurityLog.removeAuthorization(authorizationDatas.get(authorizationData));
			}
		}
		if (newCertificate) {
			// The supported extensions need to be reevaluated
			ExtensionManager extensionManager = ExtensionManager.getInstance();
			if (extensionManager != null) {
				extensionManager.updateSupportedExtensions();
			}
		}
	}

	@Override
	public void addNewCertificate(String iari, String certificate) {
		// Add IARI / Certificate in memory
		// Format certificate
		mCertificatesAfterProvisioning.add(new CertificateData(iari, CertificateData.format(certificate)));
	}

}
