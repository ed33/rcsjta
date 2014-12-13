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

/**
 * A class to hold IARI and associated certificate.
 * 
 * @author LEMORDANT Philippe
 *
 */
public class IARICertificate {

	private String mIARI;

	private String mCertificate;

	public IARICertificate(String iari, String certificate) {
		mIARI = iari;
		mCertificate = certificate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mCertificate == null) ? 0 : mCertificate.hashCode());
		result = prime * result + ((mIARI == null) ? 0 : mIARI.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IARICertificate other = (IARICertificate) obj;
		if (mCertificate == null) {
			if (other.mCertificate != null)
				return false;
		} else if (!mCertificate.equals(other.mCertificate))
			return false;
		if (mIARI == null) {
			if (other.mIARI != null)
				return false;
		} else if (!mIARI.equals(other.mIARI))
			return false;
		return true;
	}

	public String getIARI() {
		return mIARI;
	}

	public String getCertificate() {
		return mCertificate;
	}

}
