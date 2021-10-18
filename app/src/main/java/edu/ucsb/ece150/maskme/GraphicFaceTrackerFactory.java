package edu.ucsb.ece150.maskme;

import android.content.Context;

import com.google.mlkit.vision.face.Face;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Factory for managing face trackers. Face trackers will be added as needed - one for each individual.
 */
public class GraphicFaceTrackerFactory {
    private final HashMap<Integer, GraphicFaceTracker> mFaceMap;
    private final GraphicOverlay mGraphicOverlay;
    private final Context mContext;

    GraphicFaceTrackerFactory(GraphicOverlay overlay, Context context) {
        mFaceMap = new HashMap<>();
        mGraphicOverlay = overlay;
        mContext = context;
    }

    public void processFaces(List<Face> faces) {
        final Set<Integer> trackedFaceIds = new HashSet<>(mFaceMap.keySet());

        for (Face face : faces) {
            // Check if this face exists in our map
            if (mFaceMap.containsKey(face.getTrackingId())) {
                // Face already exists, update it
                mFaceMap.get(face.getTrackingId()).update(face);
            } else {
                // This is a new face, create a new GraphicFaceTracker and store it in the map
                final GraphicFaceTracker newFaceTracker = new GraphicFaceTracker(mGraphicOverlay, mContext);
                newFaceTracker.update(face);
                mFaceMap.put(face.getTrackingId(), newFaceTracker);
            }

            trackedFaceIds.remove(face.getTrackingId());
        }

        // Faces that are left in the tracked list are missing, remove them
        trackedFaceIds.forEach(removedFace -> {
            mFaceMap.get(removedFace).remove();
            mFaceMap.remove(removedFace);
        });
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private static class GraphicFaceTracker {
        private final GraphicOverlay mOverlay;
        private final FaceGraphic mFaceGraphic;

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        GraphicFaceTracker(GraphicOverlay overlay, Context context) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, context);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        public void update(Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Remove the graphic when the corresponding face was not detected. This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        public void remove() {
            mOverlay.remove(mFaceGraphic);
        }
    }
}
