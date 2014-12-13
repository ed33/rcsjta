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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import com.orangelabs.rcs.utils.DatabaseUtils;

/**
 * Security info provider
 * 
 * @author jexa7410
 * @author yplo6403
 *
 */
public class SecurityInfoProvider extends ContentProvider {
	// Database table
	private static final String TABLE = "certificates";

	private static final String SELECTION_WITH_ID_ONLY = SecurityInfoData.KEY_ID.concat("=?");

	public static final String DATABASE_NAME = "security.db";

	private static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(SecurityInfoData.CONTENT_URI.getAuthority(), SecurityInfoData.CONTENT_URI.getPath().substring(1),
				UriType.IARI);
		sUriMatcher.addURI(SecurityInfoData.CONTENT_URI.getAuthority(),
				SecurityInfoData.CONTENT_URI.getPath().substring(1).concat("/*"), UriType.IARI_WITH_ID);
	}

	// Class used to differentiate between the different URI requests
	private static final class UriType {

		private static final int IARI = 1;

		private static final int IARI_WITH_ID = 2;
	}

	private static final class CursorType {

		private static final String TYPE_DIRECTORY = "vnd.android.cursor.dir/certificate";

		private static final String TYPE_ITEM = "vnd.android.cursor.item/certificate";
	}

	/**
	 * Helper class for opening, creating and managing database version control
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final int DATABASE_VERSION = 3;

		public DatabaseHelper(Context ctx) {
			super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// @formatter:off
			db.execSQL(new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(TABLE).append("(")
					.append(SecurityInfoData.KEY_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT,")
					.append(SecurityInfoData.KEY_IARI).append(" TEXT NOT NULL,")
                    .append(SecurityInfoData.KEY_CERT).append(" TEXT NOT NULL,")
                    .append("UNIQUE(").append(SecurityInfoData.KEY_IARI).append(",").append(SecurityInfoData.KEY_CERT).append("))").toString());
			 db.execSQL(new StringBuilder("CREATE INDEX ").append(SecurityInfoData.KEY_IARI)
	                    .append("_idx").append(" ON ").append(TABLE).append("(")
	                    .append(SecurityInfoData.KEY_IARI).append(")").toString());
			 db.execSQL(new StringBuilder("CREATE INDEX ").append(SecurityInfoData.KEY_IARI)
	                    .append("_").append(SecurityInfoData.KEY_CERT).append("_idx").append(" ON ").append(TABLE).append("(")
	                    .append(SecurityInfoData.KEY_IARI).append(",")
	                    .append(SecurityInfoData.KEY_CERT).append(")").toString());
			// @formatter:on
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
			db.execSQL("DROP TABLE IF EXISTS ".concat(TABLE));
			onCreate(db);
		}
	}

	private SQLiteOpenHelper mOpenHelper;

	private String getSelectionWithId(String selection) {
		if (TextUtils.isEmpty(selection)) {
			return SELECTION_WITH_ID_ONLY;
			
		}
		return new StringBuilder("(").append(SELECTION_WITH_ID_ONLY).append(") AND (").append(selection).append(")").toString();
	}

	private String[] getSelectionArgsWithId(String[] selectionArgs, String id) {
		String[] sharingSelectionArg = new String[] { id };
		if (selectionArgs == null) {
			return sharingSelectionArg;
			
		}
		return DatabaseUtils.appendSelectionArgs(sharingSelectionArg, selectionArgs);
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case UriType.IARI:
			return CursorType.TYPE_DIRECTORY;
			
		case UriType.IARI_WITH_ID:
			return CursorType.TYPE_ITEM;
			
		default:
			throw new IllegalArgumentException(new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
		Cursor cursor = null;
		try {
			switch (sUriMatcher.match(uri)) {
			case UriType.IARI_WITH_ID:
				String iariId = uri.getLastPathSegment();
				selection = getSelectionWithId(selection);
				selectionArgs = getSelectionArgsWithId(selectionArgs, iariId);
				/* Intentional fall through */
			case UriType.IARI:
				SQLiteDatabase db = mOpenHelper.getReadableDatabase();
				cursor = db.query(TABLE, projection, selection, selectionArgs, null, null, sort);
				cursor.setNotificationUri(getContext().getContentResolver(), uri);
				return cursor;

			default:
				throw new IllegalArgumentException(new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
				
			}
		} catch (RuntimeException e) {
			if (cursor != null) {
				cursor.close();
			}
			throw e;
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		switch (sUriMatcher.match(uri)) {
		case UriType.IARI_WITH_ID:
			String Id = uri.getLastPathSegment();
			selection = getSelectionWithId(selection);
			selectionArgs = getSelectionArgsWithId(selectionArgs, Id);
			/* Intentional fall through */
		case UriType.IARI:
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			int count = db.update(TABLE, values, selection, selectionArgs);
			if (count > 0) {
				getContext().getContentResolver().notifyChange(uri, null);
			}
			return count;

		default:
			throw new IllegalArgumentException(new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		switch (sUriMatcher.match(uri)) {
		case UriType.IARI:
			/* Intentional fall through */
		case UriType.IARI_WITH_ID:
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			String iariId = initialValues.getAsString(SecurityInfoData.KEY_IARI);
			db.insert(TABLE, null, initialValues);
			Uri notificationUri = Uri.withAppendedPath(SecurityInfoData.CONTENT_URI, iariId);
			getContext().getContentResolver().notifyChange(notificationUri, null);
			return notificationUri;

		default:
			throw new IllegalArgumentException(new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		switch (sUriMatcher.match(uri)) {
		case UriType.IARI_WITH_ID:
			String id = uri.getLastPathSegment();
			selection = getSelectionWithId(selection);
			selectionArgs = getSelectionArgsWithId(selectionArgs, id);
			/* Intentional fall through */
		case UriType.IARI:
			SQLiteDatabase db = mOpenHelper.getWritableDatabase();
			int count = db.delete(TABLE, selection, selectionArgs);
			if (count > 0) {
				getContext().getContentResolver().notifyChange(uri, null);
			}
			return count;

		default:
			throw new IllegalArgumentException(new StringBuilder("Unsupported URI ").append(uri).append("!").toString());
		}
	}
}
