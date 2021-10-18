package edu.ucsb.ece150.maskme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;

import java.nio.ByteBuffer;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public class FaceTrackerActivity extends AppCompatActivity {
    private static final String TAG = "FaceTracker";

    private GraphicOverlay mGraphicOverlay;

    private GraphicFaceTrackerFactory mFaceTrackerFactory;
    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private FaceDetector mFaceDetector;
    private ImageCapture mImageCapture;
    private Bitmap mCapturedImage;
    private MaskedImageView mImageView;
    private PreviewView mPreviewView;

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2; // Request code for Camera Permission

    private enum ButtonsMode {
        PREVIEW_CAPTURE, BACK_SAVE
    }

    public enum MaskType {
        FIRST, SECOND
    }

    private ButtonsMode buttonsMode = ButtonsMode.PREVIEW_CAPTURE;
    private Button mLeftButton;
    private Button mCenterButton;
    private Button mRightButton;

    private boolean previewButtonVisible = false;
    private MaskType maskTypeDrawn = MaskType.FIRST;

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_tracker);

        final Toolbar myToolbar = findViewById(R.id.appToolbar);
        setSupportActionBar(myToolbar);

        mPreviewView = findViewById(R.id.previewView);
        mGraphicOverlay = findViewById(R.id.faceOverlay);

        mCenterButton = findViewById(R.id.centerButton);
        mCenterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(buttonsMode) {
                    case PREVIEW_CAPTURE:
                        mLeftButton.setVisibility(View.VISIBLE);
                        if (mImageCapture != null) {
                            mImageCapture.takePicture(new ThreadPerTaskExecutor(), new ImageCapture.OnImageCapturedCallback() {
                                @Override
                                public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                                    super.onCaptureSuccess(imageProxy);

                                    @SuppressLint("UnsafeExperimentalUsageError")
                                    final Image image = imageProxy.getImage();

                                    // TODO: Save captured image and get faces for use in preview screen
                                }

                                @Override
                                public void onError(@NonNull ImageCaptureException e) {
                                    super.onError(e);
                                    Log.e(TAG, "Unable to capture image", e);
                                }
                            });
                        }
                        break;
                    case BACK_SAVE:
                        // [TODO - for ECE 251 students only] Implement the Save feature.
                    default:
                        break;
                }
            }
        });

        previewButtonVisible = false;
        mLeftButton = findViewById(R.id.leftButton);
        mLeftButton.setVisibility(View.GONE);
        mLeftButton.setOnClickListener(view -> {
            switch(buttonsMode) {
                case PREVIEW_CAPTURE:
                    mImageView.setImageBitmap(mCapturedImage);
                    mPreviewView.addView(mImageView);
                    mPreviewView.bringChildToFront(mImageView);

                    mLeftButton.setText("Back");
                    mCenterButton.setText("Save");
                    buttonsMode = ButtonsMode.BACK_SAVE;

                    drawMasksOnPreview(mCapturedImage);
                    break;
                case BACK_SAVE:
                    mPreviewView.removeView(mImageView);

                    mLeftButton.setText("Preview");
                    mCenterButton.setText("Capture");
                    buttonsMode = ButtonsMode.PREVIEW_CAPTURE;
                    break;
                default:
                    break;
            }
        });

        mImageView = new MaskedImageView(getApplicationContext());
        mImageView.setScaleType(ImageView.ScaleType.FIT_XY);

        // Check for permissions before accessing the camera. If the permission is not
        // yet granted, request permission from the user.
        int cameraPermissionGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(cameraPermissionGranted == PackageManager.PERMISSION_GRANTED) {
            mCameraProviderFuture = ProcessCameraProvider.getInstance(this);
            mCameraProviderFuture.addListener(() -> {
                createCameraSource();
            }, ContextCompat.getMainExecutor(this));
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Rotates a Bitmap by a specified angle in degrees.
     * You may need this depending on how you implement your image capture function.
     */
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Log.v("MyLogger", "Rotating bitmap " + angle + " degrees.");
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Converts an Image object in JPEG format into a Bitmap.
     * Adapted from https://stackoverflow.com/questions/41775968/how-to-convert-android-media-image-to-bitmap-object
     */
    public Bitmap convertImageToBitmap(Image image) {
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        final byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

    /**
     * Creates the menu on the Action Bar for selecting masks.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final Toolbar myToolbar = findViewById(R.id.appToolbar);
        myToolbar.inflateMenu(R.menu.masks);
        myToolbar.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
        return true;
    }

    /**
     * Handler function that determines what happens when an option is pressed in the Action Bar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            // [TODO] Using this as an example, implement behavior when a mask option is pressed.
            case R.id.example:
                break;
            default:
                break;
        }
        return true;
    }

    private void drawMasksOnPreview(Bitmap bitmap) {
        if (bitmap == null) return;
        final InputImage image = InputImage.fromBitmap(bitmap, 0);

        // TODO: Using the input image, call the face detector
        // When complete, use mImageView.drawMask(faces) to draw faces on the mask
    }

    private void parseInputImage(InputImage image) {
        // TODO: Call the face detector using the input image and send to face tracker factory
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission.");

        final String[] permissions = new String[] {Manifest.permission.CAMERA};

        if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;
        Snackbar.make(mGraphicOverlay, R.string.permissionCameraRationale, Snackbar.LENGTH_INDEFINITE)
            .setAction("OK", view -> ActivityCompat.requestPermissions(thisActivity, permissions,RC_HANDLE_CAMERA_PERM)).show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {
        // [TODO] Create a face detector for real time face detection

        // 1. Set up image capture to take pictures using camera stream

        // 2. Create an image analysis buffer to stream camera frames to a face detector

        // 3. Create an analyzer that analyzes incoming images for faces on a new thread

        // 4. Set up preview view for a live camera feed on the app

        // 5. Finally, specify camera to use and bind all of the above to the camera lifecycle
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If you are using the front camera, change the last argument to true.
        mGraphicOverlay.setCameraInfo(mPreviewView.getWidth(), mPreviewView.getHeight(), false);
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // We have permission, so create the camera source
            mCameraProviderFuture = ProcessCameraProvider.getInstance(this);
            mCameraProviderFuture.addListener(() -> {
                createCameraSource();
            }, ContextCompat.getMainExecutor(this));
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        finish();
    }
}
