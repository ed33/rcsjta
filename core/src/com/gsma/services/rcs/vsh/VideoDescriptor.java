package com.gsma.services.rcs.vsh;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Video descriptor for the default video player
 *  
 * @author Jean-Marc AUFFRET
 */
public class VideoDescriptor implements Parcelable {
	/**
	 * Video orientation
	 */
	public enum Orientation {
		ANGLE_0(0),
		ANGLE_90(1),
		ANGLE_180(2),
		ANGLE_270(3);
		
		private int value;
		
		private Orientation(int value) {
		    this.value = value;
		}
		
		public int getValue() {
		    return this.value;
		}
		
		public static Orientation convert(int value) {
		    if (ANGLE_0.value == value) {
		        return ANGLE_0;
		    } else if (ANGLE_90.value == value) {
		        return ANGLE_90;
		    } else if (ANGLE_180.value == value) {
		        return ANGLE_180;
		    } else if (ANGLE_270.value == value) {
		        return ANGLE_270;
		    }
		    return ANGLE_0;
		}	
	}
	
	/**
	 * Camera source
	 */
	public enum CameraSource {
		BACK(0),
		FRONT(1);
		
		private int value;
		
		private CameraSource(int value) {
		    this.value = value;
		}
		
		public int getValue() {
		    return this.value;
		}
		
		public static CameraSource convert(int value) {
		    if (BACK.value == value) {
		        return BACK;
		    } else if (FRONT.value == value) {
		        return FRONT;
		    }
		    return BACK;
		}    	
	}
	
	/**
	 * Video orientation
	 */
	private Orientation orientation;

	/**
	 * Screen width
	 */
	private int width;
	
	/**
	 * Screen height
	 */
	private int height;

	/**
	 * Camera source
	 */
	private CameraSource source;
	
    /**
     * Constructor
     *
     * @param orientation Video orientation
     * @param width Video width
     * @param height Video height
     * @param source Camera source
     * @hide
     */
    public VideoDescriptor(Orientation orientation, int width, int height, CameraSource source) {
    	this.orientation = orientation;
    	this.width = width;
    	this.height = height;
    	this.source = source;
    }
    
    /**
	 * Constructor
	 * 
	 * @param source Parcelable source
     * @hide
	 */
	public VideoDescriptor(Parcel source) {
		this.orientation = Orientation.convert(source.readInt());
    	this.width = source.readInt();
    	this.height = source.readInt();
		this.source = CameraSource.convert(source.readInt());
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
    	dest.writeInt(orientation.getValue());
    	dest.writeInt(width);
    	dest.writeInt(height);
    	dest.writeInt(source.getValue());
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
    * @see VideoDescriptor.Orientation
    */
    public Orientation getOrientation() {
    	return orientation;
    }
    
    /**
     * Returns the video width (e.g. 176)
     * 
     * @return Video width
     */
    public int getVideoWidth() {
    	return width;
    }
    
    /**
     * Returns the video height (e.g. 144)
     * 
     * @return Video height
     */
    public int getVideoHeight() {
    	return height;
    }
    
    /**
    * Returns the camera source
    * 
    * @return Source
    * @see VideoDescriptor.CameraSource
    */
    public CameraSource getCameraSource() {
    	return source;
    }
}
