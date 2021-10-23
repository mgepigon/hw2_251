/*
 * Copyright (C) The Android Open Source Project
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
 */

package edu.ucsb.ece150.maskme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.google.mlkit.vision.face.Face;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
    private SharedPreferences myPreferences;

    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;

    private Context context;

    FaceGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);
        this.context = context;

        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        if (mFace == null) {
            return;
        }

        // Get bounding box for face
        final Rect faceBoundingBox = mFace.getBoundingBox();
        final RectF box = translateRect(faceBoundingBox);
        // Grab center coordinate of each face
        final float x = translateX(faceBoundingBox.centerX());
        final float y = translateY(faceBoundingBox.centerY());

        //set own color -- white
        Paint color = new Paint();
        color.setColor(COLOR_CHOICES[5]);
        color.setStyle(Paint.Style.STROKE);

        myPreferences = context.getSharedPreferences("maskSelect", Context.MODE_PRIVATE);
        int selectedMask = myPreferences.getInt("selected", 69);
        Log.d("FaceGraphicLog", "Mask Selected: " + selectedMask);
        // [TODO] Draw real time masks for all faces

        Bitmap mask;
        switch (selectedMask) {
            case 0:
                //Draw rectangle that surrounds entire face
                canvas.drawRect(box, color);
                break;
            case 1:
                mask = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.squid_circle);
                //canvas.drawRect(box, color);
                canvas.drawBitmap(mask, null, box, null);
                break;
            case 2:
                mask = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.squid_leader);
                //canvas.drawRect(box, color);
                canvas.drawBitmap(mask, null, box, null);
                break;
            default:
                break;
        }
    }

    private RectF translateRect(Rect rect) {
        return new RectF(translateX(rect.left) + ID_X_OFFSET, translateY(rect.top) - 1.5f * ID_Y_OFFSET, translateX(rect.right) + ID_X_OFFSET, translateY(rect.bottom) + 0.5f * ID_Y_OFFSET);
    }
}
