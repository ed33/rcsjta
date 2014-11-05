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
 * Video descriptor of a video share
 *  
 * @author Jean-Marc AUFFRET
 */
public class VideoDescriptor implements Parcelable {
	/**
	 * Video orientation
	 */
	private int orientation;

	/**
	 * Video frame width
	 */
	private int width;
	
	/**
	 * Video frame height
	 */
	private int height;
	
    /**
     * Constructor
     *
     * @param orientation Video orientation
     * @param width Video frame width
     * @param height Video frame height
     * @hide
     */
    public VideoDescriptor(int orientation, int width, int height) {
    	this.orientation = orientation;
    	this.width = width;
    	this.height = height;
    }
    
    /**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public VideoDescriptor(Parcel source) {
		this.orientation = source.readInt();
    	this.width = source.readInt();
    	this.height = source.readInt();
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
    	dest.writeInt(orientation);
    	dest.writeInt(width);
    	dest.writeInt(height);
    }
    
    /**
     * Parcelable creator
     * 
     * @hide
     */
    public static final Parcelable.Creator<VideoDescriptor> CREATOR
            = new Parcelable.Creator<VideoDescriptor>() {
        public VideoDescriptor createFromParcel(Parcel source) {
            return new VideoDescriptor(source);
        }

        public VideoDescriptor[] newArray(int size) {
            return new VideoDescriptor[size];
        }
    };	

    /**
    * Returns the video orientation
    * 
    * @return Orientation
    * @see VideoSharing.Orientation
    */
    public int getOrientation() {
    	return orientation;
    }
    
    /**
     * Returns the width of video frame (e.g. 176)
     * 
     * @return Video width in pixels
     */
    public int getWidth() {
    	return width;
    }
    
    /**
     * Returns the height of video frame (e.g. 144)
     * 
     * @return Video height in pixels
     */
    public int getHeight() {
    	return height;
    }
}
