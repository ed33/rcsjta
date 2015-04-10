
package com.orangelabs.rcs.ri.history;

import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import com.orangelabs.rcs.ri.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * History log
 */
public abstract class HistoryListView extends Activity {

    protected final static int MAX_LENGTH_DESCRIPTION = 25;
    protected final static int MAX_LENGTH_SUBJECT = 15;
    protected final static String SORT_BY = new StringBuilder(HistoryLog.TIMESTAMP).append(" DESC")
            .toString();

    /********************************************************************
     * Filtering
     ********************************************************************/
    /**
     * AlertDialog to show for selecting filters.
     */
    protected AlertDialog mFilterAlertDialog;

    protected CharSequence[] mFilterMenuItems;
    protected boolean[] mCheckedProviders;
    protected ResourceCursorAdapter mResourceCursorAdapter;

    protected abstract Map<Integer, Integer> getProvidersMap();

    protected abstract void startQuery();

    protected OnItemClickListener getOnItemClickListener() {
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set cursor adpator
        ListView view = (ListView) findViewById(android.R.id.list);

        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        view.setEmptyView(emptyView);
        view.setAdapter(mResourceCursorAdapter);
        if (getOnItemClickListener() != null) {
            view.setOnItemClickListener(getOnItemClickListener());
        }
    }

    protected void startQuery(String[] projection, String selection, String[] selectionArgs) {
        List<Integer> providers = new ArrayList<Integer>();
        Map<Integer, Integer> providersMap = getProvidersMap();
        for (int i = 0; i < mCheckedProviders.length; i++) {
            if (mCheckedProviders[i]) {
                providers.add(providersMap.get(i));
            }
        }
        Cursor cursor = null;
        if (!providers.isEmpty()) {
            Uri uri = createHistoryUri(providers);
            cursor = getContentResolver().query(uri, projection, selection, selectionArgs, SORT_BY);
        }
        mResourceCursorAdapter.changeCursor(cursor);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBooleanArray("selectedProviders", mCheckedProviders);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        mCheckedProviders = state.getBooleanArray("selectedProviders");
        super.onRestoreInstanceState(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_historylog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_filter:
                AlertDialog.Builder builder = new AlertDialog.Builder(HistoryListView.this);
                builder.setTitle(R.string.title_history_log_dialog_filter_logs_title);
                builder.setMultiChoiceItems(mFilterMenuItems, mCheckedProviders,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                mCheckedProviders[which] = isChecked;
                            }
                        });
                builder.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mFilterAlertDialog.dismiss();
                        startQuery();
                    }
                });
                builder.setNegativeButton(R.string.label_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mFilterAlertDialog.dismiss();
                            }
                        });
                mFilterAlertDialog = builder.show();
                break;
        }
        return true;
    }

    /**
     * @param providerIds
     * @return Uri
     */
    public static Uri createHistoryUri(List<Integer> providerIds) {
        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);

        for (int providerId : providerIds) {
            uriBuilder.appendProvider(providerId);
        }

        return uriBuilder.build();
    }

    protected String truncateString(String in, int maxLength) {
        if (in.length() > maxLength) {
            in = in.substring(0, maxLength).concat("...");
        }
        return new StringBuilder("\"").append(in).append("\"").toString();
    }
}
