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

package com.gsma.services.rcs.extension;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class offers the main entry point to initiate and to manage multimedia sessions. Several
 * applications may connect/disconnect to the API.
 * 
 * @author Jean-Marc AUFFRET
 */
public final class MultimediaSessionService extends RcsService {
    /**
     * API
     */
    private IMultimediaSessionService mApi;

    private final Map<MultimediaMessagingSessionListener, WeakReference<IMultimediaMessagingSessionListener>> mMultimediaMessagingSessionListeners = new WeakHashMap<MultimediaMessagingSessionListener, WeakReference<IMultimediaMessagingSessionListener>>();

    private final Map<MultimediaStreamingSessionListener, WeakReference<IMultimediaStreamingSessionListener>> mMultimediaStreamingSessionListeners = new WeakHashMap<MultimediaStreamingSessionListener, WeakReference<IMultimediaStreamingSessionListener>>();

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public MultimediaSessionService(Context ctx, RcsServiceListener listener) {
        super(ctx, listener);
    }

    /**
     * Connects to the API
     */
    public final void connect() throws RcsPermissionDeniedException {
        Intent serviceIntent = new Intent(IMultimediaSessionService.class.getName());
        serviceIntent.setPackage(RcsServiceControl.RCS_STACK_PACKAGENAME);
        mCtx.bindService(serviceIntent, apiConnection, 0);
    }

