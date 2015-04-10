
package com.orangelabs.rcs.ri.history.sharing;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactUtil;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingLog;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharingLog;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.history.HistoryListView;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sharing log
 */
public class SharingListView extends HistoryListView {

    /**
     * Selected contact number from spinner.
     */
    private String mCurrentContactNumber;

    /**
     * Selected contact label from spinner. i.e.: Home, Mobile...
     */
    private String mCurrentLabel;

    private final static String WHERE_CLAUSE_WITH_CONTACT = new StringBuilder(HistoryLog.CONTACT)
            .append("=?").toString();

    /**
     * Associate the providers position in filter menu with providerIds defined in HistoryLog
     */
    @SuppressLint("UseSparseArrays")
    private final static Map<Integer, Integer> PROVIDERS_MAP = new HashMap<Integer, Integer>();
    static {
        PROVIDERS_MAP.put(0, ImageSharingLog.HISTORYLOG_MEMBER_ID);
        PROVIDERS_MAP.put(1, VideoSharingLog.HISTORYLOG_MEMBER_ID);
        PROVIDERS_MAP.put(2, GeolocSharingLog.HISTORYLOG_MEMBER_ID);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        mCheckedProviders = new boolean[] {
                /* (Image sharing) */true,
                /* (Video sharing) */true
                /* (Geoloc sharing) */, true
        };

        mFilterMenuItems = new String[] {
                getString(R.string.label_history_log_menu_image_sharing),
                getString(R.string.label_history_log_menu_video_sharing),
                getString(R.string.label_history_log_menu_geoloc_sharing)
        };

        mResourceCursorAdapter = new SharingLogAdapter(this);
        
        setContentView(R.layout.history_log_sharing);        
        super.onCreate(savedInstanceState);
        
        Spinner spinner = (Spinner) findViewById(R.id.contact);
        spinner.setAdapter(ContactListAdapter.createContactListAdapter(this,
                getString(R.string.label_history_log_contact_spinner_default_value)));
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                /* Call when an item is selected so also at the start of the activity to initialize */
                setSelectedContact();
                setSelectedLabel();
                startQuery();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    @Override
    protected OnItemClickListener getOnItemClickListener() {
        return new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {

                // Get selected item
                Cursor cursor = (Cursor) mResourceCursorAdapter.getItem(pos);
                SharingDetailView.startActivity(
                        SharingListView.this,
                        new SharingInfos(
                                cursor.getString(cursor.getColumnIndex(HistoryLog.CONTACT)), cursor
                                        .getString(cursor.getColumnIndex(HistoryLog.FILENAME)),
                                cursor.getString(cursor.getColumnIndex(HistoryLog.FILESIZE)),
                                cursor.getString(cursor.getColumnIndex(HistoryLog.STATUS)), cursor
                                        .getString(cursor.getColumnIndex(HistoryLog.DIRECTION)),
                                DateFormat.getInstance().format(
                                        new Date(cursor.getLong(cursor
                                                .getColumnIndex(HistoryLog.TIMESTAMP)))), cursor
                                        .getString(cursor.getColumnIndex(HistoryLog.DURATION))));
            }
        };
    }

    private void setSelectedContact() {
        Spinner spinner = (Spinner) findViewById(R.id.contact);
        MatrixCursor cursor = (MatrixCursor) spinner.getSelectedItem();
        mCurrentContactNumber = cursor.getString(1);
        if (!getString(R.string.label_history_log_contact_spinner_default_value).equals(
                mCurrentContactNumber)) {
            mCurrentContactNumber = ContactUtil.getInstance(this)
                    .formatContact(mCurrentContactNumber).toString();
        }
    }

    private void setSelectedLabel() {
        Spinner spinner = (Spinner) findViewById(R.id.contact);
        MatrixCursor cursor = (MatrixCursor) spinner.getSelectedItem();
        String label = cursor.getString(2);
        if (label == null) {
            // Label is not custom, get the string corresponding to the phone type
            int type = cursor.getInt(3);
            label = getString(Phone.getTypeLabelResource(type));
        }
        mCurrentLabel = label;
    }

    protected void startQuery() {

        List<Integer> providers = new ArrayList<Integer>();
        for (int i = 0; i < mCheckedProviders.length; i++) {
            if (mCheckedProviders[i]) {
                providers.add(PROVIDERS_MAP.get(i));
            }
        }
        Cursor cursor = null;
        if (!providers.isEmpty()) {
            if (getString(R.string.label_history_log_contact_spinner_default_value).equals(
                    getCurrentContactNumber())) { /* No contact is selected */
                Uri uri = createHistoryUri(providers);
                cursor = getContentResolver().query(uri, null, null, null, SORT_BY);
            } else {
                Uri uri = createHistoryUri(providers);
                cursor = getContentResolver().query(uri, null, WHERE_CLAUSE_WITH_CONTACT,
                        new String[] {
                            getCurrentContactNumber()
                        }, SORT_BY);
            }
        }
        mResourceCursorAdapter.changeCursor(cursor);
    }

