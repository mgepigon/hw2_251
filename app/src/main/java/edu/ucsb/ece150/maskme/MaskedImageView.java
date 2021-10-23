package edu.ucsb.ece150.maskme;

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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //mBitmap --
        mBitmap = ((BitmapDrawable) getDrawable()).getBitmap();
        if (mBitmap == null) {
            return;
        }
        double viewWidth = getWidth();
        double viewHeight = getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();

        Log.d(TAG, "Bitmap Preview Width: " + mBitmap.getWidth());
        Log.d(TAG, "Bitmap Preview Height: "+ mBitmap.getHeight());
        //Makes the same size as preview
        double scale = .63f;
                //Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

        //Draw the image still
        drawBitmap(canvas, scale);

        //Draw masks depending on the type that is chosen
        Bitmap mask;
        if (faces == null){
            return;
        }
        Log.d(TAG, "Size of list: " + faces.size());

        for (Face face : faces) {
            final Rect faceBoundingBox = face.getBoundingBox();
            final RectF box = translateRect(faceBoundingBox);
            switch (maskType) {
                case NONE:
                    mPaint.setColor(Color.WHITE);
                    mPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(faceBoundingBox, mPaint);
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

    public float scale(float x){
        return x * 1.0f;
    }

    private RectF translateRect(Rect rect) {
        return new RectF(scale(rect.left)+ID_X_OFFSET,
                scale(rect.top)-1.5f*ID_Y_OFFSET,
                scale(rect.right)+ID_X_OFFSET,
                scale(rect.bottom)+0.5f*ID_Y_OFFSET);
    }

    protected void drawMask(List<Face> faces, MaskType maskType){
        this.faces = faces;
        this.maskType = maskType;
        this.invalidate();
    }

    private void drawBitmap(Canvas canvas, double scale) {
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale), (int)(imageHeight * scale));
        canvas.drawBitmap(mBitmap, null, destBounds, null);
    }

    public void noFaces() {
        faces = null;
    }

    public void reset() {
        faces = null;
        setImageBitmap(null);
    }
}
