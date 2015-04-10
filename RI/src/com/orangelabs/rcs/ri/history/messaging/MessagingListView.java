
package com.orangelabs.rcs.ri.history.messaging;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.history.HistoryListView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Messaging conversation log
 */
public class MessagingListView extends HistoryListView {

    private final static String[] PROJECTION_GROUP_CHAT = new String[] {
            ChatLog.GroupChat.CHAT_ID, ChatLog.GroupChat.SUBJECT
    };
    private final static String SORT_ORDER_GROUP_CHAT = new StringBuilder(
            ChatLog.GroupChat.TIMESTAMP).append(" DESC").toString();

    /**
     * WHERE mime_type!='rcs/groupchat-event' group by chat_id
     */
    private final static String WHERE_CLAUSE = new StringBuilder(HistoryLog.MIME_TYPE)
            .append("!='").append(ChatLog.Message.MimeType.GROUPCHAT_EVENT).append("' group by ")
            .append(HistoryLog.CHAT_ID).toString();

    /**
     * Associate the providers position in filter menu with providerIds defined in HistoryLog
     */
    @SuppressLint("UseSparseArrays")
    private final static Map<Integer, Integer> PROVIDERS_MAP = new HashMap<Integer, Integer>();
    static {
        PROVIDERS_MAP.put(0, ChatLog.Message.HISTORYLOG_MEMBER_ID);
        PROVIDERS_MAP.put(1, FileTransferLog.HISTORYLOG_MEMBER_ID);
    }

    /* mapping chat_id / group chat subject */
    private final Map<String, String> mGroupChatMap = new HashMap<String, String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        mCheckedProviders = new boolean[] {
                /* (Chat) */true,
                /* (File transfer) */true
        };

        mFilterMenuItems = new String[] {
                getString(R.string.label_history_log_menu_chat),
                getString(R.string.label_history_log_menu_file_transfer),
        };

        mResourceCursorAdapter = new MessagingLogAdapter(this);
        setContentView(R.layout.history_log_messaging);
        super.onCreate(savedInstanceState);
        fillGroupChatSubject();
        startQuery();
    }

    /**
     * Sharings log adapter
     */
    private class MessagingLogAdapter extends ResourceCursorAdapter {
        private Drawable mDrawableIncomingFailed;
        private Drawable mDrawableOutgoingFailed;
        private Drawable mDrawableIncoming;
        private Drawable mDrawableOutgoing;
        private Drawable mDrawableChat;
        private Drawable mDrawableFileTransfer;

        public MessagingLogAdapter(Context context) {
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
            mDrawableChat = context.getResources().getDrawable(R.drawable.ri_historylog_chat);
            mDrawableFileTransfer = context.getResources().getDrawable(
                    R.drawable.ri_historylog_filetransfer);

        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            int providerId = cursor.getInt(cursor.getColumnIndex(HistoryLog.PROVIDER_ID));

            TextView conversationTypeView = (TextView) view.findViewById(R.id.conversation_type);
            TextView conversationLabelView = (TextView) view.findViewById(R.id.conversation_label);
            TextView descriptionView = (TextView) view.findViewById(R.id.description);
            TextView dateView = (TextView) view.findViewById(R.id.date);

            ImageView eventDirectionIconView = (ImageView) view.findViewById(R.id.call_type_icon);
            ImageView eventIconView = (ImageView) view.findViewById(R.id.call_icon);

            // Set the date/time field by mixing relative and absolute times
            long date = cursor.getLong(cursor.getColumnIndex(HistoryLog.TIMESTAMP));
            dateView.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));

            // Set the status text and destination icon
            int status = cursor.getInt(cursor.getColumnIndex(HistoryLog.STATUS));
            switch (Direction.valueOf(cursor.getInt(cursor.getColumnIndex(HistoryLog.DIRECTION)))) {
                case INCOMING:
                    if (status == ChatLog.Message.Content.Status.FAILED.toInt()
                            || status == FileTransfer.State.FAILED.toInt()) {
                        eventDirectionIconView.setImageDrawable(mDrawableIncomingFailed);
                    } else {
                        eventDirectionIconView.setImageDrawable(mDrawableIncoming);
                    }
                    break;
                case OUTGOING:
                    if (status == ChatLog.Message.Content.Status.FAILED.toInt()
                            || status == FileTransfer.State.FAILED.toInt()) {
                        eventDirectionIconView.setImageDrawable(mDrawableOutgoingFailed);
                    } else {
                        eventDirectionIconView.setImageDrawable(mDrawableOutgoing);
                    }
                case IRRELEVANT:
                    break;
            }

            String contact = cursor.getString(cursor.getColumnIndex(HistoryLog.CONTACT));
            String chat_id = cursor.getString(cursor.getColumnIndex(HistoryLog.CHAT_ID));
            boolean isOnetoOneConversation = chat_id.equals(contact);
            if (isOnetoOneConversation) {
                conversationTypeView.setText(R.string.label_history_log_single_conversation);
                conversationLabelView.setText(contact);
            } else {
                conversationTypeView.setText(R.string.label_history_log_group_conversation);
                String subject = mGroupChatMap.get(chat_id);
                if (subject != null) {
                    conversationLabelView.setText(truncateString(subject, MAX_LENGTH_SUBJECT));
                }
            }

            if (ChatLog.Message.HISTORYLOG_MEMBER_ID == providerId) {
                eventIconView.setImageDrawable(mDrawableChat);
                descriptionView.setText(truncateString(
                        cursor.getString(cursor.getColumnIndex(HistoryLog.CONTENT)),
                        MAX_LENGTH_DESCRIPTION));
            } else if (FileTransferLog.HISTORYLOG_MEMBER_ID == providerId) {
                eventIconView.setImageDrawable(mDrawableFileTransfer);
                descriptionView.setText(truncateString(
                        cursor.getString(cursor.getColumnIndex(HistoryLog.FILENAME)),
                        MAX_LENGTH_DESCRIPTION));
            }
        }
    }

    @Override
    protected Map<Integer, Integer> getProvidersMap() {
        return PROVIDERS_MAP;
    }

    @Override
    protected void startQuery() {

        List<Integer> providers = new ArrayList<Integer>();
        for (int i = 0; i < mCheckedProviders.length; i++) {
            if (mCheckedProviders[i]) {
                providers.add(PROVIDERS_MAP.get(i));
            }
        }
        Cursor cursor = null;
        if (!providers.isEmpty()) {
            Uri uri = createHistoryUri(providers);
            cursor = getContentResolver().query(uri, null, WHERE_CLAUSE, null, SORT_BY);
        }
        mResourceCursorAdapter.changeCursor(cursor);
    }

    private void fillGroupChatSubject() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(ChatLog.GroupChat.CONTENT_URI,
                    PROJECTION_GROUP_CHAT, null, null, SORT_ORDER_GROUP_CHAT);
            if (cursor == null) {
                return;
            }
            int columnChatId = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.CHAT_ID);
            int columnSubject = cursor.getColumnIndexOrThrow(ChatLog.GroupChat.SUBJECT);
            while (cursor.moveToNext()) {
                mGroupChatMap.put(cursor.getString(columnChatId), cursor.getString(columnSubject));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
