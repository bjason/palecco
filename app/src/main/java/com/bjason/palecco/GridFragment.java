/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bjason.palecco;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.SharedElementCallback;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.ContentValues.TAG;

/**
 * A fragment for displaying a grid of images.
 */
public class GridFragment extends Fragment {
    private static final int REQUEST_CODE_ACTION_ADD_FROM_STORAGE = 0;
    private static final int REQUEST_CODE_ACTION_ADD_FROM_CAMERA = 1;
    private static final String IMAGE_ID_DATAHOLDER = "key.IMAGE";
    private static final String BUNDLE_SAVED_BITMAPS = "bitmaps";
    public static final String DIR_NAME_FOR_IMAGE = "Images";

    public static ArrayList<Bitmap> mBitmaps;
    private View mView;
    private GridAdapter mAdapter;

    public static int currentPosition; // which image clicked
    private static final String KEY_CURRENT_POSITION = "key.currentPosition";

    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        /* set images list */
        if (savedInstanceState != null) {
            mBitmaps = (ArrayList<Bitmap>) WeakDataHolder.getInstance().getData(IMAGE_ID_DATAHOLDER);
        } else {
            mBitmaps = new ArrayList<>();
        }

        recyclerView = (RecyclerView) inflater.inflate(R.layout.fragment_grid, container, false);
        mAdapter = new GridAdapter(this);
        recyclerView.setAdapter(mAdapter);
        try {
            addCards();
        } catch (IOException e) {
            e.printStackTrace();
        }

        prepareTransitions();
        postponeEnterTransition();

        return recyclerView;
    }


    /**
     * Adds the provided bitmap to a list, and repopulates the main GridView with the new card.
     */
    private void addCard(Bitmap bitmap) {
        mBitmaps.add(bitmap);
        // notify
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Adds cards with the default images stored in assets.
     */
    private void addCards() throws IOException {
        // load sample images
        File directory = new File(getContext().getFilesDir(), "Palette" + File.separator + DIR_NAME_FOR_IMAGE);
        if (directory.listFiles() == null) {
            recyclerView.findViewById(R.id.emptyLibPrompt).setVisibility(View.VISIBLE);
        } else {
            for (File image : directory.listFiles()) {
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(image));
                addCard(b);
            }
        }
    }

    /* add pictures funcs */
    private void getPhotoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_ACTION_ADD_FROM_STORAGE);  //Check onActivityResult on how to handle the photo selected}
    }

    private void getPhotoFromCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQUEST_CODE_ACTION_ADD_FROM_CAMERA);
    }

    private void openGallery() {
        //Ask for permission to access/read storage for marshmallow and greater here
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},   //Requesting the permission with the appropriate request code
                        REQUEST_CODE_ACTION_ADD_FROM_STORAGE);
            } else {
                //If the permission was already granted the first time it will run the method to open the gallery intent
                getPhotoFromGallery();
            }
        }
    }

    private void openCamera() {
        //Ask for permission to access/read storage for marshmallow and greater here
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (getContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.CAMERA},   //Requesting the permission with the appropriate request code
                        REQUEST_CODE_ACTION_ADD_FROM_CAMERA);
            } else {
                //If the permission was already granted the first time it will run the method to open the gallery intent
                getPhotoFromCamera();
            }
        }
    }

    /* END add pictures funcs */

    // save data during bundle in a helper class so that it would overflow
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedState) {
        super.onSaveInstanceState(savedState);

        WeakDataHolder.getInstance().saveData(IMAGE_ID_DATAHOLDER, mBitmaps);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ACTION_ADD_FROM_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now user should be able to access gallery
                getPhotoFromGallery();
            } else {
                Snackbar.make(mView, "Please grant permission to proceed", Snackbar.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_CODE_ACTION_ADD_FROM_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now user should be able to access the Camera
                getPhotoFromCamera();
            } else {
                Snackbar.make(mView, "Please grant permission to proceed", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap = null;
        if (Activity.RESULT_OK == resultCode) {
            if (REQUEST_CODE_ACTION_ADD_FROM_STORAGE == requestCode) {
                try {
                    InputStream stream = getContext().getContentResolver().openInputStream(
                            data.getData());
                    bitmap = BitmapFactory.decodeStream(stream);
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (REQUEST_CODE_ACTION_ADD_FROM_CAMERA == requestCode) {
                Bundle extras = data.getExtras();
                bitmap = (Bitmap) extras.get("data"); // Just a thumbnail, but works okay for this.
            }
        }
        if (bitmap != null) {
            // addCard(bitmap);

            // TODO generate picture with its palette async-ly and display a progress bar
            Bitmap pictureWithPalette = null;

            saveImageToInternal(bitmap);

            // TODO display this picture to libraryLayout
            addCard(bitmap);
        }
    }

    void saveImageToInternal(Bitmap bitmap) {// save the image to internal memory
        try {
            File path = new File(getContext().getFilesDir(), "Palette" + File.separator + DIR_NAME_FOR_IMAGE);
            if (!path.exists()) {
                if (!path.mkdirs()) {
                    throw new IOException();
                }
            }
            // use current time to name the picture
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String imageName = sdf.format(new Date());

            File outFile = new File(path, imageName + ".jpeg");
            FileOutputStream outputStream = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Saving received message failed with", e);
            Snackbar.make(mView, "Saving received message failed", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /* fab setting */
        FloatingActionButton fabGallery = getActivity().findViewById(R.id.fabGallery);
        fabGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        FloatingActionButton fabCamera = getActivity().findViewById(R.id.fabCamera);
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
            }
        });

        scrollToPosition();
    }

    /**
     * Scrolls the recycler view to show the last viewed item in the grid. This is important when
     * navigating back from the grid.
     */
    private void scrollToPosition() {
        recyclerView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left,
                                       int top,
                                       int right,
                                       int bottom,
                                       int oldLeft,
                                       int oldTop,
                                       int oldRight,
                                       int oldBottom) {
                recyclerView.removeOnLayoutChangeListener(this);
                final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                View viewAtPosition = layoutManager.findViewByPosition(MainActivity.currentPosition);
                // Scroll to position if the view for the current position is null (not currently part of
                // layout manager children), or it's not completely visible.
                if (viewAtPosition == null || layoutManager
                        .isViewPartiallyVisible(viewAtPosition, false, true)) {
                    recyclerView.post(() -> layoutManager.scrollToPosition(MainActivity.currentPosition));
                }
            }
        });
    }

    /**
     * Prepares the shared element transition to the pager fragment, as well as the other transitions
     * that affect the flow.
     */
    private void prepareTransitions() {
        setExitTransition(TransitionInflater.from(getContext())
                .inflateTransition(R.transition.grid_exit_transition));

        // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
        setExitSharedElementCallback(
                new SharedElementCallback() {
                    @Override
                    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                        // Locate the ViewHolder for the clicked position.
                        RecyclerView.ViewHolder selectedViewHolder = recyclerView
                                .findViewHolderForAdapterPosition(MainActivity.currentPosition);
                        if (selectedViewHolder == null) {
                            return;
                        }

                        // Map the first shared element name to the child ImageView.
                        sharedElements
                                .put(names.get(0), selectedViewHolder.itemView.findViewById(R.id.card_image));
                    }
                });
    }
}
