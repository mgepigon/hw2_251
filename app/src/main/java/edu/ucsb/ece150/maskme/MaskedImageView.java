package edu.ucsb.ece150.maskme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.face.Face;

import java.util.List;

import edu.ucsb.ece150.maskme.FaceTrackerActivity.MaskType;

public class MaskedImageView extends AppCompatImageView {
    private Context context;

    private static final String TAG = "MaskedImageViewLog";
    private List<Face> faces = null;
    private FaceTrackerActivity.MaskType maskType = FaceTrackerActivity.MaskType.NONE;
    Paint mPaint = new Paint();
    private Bitmap mBitmap;

    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;

    public MaskedImageView(Context context) {
        super(context);
        this.context = context;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //grab image and resize it
        mBitmap = ((BitmapDrawable)getDrawable()).getBitmap();
        Rect bounded = new Rect(0,0, getWidth(), getHeight());

        if (mBitmap==null){
            return;
        }

        //Draw image on top of image view, bounded by image view boundaries
        canvas.drawBitmap(mBitmap, null, bounded, null);

        //With list of faces, draw masks
        if (faces == null){
            return;
        }
        Log.d(TAG, "Size of list: " + faces.size());
        Bitmap mask;
        for (Face face : faces) {
            final Rect faceBoundingBox = face.getBoundingBox();
            final RectF box = translateRect(faceBoundingBox);
            switch (maskType) {
                case NONE:
                    mPaint.setColor(Color.WHITE);
                    mPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(box, mPaint);
                    break;
                case FIRST:
                    mask = BitmapFactory.decodeResource(context.getResources(), R.drawable.squid_circle);
                    canvas.drawBitmap(mask, null, box, null);
                    break;
                case SECOND:
                    mask = BitmapFactory.decodeResource(context.getResources(), R.drawable.squid_leader);
                    canvas.drawBitmap(mask, null, box, null);
                    break;
                default:
                    break;
            }
        }
    }

    private RectF translateRect(Rect rect) {
        return new RectF(rect.left,
                rect.top-1.5f*ID_Y_OFFSET,
                rect.right,
                rect.bottom+0.5f*ID_Y_OFFSET);
    }

    protected void drawMask(List<Face> faces, MaskType maskType){
        this.faces = faces;
        this.maskType = maskType;
        this.invalidate();
    }

    public void noFaces() {
        faces = null;
    }

    public void reset() {
        faces = null;
        setImageBitmap(null);
    }
}
