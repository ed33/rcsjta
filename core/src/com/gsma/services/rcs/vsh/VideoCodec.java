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
package com.gsma.services.rcs.vsh;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Video codec
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoCodec implements Parcelable {
	/**
	 * Video encoding
	 */
	private String encoding;
	
	/**
	 * Payload
	 */
	private int payload;
	
	/**
	 * Clock rate
	 */
	private int clockRate;
	
	/**
	 * Frame rate
	 */
	private int frameRate;
	
	/**
	 * Bit rate
	 */
	private int bitRate;

	/**
	 * Video descriptor
	 */
	private VideoDescriptor descriptor;

	/**
	 * Video parameters
	 */
	private String parameters;
	
    /**
     * Constructor
     *
     * @param encoding Video encoding
     * @param payload Payload
     * @param clockRate Clock rate
     * @param frameRate Frame rate
     * @param bitRate Bit rate
     * @param descriptor Video descriptor
     * @param parameters Codec parameters
     * @hide
     */
    public VideoCodec(String encoding, int payload, int clockRate, int frameRate, int bitRate, VideoDescriptor descriptor, String parameters) {
    	this.encoding = encoding;
    	this.payload = payload;
    	this.clockRate = clockRate;
    	this.frameRate = frameRate;
    	this.bitRate = bitRate;
    	this.descriptor = descriptor;
    	this.parameters = parameters;
    }
    
    /**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public VideoCodec(Parcel source) {
		this.encoding = source.readString();
    	this.payload = source.readInt();
    	this.clockRate = source.readInt();
    	this.frameRate = source.readInt();
    	this.bitRate = source.readInt();
    	this.descriptor = source.readParcelable(VideoDescriptor.class.getClassLoader());
		this.parameters = source.readString();
	}

	/**
	 * Describe the kinds of special objects contained in this Parcelable's
	 * marshalled representation
	 * 
	 * @return Integer
     * @hide
	 */
	public int describeContents() {
        return 0;
    }

	/**
	 * Write parcelable object
	 * 
	 * @param dest The Parcel in which the object should be written
	 * @param flags Additional flags about how the object should be written
     * @hide
	 */
    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeString(encoding);
    	dest.writeInt(payload);
    	dest.writeInt(clockRate);
    	dest.writeInt(frameRate);
    	dest.writeInt(bitRate);
    	dest.writeParcelable(descriptor, flags);
    	dest.writeString(parameters);
    }
    
    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<VideoCodec> CREATOR
            = new Parcelable.Creator<VideoCodec>() {
        public VideoCodec createFromParcel(Parcel source) {
            return new VideoCodec(source);
        }

        public VideoCodec[] newArray(int size) {
            return new VideoCodec[size];
        }
    };	

    /**
    * Returns the encoding name (e.g. H264)
    * 
    * @return Encoding name
    */
    public String getEncoding() {
    	return encoding;
    }
    
    /**
     * Returns the codec payload type (e.g. 96)
     * 
     * @return Payload type
     */
    public int getPayloadType() {
    	return payload;
    }
    
    /**
     * Returns the codec clock rate (e.g. 90000)
     * 
     * @return Clock rate
     */
    public int getClockRate() {
    	return clockRate;
    }
    
    /**
     * Returns the codec frame rate (e.g. 10)
     * 
     * @return Frame rate
     */
    public int getFrameRate() {
    	return frameRate;
    }
    
    /**
     * Returns the codec bit rate (e.g. 64000)
     * 
     * @return Bit rate
     */
    public int getBitRate() {
    	return bitRate;
    }
    
    /**
     * Returns the video descriptor
     * 
     * @return Video descriptor
     */
    public VideoDescriptor getVideoDescriptor() {
    	return descriptor;
    }
    
    /**
     * Returns the list of codec parameters (e.g. profile-level-id, packetization-mode).
     * Parameters are are semicolon separated.
     * 
     * @return Parameters
     */
    public String getParameters() {
    	return parameters;    	
    }
}
