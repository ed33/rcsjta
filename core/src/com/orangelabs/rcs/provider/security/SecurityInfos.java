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

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Security infos
 * 
 * @author jexa7410
 */
public class SecurityInfos {
	/**
	 * Current instance
	 */
	private static SecurityInfos instance = null;

	/**
	 * Content resolver
	 */
	private ContentResolver cr;
	
	/**
	 * Database URI
	 */
	private Uri databaseUri = SecurityInfoData.CONTENT_URI;

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	/**
	 * Create instance
	 * 
	 * @param ctx Context
	 */
	public static synchronized void createInstance(Context ctx) {
		if (instance == null) {
			instance = new SecurityInfos(ctx);
		}
	}
	
	/**
	 * Returns instance
	 * 
	 * @return Instance
	 */
	public static SecurityInfos getInstance() {
		return instance;
	}
	
	/**
     * Constructor
     * 
     * @param ctx Application context
     */
	private SecurityInfos(Context ctx) {
		super();
		
        this.cr = ctx.getContentResolver();
    }
	
	/**
	 * Add a IARI authorization
	 * 
	 * @param iari IARI
	 * @param cert Certificate
	 */
	public void addIARI(String iari, String cert) {
		if (logger.isActivated()) {
			logger.debug("Add IARI " + iari);
		}

		ContentValues values = new ContentValues();
		values.put(SecurityInfoData.KEY_IARI, iari);
		values.put(SecurityInfoData.KEY_CERT, cert);
		cr.insert(databaseUri, values);
	}
	
	/**
	 * Remove all IARI authorizations
	 */
	public void removeAllIARI() {
		if (logger.isActivated()) {
			logger.debug("Remove all IARI");
		}

		cr.delete(databaseUri, null, null);
	}	

	/**
	 * Remove a IARI authorization
	 * 
	 * @param iari IARI
	 */
	public void removeIARI(String iari) {
		if (logger.isActivated()) {
			logger.debug("Remove IARI " + iari);
		}

		cr.delete(databaseUri, SecurityInfoData.KEY_IARI + "='" + iari + "'", null);
	}

	/**
	 * Returns a list of certificates associated to a IARI
	 * 
	 * @param iari IARI
	 * @return List of certificates
	 */
	public List<String> getIARICert(String iari) {
		if (logger.isActivated()) {
			logger.debug("Get certificates for IARI " + iari);
		}

		List<String> result = new ArrayList<String>();
		Cursor c = cr.query(databaseUri, null, SecurityInfoData.KEY_IARI + "='" + iari + "'", null, null);
		while(c.moveToNext()) {
			String cert = c.getString(c.getColumnIndexOrThrow(SecurityInfoData.KEY_CERT));
			result.add(cert);
		}
		c.close();
		
		return result;
	}
}
