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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Security info provider
 * 
 * @author jexa7410
 */
public class SecurityInfoProvider extends ContentProvider {
	// Database table
	public static final String TABLE = "authorizations";
		
	// Create the constants used to differentiate between the different
	// URI requests
	private static final int ROWS = 1;
	private static final int ROW_ID = 2;
		
	// Allocate the UriMatcher object, where a URI ending in 'security'
	// will correspond to a request for all security, and 'security'
	// with a trailing '/[rowID]' will represent a single security row.
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("com.orangelabs.rcs.security", "security", ROWS);
		uriMatcher.addURI("com.orangelabs.rcs.security", "security/#", ROW_ID);
	}
			
	/**
	 * Database helper class
	 */
	private SQLiteOpenHelper openHelper;	
	 
    /**
     * Database name
     */
    public static final String DATABASE_NAME = "security.db";

    /**
     * Helper class for opening, creating and managing database version control
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 2;

        public DatabaseHelper(Context ctx) {
            super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	db.execSQL("CREATE TABLE " + TABLE + " ("
        			+ SecurityInfoData.KEY_ID + " integer primary key autoincrement,"
        			+ SecurityInfoData.KEY_IARI + " TEXT,"
        			+ SecurityInfoData.KEY_CERT + " TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        openHelper = new DatabaseHelper(getContext());
        return true;
    }
	
	@Override
	public String getType(Uri uri) {
		switch(uriMatcher.match(uri)){
			case ROWS:
				return "vnd.android.cursor.dir/security";
			case ROW_ID:
				return "vnd.android.cursor.item/security";
			default:
				throw new IllegalArgumentException("Unsupported URI " + uri);
		}
	}
	
	@Override
    public Cursor query(Uri uri, String[] projectionIn, String selection, String[] selectionArgs, String sort) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE);

        // Generate the body of the query
        int match = uriMatcher.match(uri);
        switch(match) {
            case ROWS:
                break;
            case ROW_ID:
                qb.appendWhere(SecurityInfoData.KEY_ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor c = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
        return c;
	}
	
	@Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count = 0;
        SQLiteDatabase db = openHelper.getWritableDatabase();

        int match = uriMatcher.match(uri);
        switch (match) {
	        case ROWS:
	            count = db.update(TABLE, values, where, null);
	            break;
            case ROW_ID:
                String segment = uri.getPathSegments().get(1);
                int id = Integer.parseInt(segment);
                count = db.update(TABLE, values, SecurityInfoData.KEY_ID + "=" + id, null);
                break;
            default:
                throw new UnsupportedOperationException("Cannot update URI " + uri);
        }
        return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        switch(uriMatcher.match(uri)){
	        case ROWS:
	        case ROW_ID:
	    		long rowId = db.insert(TABLE, null, initialValues);
	    		uri = ContentUris.withAppendedId(SecurityInfoData.CONTENT_URI, rowId);
	        	break;
	        default:
	    		throw new SQLException("Failed to insert row into " + uri);
        }
        return uri;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        int count = 0;
        switch(uriMatcher.match(uri)){
	        case ROWS:
	        	count = db.delete(TABLE, where, whereArgs);
	        	break;
	        case ROW_ID:
	        	String segment = uri.getPathSegments().get(1);
				count = db.delete(TABLE, SecurityInfoData.KEY_ID + "="
						+ segment
						+ (!TextUtils.isEmpty(where) ? " AND ("	+ where + ')' : ""),
						whereArgs);
				
				break;
	        default:
	    		throw new SQLException("Failed to delete row " + uri);
        }
        return count;    
	}
}
