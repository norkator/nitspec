package com.nitramite.adapters;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class SingleTapDetector extends GestureDetector.SimpleOnGestureListener {

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        return true;
    }

} // End of class