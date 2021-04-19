package com.nitramite.nitspec;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

// https://stackoverflow.com/questions/32718941/is-it-possible-to-zoom-and-focus-using-opencv-on-android
public class ZoomCameraView extends JavaCameraView {


    // Variables
    private Camera.Parameters parameters;
    private Boolean isZoomSupported = false;


    // Constructor
    public ZoomCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public ZoomCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * Initialize camera method
     *
     * @param width  frame width
     * @param height frame height
     * @return init result
     */
    protected boolean initializeCamera(int width, int height) {
        boolean ret = super.initializeCamera(width, height);
        parameters = mCamera.getParameters();
        isZoomSupported = parameters.isZoomSupported();
        return ret;
    }


    /**
     * Get maximum supported zoom level intereger
     *
     * @return zoom level
     */
    protected Integer getMaxZoomLevel() {
        return parameters.getMaxZoom();
    }


    /**
     * Set zoom level
     *
     * @param zoomLevel integer value
     */
    protected void setZoomLevel(final int zoomLevel) {
        parameters.setZoom(zoomLevel);
        mCamera.setParameters(parameters);
    }


    /**
     * Return zoom supported boolean
     *
     * @return boolean
     */
    public Boolean getZoomSupported() {
        return isZoomSupported;
    }


    /**
     * Get camera min exposure compensation
     *
     * @return integer
     */
    public Integer getMinExposureCompensation() {
        if (parameters != null) {
            return parameters.getMinExposureCompensation();
        } else {
            return 0;
        }
    }


    /**
     * Get camera max exposure compensation
     *
     * @return integer
     */
    public Integer getMaxExposureCompensation() {
        if (parameters != null) {
            return parameters.getMaxExposureCompensation();
        } else {
            return 0;
        }
    }


    /**
     * Set camera exposure compensation value
     *
     * @param exposureCompensationValue integer
     */
    public boolean setExposureCompensation(final Integer exposureCompensationValue) {
        if (parameters != null) {
            if (parameters.isAutoExposureLockSupported()) {
                parameters.setAutoExposureLock(false);
            }
            parameters.setExposureCompensation(exposureCompensationValue);
            mCamera.setParameters(parameters);
            return true;
        }
        return false;
    }


    /**
     * Set fps range for camera
     *
     * @param min min
     * @param max max
     */
    public boolean setPreviewFpsRange(final Integer min, final Integer max) {
        if (parameters != null) {
            parameters.setPreviewFpsRange(min, max); // 12000, 15000
            mCamera.setParameters(parameters);
            return true;
        }
        return false;
    }


    /**
     * Get camera settings
     *
     * @return flatten string
     */
    public String getFlatten() {
        return parameters.flatten();
    }


} // End of class