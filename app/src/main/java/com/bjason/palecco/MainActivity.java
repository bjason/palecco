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
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ACTION_ADD_FROM_STORAGE = 0;
    private static final int REQUEST_CODE_ACTION_ADD_FROM_CAMERA = 1;
    private static final String IMAGE_ID_DATAHOLDER = "1";
    private static final String BUNDLE_SAVED_BITMAPS = "bitmaps";
    private static final String DIR_NAME_FOR_IMAGE = "Images";


    private ArrayList<Bitmap> mBitmaps;
    private View mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mView = findViewById(R.id.coordinatorView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        if (savedInstanceState != null) {
            mBitmaps = (ArrayList<Bitmap>) WeakDataHolder.getInstance().getData(IMAGE_ID_DATAHOLDER);
        } else {
            mBitmaps = new ArrayList<>();
        }

        FloatingActionButton fabGallery = findViewById(R.id.fabGallery);
        fabGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        FloatingActionButton fabCamera = findViewById(R.id.fabCamera);
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
            }
        });
    }

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

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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

            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.CAMERA},   //Requesting the permission with the appropriate request code
                        REQUEST_CODE_ACTION_ADD_FROM_CAMERA);
            } else {
                //If the permission was already granted the first time it will run the method to open the gallery intent
                getPhotoFromCamera();
            }
        }
    }

/*
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }
*/

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
                Toast.makeText(getApplicationContext(), "Please grant permission to proceed", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_CODE_ACTION_ADD_FROM_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now user should be able to access the Camera
                getPhotoFromCamera();
            } else {
                Toast.makeText(getApplicationContext(), "Please grant permission to proceed", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap = null;
        if (Activity.RESULT_OK == resultCode) {
            if (REQUEST_CODE_ACTION_ADD_FROM_STORAGE == requestCode) {
                try {
                    InputStream stream = getContentResolver().openInputStream(
                            data.getData());
                    bitmap = BitmapFactory.decodeStream(stream);
                    stream.close();
                    o
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (REQUEST_CODE_ACTION_ADD_FROM_CAMERA == requestCode) {
                Bundle extras = data.getExtras();
                bitmap = (Bitmap) extras.get("data"); // Just a thumbnail, but works okay for this.
            }
        }
        if (bitmap != null) {
            addCard(bitmap);
            mGridView.smoothScrollToPosition(mBitmaps.size() - 1);

            // save the image to internal memory
            try {
                File path = new File(getApplicationContext().getFilesDir(), "Palette" + File.separator + DIR_NAME_FOR_IMAGE);
                if (!path.exists()) {
                    if (!path.mkdirs()) {
                        throw new IOException();
                    }
                }
                // use current time to name the picture
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssz");
                String imageName = sdf.format(new Date());

                Log.d(TAG, "onActivityResult() returned: " + sdf);

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
    }
}