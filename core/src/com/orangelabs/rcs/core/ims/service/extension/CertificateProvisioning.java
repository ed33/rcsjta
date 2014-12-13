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
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * A Class to update security provider and trust store only once provisioning is fully parsed
 * 
 * @author LEMORDANT Philippe
 *
 */
public class CertificateProvisioning implements ICertificateProvisioning {

	private SecurityInfos mSecurityInfos;

	private Map<IARICertificate, Integer> mProviderData;

	private Set<IARICertificate> mMemoryData;

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(CertificateProvisioning.class.getSimpleName());

	public CertificateProvisioning() {
		// TODO Check if SecurityInfos instance is created
		mSecurityInfos = SecurityInfos.getInstance();
	}

	@Override
	public void start() {
		// Check if not already started
		if (mProviderData != null) {
			return;

		}
		// Save all provider content in memory
		mProviderData = mSecurityInfos.getAll();
		mMemoryData = new HashSet<IARICertificate>();
	}

	@Override
	public void stop() {
		// Check if not already stopped or never started
		if (mProviderData == null) {
			return;

		}

		// Check for new Certificates
		for (IARICertificate iariCertificate : mMemoryData) {
			if (!mProviderData.containsKey(iariCertificate)) {
				// new certificate: add to provider
				mSecurityInfos.addCertificateForIARI(iariCertificate);
				int id = mSecurityInfos.getIdForIariAndCertificate(iariCertificate);
				if (id != SecurityInfos.INVALID_ID) {
					// TODO add certificate to key store using ID as alias
					// ks.add( id, iariCertificate.getCertificate());
				} else {
					if (logger.isActivated()) {
						logger.warn("Invalid ID for IARI ".concat(iariCertificate.getIARI()));
					}
				}
			}
		}
		// Check for revoked certificates
		for (IARICertificate iariCertificate : mProviderData.keySet()) {
			if (!mMemoryData.contains(iariCertificate)) {
				// revoked certificate: remove from provider
				mSecurityInfos.removeCertificate(mProviderData.get(iariCertificate));
				int id = mProviderData.get(iariCertificate);
				// TODO remove from key store certificate with alias
				// ks.remove( id, iariCertificate.getCertificate());
			}
		}
		// Only stop provisioning once
		mProviderData = null;
	}

	@Override
	public void addNewCertificate(String iari, String certificate) {
		// Add IARI / Certificate in memory
		mMemoryData.add(new IARICertificate(iari, certificate));
	}

}
