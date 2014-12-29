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
package com.orangelabs.rcs.provider.security;

import com.gsma.iariauth.validator.IARIAuthDocument.AuthType;

import android.net.Uri;

/**
 * A class to hold the IARI authorization data.<br>
 * It also defines data to access the authorization table from security provider.
 * 
 * @author yplo6403
 *
 */
public class AuthorizationData {
	/**
	 * Database URI
	 */
	public static final Uri CONTENT_URI = Uri.parse("content://com.orangelabs.rcs.security/authorization");
	
	/**
	 * Column name primary key
	 * <P>
	 * Type: INTEGER AUTO INCREMENTED
	 * </P>
	 */
	public static final String KEY_ID = "_id";

	/**
	 * The name of the column containing the IARI tag as the unique ID of certificate
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String KEY_IARI = "iari";

	/**
	 * The name of the column containing the package name.
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String KEY_PACK_NAME = "pack_name";
	
	/**
	 * The name of the column containing the authorization type.
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String KEY_AUTH_TYPE = "auth_type";
	
	/**
	 * The name of the column containing the package signer.
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String KEY_SIGNER = "signer";
	
	/**
	 * The name of the column containing the IARI range.
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String KEY_RANGE = "range";
	
	/**
	 * The name of the column containing the extension.
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String KEY_EXT = "ext";
	
	final private AuthType mAuthType;
	final private String mIari;
	final private String mRange;
	final private String mPackageName;
	final private String mPackageSigner;
	final private String mExtension;
	
	/**
	 * @param packageName
	 * @param extension
	 * @param iari
	 * @param authType
	 * @param range
	 * @param packageSigner
	 */
	public AuthorizationData(String packageName, String extension, String iari, AuthType authType, String range, String packageSigner) {
		mAuthType = authType;
		mIari = iari;
		mRange = range;
		mPackageName = packageName;
		mPackageSigner = packageSigner;
		mExtension = extension;
	}

	/**
	 * @param packageName
	 * @param extension
	 */
	public AuthorizationData(String packageName, String extension) {
		mAuthType = AuthType.UNSPECIFIED;
		mPackageName = packageName;
		mExtension = extension;
		mIari = null;
		mPackageSigner = null;
		mRange = null;
	}
	
	public AuthType getAuthType() {
		return mAuthType;
	}

	public String getIari() {
		return mIari;
	}

	public String getRange() {
		return mRange;
	}

	public String getPackageName() {
		return mPackageName;
	}

	public String getPackageSigner() {
		return mPackageSigner;
	}

	public String getExtension() {
		return mExtension;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mAuthType == null) ? 0 : mAuthType.hashCode());
		result = prime * result + ((mExtension == null) ? 0 : mExtension.hashCode());
		result = prime * result + ((mIari == null) ? 0 : mIari.hashCode());
		result = prime * result + ((mPackageName == null) ? 0 : mPackageName.hashCode());
		result = prime * result + ((mPackageSigner == null) ? 0 : mPackageSigner.hashCode());
		result = prime * result + ((mRange == null) ? 0 : mRange.hashCode());
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
		AuthorizationData other = (AuthorizationData) obj;
		if (mAuthType != other.mAuthType)
			return false;
		if (mExtension == null) {
			if (other.mExtension != null)
				return false;
		} else if (!mExtension.equals(other.mExtension))
			return false;
		if (mIari == null) {
			if (other.mIari != null)
				return false;
		} else if (!mIari.equals(other.mIari))
			return false;
		if (mPackageName == null) {
			if (other.mPackageName != null)
				return false;
		} else if (!mPackageName.equals(other.mPackageName))
			return false;
		if (mPackageSigner == null) {
			if (other.mPackageSigner != null)
				return false;
		} else if (!mPackageSigner.equals(other.mPackageSigner))
			return false;
		if (mRange == null) {
			if (other.mRange != null)
				return false;
		} else if (!mRange.equals(other.mRange))
			return false;
		return true;
	}
	
}