    /**
     * @return current label
     */
    public String getCurrentLabel() {
        return mCurrentLabel;
    }

    /**
     * @return current contact number
     */
    public String getCurrentContactNumber() {
        return mCurrentContactNumber;
    }

    /**
     * Sharings log adapter
     */
    private class SharingLogAdapter extends ResourceCursorAdapter {
        private Drawable mDrawableIncomingFailed;
        private Drawable mDrawableOutgoingFailed;
        private Drawable mDrawableIncoming;
        private Drawable mDrawableOutgoing;
        private Drawable mDrawableRichCall;

        public SharingLogAdapter(Context context) {
            super(context, R.layout.history_log_list, null);

            // Load the drawables
            mDrawableIncomingFailed = context.getResources().getDrawable(
                    R.drawable.ri_historylog_list_incoming_call_failed);
            mDrawableOutgoingFailed = context.getResources().getDrawable(
                    R.drawable.ri_historylog_list_outgoing_call_failed);
            mDrawableIncoming = context.getResources().getDrawable(
                    R.drawable.ri_historylog_list_incoming_call);
            mDrawableOutgoing = context.getResources().getDrawable(
                    R.drawable.ri_historylog_list_outgoing_call);
            mDrawableRichCall = context.getResources().getDrawable(R.drawable.ri_historylog_csh);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            int providerId = cursor.getInt(cursor.getColumnIndex(HistoryLog.PROVIDER_ID));

            TextView sharingTypeView = (TextView) view.findViewById(R.id.conversation_type);
            TextView sharingLabelView = (TextView) view.findViewById(R.id.conversation_label);
            TextView descriptionView = (TextView) view.findViewById(R.id.description);
            TextView dateView = (TextView) view.findViewById(R.id.date);

            ImageView eventDirectionIconView = (ImageView) view.findViewById(R.id.call_type_icon);
            ImageView eventIconView = (ImageView) view.findViewById(R.id.call_icon);

            // Set contact number
            sharingLabelView.setText(cursor.getString(cursor.getColumnIndex(HistoryLog.CONTACT)));

            // Set the date/time field by mixing relative and absolute times
            long date = cursor.getLong(cursor.getColumnIndex(HistoryLog.TIMESTAMP));
            dateView.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));

            // Set the status text and destination icon
            int status = cursor.getInt(cursor.getColumnIndex(HistoryLog.STATUS));
            switch (Direction.valueOf(cursor.getInt(cursor.getColumnIndex(HistoryLog.DIRECTION)))) {
                case INCOMING:
                    if (status == ImageSharing.State.FAILED.toInt()
                            || status == VideoSharing.State.FAILED.toInt()
                            || status == GeolocSharing.State.FAILED.toInt()) {
                        eventDirectionIconView.setImageDrawable(mDrawableIncomingFailed);
                    } else {
                        eventDirectionIconView.setImageDrawable(mDrawableIncoming);
                    }
                    break;
                case OUTGOING:
                    if (status == ImageSharing.State.FAILED.toInt()
                            || status == VideoSharing.State.FAILED.toInt()
                            || status == GeolocSharing.State.FAILED.toInt()) {
                        eventDirectionIconView.setImageDrawable(mDrawableOutgoingFailed);
                    } else {
                        eventDirectionIconView.setImageDrawable(mDrawableOutgoing);
                    }
                case IRRELEVANT:
                    break;
            }

            eventIconView.setImageDrawable(mDrawableRichCall);
            switch (providerId) {
                case ImageSharingLog.HISTORYLOG_MEMBER_ID:
                    sharingTypeView.setText(R.string.label_history_log_image_sharing);
                    String filename = cursor.getString(cursor.getColumnIndex(HistoryLog.FILENAME));                      
                    descriptionView.setText(truncateString(filename, MAX_LENGTH_DESCRIPTION));                    
                    break;
                case VideoSharingLog.HISTORYLOG_MEMBER_ID:
                    sharingTypeView.setText(R.string.label_history_log_video_sharing);
                    int duration = cursor.getInt(cursor.getColumnIndex(HistoryLog.DURATION)); 
                    descriptionView.setText(new StringBuilder("Duration : ").append(duration/1000).append("s").toString());
                    break;
                case GeolocSharingLog.HISTORYLOG_MEMBER_ID:
                    sharingTypeView.setText(R.string.label_history_log_geoloc_sharing);
                    break;
            }
        }
    }

    @Override
    protected Map<Integer, Integer> getProvidersMap() {
        return PROVIDERS_MAP;
    }

}
