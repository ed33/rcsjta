package com.orangelabs.rcs.provider.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.orangelabs.rcs.utils.logger.Logger;

import android.annotation.SuppressLint;

/**
 * Cache implementation for authorization data
 *
 */
@SuppressLint("UseSparseArrays")
public class CacheAuth {

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(SecurityLog.class.getSimpleName());
	private Map<String, AuthorizationData> iariMap;
	private Map<Integer, Set<AuthorizationData>> uidMap;

	/**
	 * Default Constructor
	 */
	public CacheAuth() {
		iariMap = new HashMap<String, AuthorizationData>();
		uidMap = new HashMap<Integer, Set<AuthorizationData>>();
	}

	/**
	 * Add an authorization in the cache
	 * 
	 * @param authorization
	 */
	public void add(AuthorizationData authorization) {
		if (logger.isActivated()) {
			logger.debug(new StringBuilder("Add authorziation in cache for uid / iari : ").append(authorization.getPackageUid())
					.append(",").append(authorization.getIARI()).toString());
		}
		iariMap.put(authorization.getIARI(), authorization);
		Integer uid = authorization.getPackageUid();
		Set<AuthorizationData> authorizationDatas = uidMap.get(uid);
		if (authorizationDatas == null) {
			authorizationDatas = new HashSet<AuthorizationData>();
			uidMap.put(uid, authorizationDatas);
		}
		authorizationDatas.add(authorization);
	}

	/**
	 * Get an authorization by IARI
	 * 
	 * @param iari
	 * @return AuthorizationData
	 */
	public AuthorizationData get(String iari) {
		if (logger.isActivated() && iariMap.get(iari) != null) {
			logger.debug("Retrieve authorization from cache for iari : ".concat(iari));
		}
		return iariMap.get(iari);
	}

	/**
	 * Get an authorization by uid and iari
	 * 
	 * @param uid
	 * @param iari
	 * @return AuthorizationData
	 */
	public AuthorizationData get(Integer uid, String iari) {
		AuthorizationData auth = iariMap.get(iari);
		if (auth == null) {
			return null;
		}
		if (uidMap.get(uid).contains(auth)) {
			if (logger.isActivated()) {
				logger.debug(new StringBuilder("Retrieve authorziation from cache for uid / iari : ").append(uid).append(",")
						.append(iari).toString());
			}
			return auth;
		}
		return null;
	}

	/**
	 * Remove an authorization by iari
	 * 
	 * @param iari
	 */
	public void remove(String iari) {
		if (logger.isActivated()) {
			logger.debug("Remove authorization in cache for iari : ".concat(iari));
		}
		AuthorizationData auth = iariMap.get(iari);
		if (auth == null) {
			return;
		}
		iariMap.remove(iari);
		uidMap.get(auth.getPackageUid()).remove(auth);
	}
};
