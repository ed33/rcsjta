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

import com.orangelabs.rcs.provider.security.SecurityInfos;

/**
 * A Class to update security provider only once provisioning is fully parsed
 * 
 * @author Oh. LEMORDANT
 *
 */
public class CertificateProvisioning implements ICertificateProvisioningListener {

	private SecurityInfos mSecurityInfos;

	private Map<IARICertificate, Integer> mCertiticatesBeforeProvisioning;

	private Set<IARICertificate> mCertificatesAfterProvisioning;

	public CertificateProvisioning(SecurityInfos securityInfos) {
		mSecurityInfos = securityInfos;
	}

	@Override
	public void start() {
		// Check if not already started
		if (mCertiticatesBeforeProvisioning != null) {
			return;

		}
		// Save certificates before provisioning
		mCertiticatesBeforeProvisioning = mSecurityInfos.getAll();
		// No certificates yet newly provisioned
		mCertificatesAfterProvisioning = new HashSet<IARICertificate>();
	}

	@Override
	public void stop() {
		// Check if not already stopped or never started
		if (mCertiticatesBeforeProvisioning == null) {
			return;

		}
		// Check for new Certificates
		for (IARICertificate iariCertificate : mCertificatesAfterProvisioning) {
			if (!mCertiticatesBeforeProvisioning.containsKey(iariCertificate)) {
				// new certificate: add to provider
				mSecurityInfos.addCertificateForIARI(iariCertificate);
			}
		}

		// Check for revoked certificates
		for (IARICertificate iariCertificate : mCertiticatesBeforeProvisioning.keySet()) {
			if (!mCertificatesAfterProvisioning.contains(iariCertificate)) {
				// revoked certificate: remove from provider
				mSecurityInfos.removeCertificate(mCertiticatesBeforeProvisioning.get(iariCertificate));
			}
		}
		// Only stop provisioning once
		mCertiticatesBeforeProvisioning = null;
	}


	@Override
	public void addNewCertificate(String iari, String certificate) {
		// Add IARI / Certificate in memory
		// Format certificate
		mCertificatesAfterProvisioning.add(new IARICertificate(iari, IARICertificate.format(certificate)));
	}

}
