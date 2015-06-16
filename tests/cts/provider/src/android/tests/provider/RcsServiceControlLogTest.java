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

package android.tests.provider;

import com.gsma.services.rcs.RcsServiceControlLog;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.InstrumentationTestCase;

public class RcsServiceControlLogTest extends InstrumentationTestCase {

    private final String[] RCS_SERVICE_CONTROL_LOG_PROJECTION = new String[] {
            RcsServiceControlLog.KEY, RcsServiceControlLog.VALUE
    };

    private ContentProviderClient mProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mProvider = getInstrumentation().getTargetContext().getContentResolver()
                .acquireContentProviderClient(RcsServiceControlLog.CONTENT_URI);
        assertNotNull(mProvider);
    }

    /**
     * Test the RcsServiceControlLog provider according to GSMA API specifications.<br>
     * Check the following operations:
     * <ul>
     * <li>query
     * <li>insert
     * <li>delete
     * <li>update
     */
    public void testRcsServiceControlLogQuery() {
        /* Check that provider handles columns names and query operation */
        Cursor cursor = null;
        try {
            String where = RcsServiceControlLog.KEY.concat("=?");
            String[] whereArgs = new String[] {
                RcsServiceControlLog.ENABLE_RCS_SWITCH
            };
            cursor = mProvider.query(RcsServiceControlLog.CONTENT_URI,
                    RCS_SERVICE_CONTROL_LOG_PROJECTION, where, whereArgs, null);
            assertNotNull(cursor);
        } catch (Exception e) {
            fail("query of RcsServiceControlLog failed " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testRcsServiceControlLogQueryByKey() {
        Cursor cursor = null;
        try {
            String where = RcsServiceControlLog.KEY.concat("=?");
            String[] whereArgs = new String[] {
                RcsServiceControlLog.ENABLE_RCS_SWITCH
            };
            cursor = mProvider.query(RcsServiceControlLog.CONTENT_URI,
                    RCS_SERVICE_CONTROL_LOG_PROJECTION, where, whereArgs, null);
            assertTrue(cursor.moveToFirst());
            String key = cursor.getString(cursor.getColumnIndexOrThrow(RcsServiceControlLog.KEY));
            assertTrue(key.equals(RcsServiceControlLog.ENABLE_RCS_SWITCH));
        } catch (Exception e) {
            fail("query of RcsServiceControlLog failed " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testRcsServiceControlLogQueryByKeyBis() {
        Cursor cursor = null;
        try {
            String where = new StringBuilder(RcsServiceControlLog.KEY).append("= '")
                    .append(RcsServiceControlLog.ENABLE_RCS_SWITCH).append("'").toString();
            cursor = mProvider.query(RcsServiceControlLog.CONTENT_URI, null, where, null, null);
            assertTrue(cursor.getCount() == 1);
            assertTrue(cursor.moveToFirst());
            String key = cursor.getString(cursor.getColumnIndexOrThrow(RcsServiceControlLog.KEY));
            assertTrue(key.equals(RcsServiceControlLog.ENABLE_RCS_SWITCH));
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testRcsServiceControlLogQueryWithoutWhereClause() {
        Cursor cursor = null;
        try {
            Uri uri = Uri.withAppendedPath(RcsServiceControlLog.CONTENT_URI,
                    RcsServiceControlLog.ENABLE_RCS_SWITCH);
            cursor = mProvider.query(uri, null, null, null, null);
            assertTrue(cursor.getCount() == 1);
            assertTrue(cursor.moveToFirst());
            Utils.checkProjection(RCS_SERVICE_CONTROL_LOG_PROJECTION, cursor.getColumnNames());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void testRcsServiceControlLogInsert() {
        /* Check that provider does not support insert operation */
        ContentValues values = new ContentValues();
        values.put(RcsServiceControlLog.KEY, RcsServiceControlLog.ENABLE_RCS_SWITCH);
        values.put(RcsServiceControlLog.VALUE, RcsServiceControlLog.EnableRcseSwitch.ALWAYS_SHOW.toInt());
        try {
            mProvider.insert(RcsServiceControlLog.CONTENT_URI, values);
            fail("RcsServiceControlLog is read only");
        } catch (Exception ex) {
            assertTrue("insert into RcsServiceControlLog should be forbidden",
                    ex instanceof RuntimeException);
        }

    }

    public void testRcsServiceControlLogDelete() {
        /* Check that provider supports delete operation */
        try {
            mProvider.delete(RcsServiceControlLog.CONTENT_URI, null, null);
            fail("RcsServiceControlLog is read only");
        } catch (Exception e) {
            assertTrue("delete of RcsServiceControlLog should be forbidden",
                    e instanceof RuntimeException);
        }
    }

    public void testRcsServiceControlLogUpdate() {
        /* Check that provider does not support update operation */
        ContentValues values = new ContentValues();
        values.put(RcsServiceControlLog.KEY, RcsServiceControlLog.ENABLE_RCS_SWITCH);
        values.put(RcsServiceControlLog.VALUE, RcsServiceControlLog.EnableRcseSwitch.ALWAYS_SHOW.toInt());
        try {
            mProvider.update(RcsServiceControlLog.CONTENT_URI, values, null, null);
            fail("RcsServiceControlLog is read only");
        } catch (Exception ex) {
            assertTrue("update of RcsServiceControlLog should be forbidden",
                    ex instanceof RuntimeException);
        }
    }

}
