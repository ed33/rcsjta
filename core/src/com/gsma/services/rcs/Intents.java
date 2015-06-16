/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.services.rcs;

/**
 * Intents related to rcs service activities
 * 
 * @author Jean-Marc AUFFRET
 */
public class Intents {
    /**
     * Intents for RCS service
     */
    public static class Service {

        /**
         * Intent to set the activation mode of the RCS stack
         */
        public static final String ACTION_SET_ACTIVATION_MODE = "com.gsma.services.rcs.action.SET_ACTIVATION_MODE";

        /**
         * A boolean extra field in ACTION_SET_ACTIVATION_MODE intent to set the activation mode.
         */
        public static final String EXTRA_SET_ACTIVATION_MODE = "set_activation_mode";

        /**
         * Intent to query if RCS stack is compatible with RCS API
         */
        public static final String ACTION_QUERY_COMPATIBILITY = "com.gsma.services.rcs.action.QUERY_COMPATIBILITY";

        /**
         * A string extra field in ACTION_QUERY_COMPATIBILITY intent to convey the codename
         */
        public static final String EXTRA_QUERY_COMPATIBILITY_CODENAME = "query_compatibility_codename";

        /**
         * Used as an integer extra field in ACTION_QUERY_COMPATIBILITY intent to convey the version
         */
        public static final String EXTRA_QUERY_COMPATIBILITY_VERSION = "query_compatibility_version";

        /**
         * An integer extra field in ACTION_QUERY_COMPATIBILITY intent to convey the increment
         */
        public static final String EXTRA_QUERY_COMPATIBILITY_INCREMENT = "query_compatibility_increment";

        /**
         * A string extra field in ACTION_QUERY_SERVICE_STARTING_STATE intent to convey the package
         * name.
         */
        public static final String EXTRA_QUERY_COMPATIBILITY_PACKAGENAME = "query_compatibility_packagename";

        /**
         * Intent to respond to query if RCS stack is compatible with RCS API
         */
        public static final String ACTION_RESP_COMPATIBILITY = "com.gsma.services.rcs.action.RESP_COMPATIBILITY";

        /**
         * A string extra field in ACTION_RESP_COMPATIBILITY intent to convey the response
         */
        public static final String EXTRA_RESP_COMPATIBILITY = "resp_compatibility";

        /**
         * Intent to query the RCS service starting state.
         */
        public static final String ACTION_QUERY_SERVICE_STARTING_STATE = "com.gsma.services.rcs.action.QUERY_SERVICE_STARTING_STATE";

        /**
         * A string extra field in ACTION_QUERY_SERVICE_STARTING_STATE intent to convey the package
         * name.
         */
        public static final String EXTRA_QUERY_SERVICE_STARTING_STATE_PACKAGENAME = "query_service_starting_state_packagename";

        /**
         * Intent to respond to the RCS service starting state query.
         */
        public static final String ACTION_RESP_SERVICE_STARTING_STATE = "com.gsma.services.rcs.action.RESP_SERVICE_STARTING_STATE";

        /**
         * A boolean extra field in ACTION_RESP_SERVICE_STARTING_STATE intent to convey the starting
         * state.
         */
        public static final String EXTRA_RESP_SERVICE_STARTING_STATE = "resp_service_starting_state";

        private Service() {
        }
    }

    /**
     * Intents for chat service
     */
    public static class Chat {
        /**
         * Load the chat application to view a chat conversation. This Intent takes into parameter
         * an URI on the chat conversation (i.e. content://chats/chat_ID). If no parameter found the
         * main entry of the chat application is displayed.
         */
        public static final String ACTION_VIEW_ONE_TO_ONE_CHAT = "com.gsma.services.rcs.action.VIEW_ONE_TO_ONE_CHAT";

        /**
         * Load the chat application to send a new chat message to a given contact. This Intent
         * takes into parameter a contact URI (i.e. content://contacts/people/contact_ID). If no
         * parameter the main entry of the chat application is displayed.
         */
        public static final String ACTION_SEND_ONE_TO_ONE_CHAT_MESSAGE = "com.gsma.services.rcs.action.SEND_ONE_TO_ONE_CHAT_MESSAGE";

        /**
         * Load the group chat application. This Intent takes into parameter an URI on the group
         * chat conversation (i.e. content://chats/chat_ID). If no parameter found the main entry of
         * the group chat application is displayed.
         */
        public static final String ACTION_VIEW_GROUP_CHAT = "com.gsma.services.rcs.action.VIEW_GROUP_CHAT";

        /**
         * Load the group chat application to start a new conversation with a group of contacts.
         * This Intent takes into parameter a list of contact URIs. If no parameter the main entry
         * of the group chat application is displayed.
         */
        public static final String ACTION_INITIATE_GROUP_CHAT = "com.gsma.services.rcs.action.INITIATE_GROUP_CHAT";

        private Chat() {
        }
    }

    /**
     * Intents for file transfer service
     */
    public static class FileTransfer {
        /**
         * Load the file transfer application to view a file transfer. This Intent takes into
         * parameter an URI on the file transfer (i.e. content://filetransfers/ft_ID). If no
         * parameter found the main entry of the file transfer application is displayed.
         */
        public static final String ACTION_VIEW_FILE_TRANSFER = "com.gsma.services.rcs.action.VIEW_FILE_TRANSFER";

        /**
         * Load the file transfer application to start a new file transfer to a given contact. This
         * Intent takes into parameter a contact URI (i.e. content://contacts/people/contact_ID). If
         * no parameter the main entry of the file transfer application is displayed.
         */
        public static final String ACTION_INITIATE_ONE_TO_ONE_FILE_TRANSFER = "com.gsma.services.rcs.action.INITIATE_ONE_TO_ONE_FILE_TRANSFER";

        /**
         * Load the group chat application to start a new conversation with a group of contacts and
         * send a file to them. This Intent takes into parameter a list of contact URIs (i.e.
         * content://contacts/people/contact_ID). If no parameter, the main entry of the group chat
         * application is displayed.
         */
        public static final String ACTION_INITIATE_GROUP_FILE_TRANSFER = "com.gsma.services.rcs.action.ACTION_INITIATE_GROUP_FILE_TRANSFER";

        private FileTransfer() {
        }
    }

}