    /**
     * Disconnects from the API
     */
    public void disconnect() {
        try {
            mCtx.unbindService(apiConnection);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    /**
     * Set API interface
     * 
     * @param api API interface
     */
    protected void setApi(IInterface api) {
        super.setApi(api);
        mApi = (IMultimediaSessionService) api;
    }

    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            setApi(IMultimediaSessionService.Stub.asInterface(service));
            if (mListener != null) {
                mListener.onServiceConnected();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            setApi(null);
            if (mListener == null) {
                return;
            }
            ReasonCode reasonCode = ReasonCode.CONNECTION_LOST;
            try {
                if (!mRcsServiceControl.isActivated()) {
                    reasonCode = ReasonCode.SERVICE_DISABLED;
                }
            } catch (RcsPersistentStorageException e) {
                /* Do nothing */
            }
            mListener.onServiceDisconnected(reasonCode);
        }
    };

    /**
     * Returns the configuration of the multimedia session service
     * 
     * @return MultimediaSessionServiceConfiguration
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public MultimediaSessionServiceConfiguration getConfiguration()
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new MultimediaSessionServiceConfiguration(mApi.getConfiguration());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Initiates a new session for real time messaging with a remote contact and for a given service
     * extension. The messages are exchanged in real time during the session may be from any type.
     * The parameter contact supports the following formats: MSISDN in national or international
     * format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not supported an
     * exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact identifier
     * @return MultimediaMessagingSession
     * @throws RcsServiceNotRegisteredException
     * @throws RcsPermissionDeniedException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public MultimediaMessagingSession initiateMessagingSession(String serviceId, ContactId contact)
            throws RcsServiceNotRegisteredException, RcsPermissionDeniedException,
            RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IMultimediaMessagingSession sessionIntf = mApi.initiateMessagingSession(serviceId,
                    contact);
            if (sessionIntf != null) {
                return new MultimediaMessagingSession(sessionIntf);

            } else {
                return null;
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsServiceNotRegisteredException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the list of messaging sessions associated to a given service ID
     * 
     * @param serviceId Service ID
     * @return Set&lt;MultimediaMessagingSession&gt; List of messaging sessions
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public Set<MultimediaMessagingSession> getMessagingSessions(String serviceId)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            Set<MultimediaMessagingSession> result = new HashSet<MultimediaMessagingSession>();
            List<IBinder> mmsList = mApi.getMessagingSessions(serviceId);
            for (IBinder binder : mmsList) {
                MultimediaMessagingSession session = new MultimediaMessagingSession(
                        IMultimediaMessagingSession.Stub.asInterface(binder));
                result.add(session);
            }
            return result;

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns a current messaging session from its unique session ID
     * 
     * @param sessionId
     * @return MultimediaMessagingSession Multimedia messaging session or null if not found
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public MultimediaMessagingSession getMessagingSession(String sessionId)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IMultimediaMessagingSession sessionIntf = mApi.getMessagingSession(sessionId);
            if (sessionIntf != null) {
                return new MultimediaMessagingSession(sessionIntf);

            } else {
                return null;
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Initiates a new session for real time streaming with a remote contact and for a given service
     * extension. The payload are exchanged in real time during the session and may be from any
     * type. The parameter contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of the contact is not
     * supported an exception is thrown.
     * 
     * @param serviceId Service ID
     * @param contact Contact ID
     * @return MultimediaStreamingSession
     * @throws RcsServiceNotRegisteredException
     * @throws RcsPermissionDeniedException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public MultimediaStreamingSession initiateStreamingSession(String serviceId, ContactId contact)
            throws RcsServiceNotRegisteredException, RcsPermissionDeniedException,
            RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IMultimediaStreamingSession sessionIntf = mApi.initiateStreamingSession(serviceId,
                    contact);
            if (sessionIntf != null) {
                return new MultimediaStreamingSession(sessionIntf);

            } else {
                return null;
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsServiceNotRegisteredException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the list of streaming sessions associated to a given service ID
     * 
     * @param serviceId Service ID
     * @return Set&lt;MultimediaStreamingSession&gt; List of streaming sessions
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public Set<MultimediaStreamingSession> getStreamingSessions(String serviceId)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            Set<MultimediaStreamingSession> result = new HashSet<MultimediaStreamingSession>();
            List<IBinder> mmsList = mApi.getStreamingSessions(serviceId);
            for (IBinder binder : mmsList) {
                MultimediaStreamingSession session = new MultimediaStreamingSession(
                        IMultimediaStreamingSession.Stub.asInterface(binder));
                result.add(session);
            }
            return result;

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns a current streaming session from its unique session ID
     * 
     * @param sessionId
     * @return MultimediaStreamingSession Multimedia streaming session or null if not found
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public MultimediaStreamingSession getStreamingSession(String sessionId)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IMultimediaStreamingSession sessionIntf = mApi.getStreamingSession(sessionId);
            if (sessionIntf != null) {
                return new MultimediaStreamingSession(sessionIntf);

            } else {
                return null;
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Adds a listener on multimedia messaging session events
     * 
     * @param listener Session event listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addEventListener(MultimediaMessagingSessionListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (listener == null) {
            throw new RcsIllegalArgumentException("listener must not be null!");
        }
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IMultimediaMessagingSessionListener multimediaMessagingSessionListener = new MultimediaMessagingSessionListenerImpl(
                    listener);
            mMultimediaMessagingSessionListeners.put(listener,
                    new WeakReference<IMultimediaMessagingSessionListener>(
                            multimediaMessagingSessionListener));
            mApi.addEventListener2(multimediaMessagingSessionListener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Removes a listener on multimedia messaging session events
     * 
     * @param listener Session event listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeEventListener(MultimediaMessagingSessionListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<IMultimediaMessagingSessionListener> weakRef = mMultimediaMessagingSessionListeners
                    .remove(listener);
            if (weakRef == null) {
                return;
            }
            IMultimediaMessagingSessionListener multimediaMessagingSessionListener = weakRef.get();
            if (multimediaMessagingSessionListener != null) {
                mApi.removeEventListener2(multimediaMessagingSessionListener);
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Adds a listener on multimedia streaming session events
     * 
     * @param listener Session event listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addEventListener(MultimediaStreamingSessionListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (listener == null) {
            throw new RcsIllegalArgumentException("listener must not be null!");
        }
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IMultimediaStreamingSessionListener multimediaStreamingSessionListener = new MultimediaStreamingSessionListenerImpl(
                    listener);
            mMultimediaStreamingSessionListeners.put(listener,
                    new WeakReference<IMultimediaStreamingSessionListener>(
                            multimediaStreamingSessionListener));
            mApi.addEventListener3(multimediaStreamingSessionListener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Removes a listener on multimedia streaming session events
     * 
     * @param listener Session event listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeEventListener(MultimediaStreamingSessionListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<IMultimediaStreamingSessionListener> weakRef = mMultimediaStreamingSessionListeners
                    .remove(listener);
            if (weakRef == null) {
                return;
            }
            IMultimediaStreamingSessionListener multimediaStreamingSessionListener = weakRef.get();
            if (multimediaStreamingSessionListener != null) {
                mApi.removeEventListener3(multimediaStreamingSessionListener);
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
