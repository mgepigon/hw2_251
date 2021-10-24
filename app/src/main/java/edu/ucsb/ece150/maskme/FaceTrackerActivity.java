package edu.ucsb.ece150.maskme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.GestureDetector;
import android.view.MotionEvent;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public class FaceTrackerActivity extends AppCompatActivity {
    private static final String TAG = "FaceTrackerLog";

    private GraphicOverlay mGraphicOverlay;

    private GraphicFaceTrackerFactory mFaceTrackerFactory;
    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private FaceDetector mFaceDetector;
    private ImageCapture mImageCapture;
    private Bitmap mCapturedImage;
    private MaskedImageView mImageView;
    private PreviewView mPreviewView;

    public int trueHeight;
    public int trueWidth;

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2; // Request code for Camera Permission

    private enum ButtonsMode {
        PREVIEW_CAPTURE, BACK_SAVE
    }

    public enum MaskType {
        NONE, FIRST, SECOND
    }

    private ButtonsMode buttonsMode = ButtonsMode.PREVIEW_CAPTURE;
    private Button mLeftButton;
    private Button mCenterButton;
    private Button mRightButton;

    private boolean inPreview = false;
    private MaskType maskTypeDrawn = MaskType.NONE;

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_tracker);

        //Create the toolbar
        final Toolbar myToolbar = findViewById(R.id.appToolbar);
        setSupportActionBar(myToolbar);

        mPreviewView = findViewById(R.id.previewView);
        mGraphicOverlay = findViewById(R.id.faceOverlay);
        mFaceTrackerFactory = new GraphicFaceTrackerFactory(mGraphicOverlay, getApplicationContext());

        //Create capture button
        mCenterButton = findViewById(R.id.centerButton);
        mCenterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(buttonsMode) {
                    //Press Capture
                    case PREVIEW_CAPTURE:
                        inPreview = false;
                        mLeftButton.setVisibility(View.VISIBLE);
                        if (mImageCapture != null) {
                            Toast.makeText(FaceTrackerActivity.this, "Captured!", Toast.LENGTH_SHORT).show();
                            mImageCapture.takePicture(new ThreadPerTaskExecutor(), new ImageCapture.OnImageCapturedCallback() {
                                @Override
                                public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                                    super.onCaptureSuccess(imageProxy);

                                    @SuppressLint("UnsafeExperimentalUsageError")
                                    final Image image = imageProxy.getImage();

                                    //Full Resolution Image Scaling & Rotation
                                    Bitmap imageFullRes = convertImageToBitmap(image);
                                    imageFullRes = rotateImage(imageFullRes, 90);
                                    mCapturedImage = Bitmap.createScaledBitmap(imageFullRes, trueWidth, trueHeight,false);
                                    Log.d(TAG, "Captured Image Width: "+ mCapturedImage.getWidth());
                                    Log.d(TAG, "Captured Image Height: "+ mCapturedImage.getHeight());
                                    Log.d(TAG, "Width: "+ trueWidth);
                                    Log.d(TAG, "Height: "+ trueHeight);
                                    imageProxy.close();
                                    // TODO: Save captured' image and get faces for use in preview screen
                                }
                                @Override
                                public void onError(@NonNull ImageCaptureException e) {
                                    super.onError(e);
                                    Log.e(TAG, "Unable to capture image", e);
                                }
                            });
                        }
                        break;
                    //Press Save
                    case BACK_SAVE:
                        // [TODO - for ECE 251 students only] Implement the Save feature.
                        inPreview = true;
                        //Check permissions
                        if(ActivityCompat.checkSelfPermission(FaceTrackerActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0) {
                            //Go to Storage Folder
                            String root = Environment.getExternalStorageDirectory().toString() + "/Pictures";
                            File myDir = new File(root);
                            myDir.mkdirs();
                            String f_name = String.format("MaskMe.jpg", System.currentTimeMillis());
                            File file = new File(myDir, f_name);
                            if (file.exists()) file.delete();
                            Log.i("LOAD", root + "/" + f_name);
                            Toast.makeText(FaceTrackerActivity.this, "Saved!", Toast.LENGTH_LONG).show();
                            try {
                                FileOutputStream out = new FileOutputStream(file);
                                Bitmap toSave = mImageView.save();
                                toSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                out.flush();
                                out.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else{
                            requestSavePermission();
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        mLeftButton = findViewById(R.id.leftButton);
        mLeftButton.setVisibility(View.GONE);
        mLeftButton.setOnClickListener(view -> {
            switch(buttonsMode) {
                //Press Preview
                case PREVIEW_CAPTURE:
                    mRightButton = findViewById(R.id.rightButton);
                    mRightButton.setVisibility(View.VISIBLE);
                    inPreview = true;
                    // Set image view with image captured
                    mImageView.setImageBitmap(mCapturedImage);
                    // Bring to front of preview view
                    mPreviewView.addView(mImageView);
                    mPreviewView.bringChildToFront(mImageView);

                    mLeftButton.setText("Back");
                    mCenterButton.setText("Save");
                    buttonsMode = ButtonsMode.BACK_SAVE;

                    //Draw Listener
                    mPreviewView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            //Grab Coordinates
                            mImageView.grab(event.getX(), event.getY());
                            return true;
                        }
                    });

                    drawMasksOnPreview(mCapturedImage);
                    break;
                //Press Back
                case BACK_SAVE:
                    mRightButton.setVisibility(View.GONE);
                    mPreviewView.removeView(mImageView);
                    //Clear Drawings
                    mImageView.clear();
                    //Change Button Text
                    mLeftButton.setText("Preview");
                    mCenterButton.setText("Capture");
                    buttonsMode = ButtonsMode.PREVIEW_CAPTURE;
                    break;
                default:
                    break;
            }
        });

        // Add Clear
        mRightButton = findViewById(R.id.rightButton);
        mRightButton.setVisibility(View.GONE
        );
        mRightButton.setOnClickListener(view -> {
            mImageView.clear();
        });

        mImageView = new MaskedImageView(getApplicationContext());
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

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
        SharedPreferences selectedMask = getSharedPreferences("maskSelect", Context.MODE_PRIVATE);
        switch(item.getItemId()) {
            // [TODO] Using this as an example, implement behavior when a mask option is pressed.
            case R.id.box:
                selectedMask.edit().putInt("selected", 0).apply();
                Toast.makeText(FaceTrackerActivity.this, "Bounding Box!", Toast.LENGTH_LONG).show();
                break;
            case R.id.circle:
                selectedMask.edit().putInt("selected", 1).apply();
                Toast.makeText(FaceTrackerActivity.this, "Circle Mask!", Toast.LENGTH_LONG).show();
                break;
            case R.id.leader:
                selectedMask.edit().putInt("selected", 2).apply();
                Toast.makeText(FaceTrackerActivity.this, "Leader Mask!", Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
        //Change masks even if in preview
        if (inPreview){
            drawMasksOnPreview(mCapturedImage);
        }
        return true;
    }

    private void drawMasksOnPreview(Bitmap bitmap) {
        if (bitmap == null) return;
        //Captured image shown in preview -- do processing on it
        final InputImage image = InputImage.fromBitmap(bitmap, 0);

        //Grab which mask was selected
        SharedPreferences selectedMaskPref = getSharedPreferences("maskSelect", Context.MODE_PRIVATE);
        int selectedMask = selectedMaskPref.getInt("selected", 69);

        Log.d(TAG, "Mask Selected Preview: " + selectedMask);
        switch(selectedMask){
            case 0:
                maskTypeDrawn=MaskType.NONE;
                break;
            case 1:
                maskTypeDrawn=MaskType.FIRST;
                break;
            case 2:
                maskTypeDrawn=MaskType.SECOND;
                break;
            default:
                break;
        }

        // TODO: Using the input image, call the face detector
        Task<List<Face>> result = mFaceDetector.process(image)
                    .addOnSuccessListener(
                            new OnSuccessListener<List<Face>>() {
                                @Override
                                public void onSuccess(List<Face> faces) {
                                    // Task completed successfully
                                    mFaceTrackerFactory.processFaces(faces);
                                    // When complete, use mImageView.drawMask(faces) to draw faces on the mask
                                    mImageView.drawMask(faces, maskTypeDrawn);
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    Log.e(TAG,"Failed face detection on preview", e);
                                }
                            });
    }

    private void parseInputImage(InputImage image) {
        // TODO: Call the face detector using the input image and send to face tracker factory
        if (buttonsMode == ButtonsMode.PREVIEW_CAPTURE){
            final Task<List<Face>> task = mFaceDetector.process(image);
            try {
                List<Face> faces = Tasks.await(task);
                mFaceTrackerFactory.processFaces(faces);
            }
            catch (Exception e){
                Log.e(TAG,"Failed face detection", e);
            }
        }
        else{
            mFaceTrackerFactory.processFaces(Collections.emptyList());
        }
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

    private void requestSavePermission() {
        Log.w(TAG, "Saving permission is not granted. Requesting permission.");

        ActivityCompat.requestPermissions(FaceTrackerActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {
        // Face Detector Initialization
        final FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .enableTracking()
                        .build();
        mFaceDetector = FaceDetection.getClient(options);

        // Image capture object set up -- gives user takePicture() capability
        mImageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Class that sets up the analyzer -- post processing
        final ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                    //.setTargetResolution(new Size(trueWidth, trueHeight))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

        // Analyzes image
        imageAnalysis.setAnalyzer(new ThreadPerTaskExecutor(), (imageProxy)-> {
            @SuppressLint("UnsafeExperimentalUsageError")
            final Image image = imageProxy.getImage();
            //Process image if exists
            if (image!=null){
                //get bitmap->resize->back to image class
//                resized = Bitmap.createScaledBitmap(convertImageToBitmap(image),1440, 2048, false);
//                Log.d(TAG, "CropRect: " + image.getCropRect());
//                Log.d(TAG, "Width Input Stream: " + image.getWidth());
//                Log.d(TAG, "Height Input Stream: "+ image.getHeight());
                final InputImage inputImage = InputImage.fromMediaImage(image,
                        imageProxy.getImageInfo().getRotationDegrees());
                //IN WIDTH -- 1440, OUT WIDTH -- 960 --> 1.5
                //OUT WIDTH -- 2048, OUT HEIGHT -- 720 --> 2.84
                // process and find faces in picture
                parseInputImage(inputImage);
            }
            imageProxy.close();
        });

        // Set up camera
        try {
            // Create camera provider
            final ProcessCameraProvider cameraProvider = mCameraProviderFuture.get();

            // Create a preview for the camera input to sit in
            final Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

            mPreviewView.post(new Runnable() {
                @Override
                public void run() {
                    trueHeight = mPreviewView.getHeight();
                    trueWidth = mPreviewView.getWidth();
                }
            });

            // Put it all together along with imageAnalysis block
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                    mImageCapture, imageAnalysis, preview);
        } catch (Exception e) {
            Log.e("MaskMe", "Unable to get camera provider.", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

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
