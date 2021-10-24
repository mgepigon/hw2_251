package edu.ucsb.ece150.maskme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.camera.view.PreviewView;

import com.google.mlkit.vision.face.Face;

import java.util.ArrayList;
import java.util.List;

import edu.ucsb.ece150.maskme.FaceTrackerActivity.MaskType;

public class MaskedImageView extends AppCompatImageView {
    private Context context;

    private static final String TAG = "MaskedImageViewLog";
    private List<Face> faces = null;
    private FaceTrackerActivity.MaskType maskType = FaceTrackerActivity.MaskType.NONE;
    Paint mPaint = new Paint();
    private Bitmap mBitmap;
    private Bitmap result;

    private ArrayList<PointF> touchDraw = new ArrayList<PointF>();;

    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;

    public MaskedImageView(Context context) {
        super(context);
        this.context = context;
    }

    public Bitmap save(){
        return result;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Paint Properties
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);

        //Create new canvas for Exporting
        result = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        Canvas saveCanvas = new Canvas(result);

        //Grab image, create bounding box
        mBitmap = ((BitmapDrawable)getDrawable()).getBitmap();
        Rect bounded = new Rect(0,0, getWidth(), getHeight());

        //Draw image on top of image view, bounded by mImageView boundaries
        saveCanvas.drawBitmap(mBitmap, null, bounded, null);

        //Mask Drawing
        if (faces == null){ return; }

        Log.d(TAG, "Size of list: " + faces.size());
        Bitmap mask;
        for (Face face : faces) {
            final Rect faceBoundingBox = face.getBoundingBox();
            final RectF box = translateRect(faceBoundingBox);
            switch (maskType) {
                case NONE:
                    mPaint.setStrokeWidth(1);
                    saveCanvas.drawRect(faceBoundingBox, mPaint);
                    canvas.drawRect(faceBoundingBox, mPaint);
                    break;
                case FIRST:
                    mask = BitmapFactory.decodeResource(context.getResources(), R.drawable.squid_circle);
                    saveCanvas.drawBitmap(mask, null, box, null);
                    canvas.drawBitmap(mask, null, box, null);
                    break;
                case SECOND:
                    mask = BitmapFactory.decodeResource(context.getResources(), R.drawable.squid_leader);
                    saveCanvas.drawBitmap(mask, null, box, null);
                    canvas.drawBitmap(mask, null, box, null);
                    break;
                default:
                    break;
            }
        }

        //Touch Drawing
        mPaint.setStrokeWidth(25);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        if (touchDraw.size() == 0){ return; }

        for (PointF p : touchDraw){
            saveCanvas.drawPoint(p.x,p.y,mPaint);
            canvas.drawPoint(p.x, p.y, mPaint);
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

    public void clear(){
        touchDraw.clear();
        this.invalidate();
    }

    //Grab coordinates
    public void grab(float xf, float yf){
        touchDraw.add(new PointF(xf,yf));
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
