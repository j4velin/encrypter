/*
 * Copyright 2016 Thomas Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.j4velin.encrypter;

import android.Manifest;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_INPUT = 1;
    private final static int REQUEST_OUTPUT = 2;
    private final static int REQUEST_PERMISSION = 3;
    private InputStream input;
    private OutputStream output;
    private String inputName;
    private int inputSize;
    private SaveTask saveTask;

    private enum Requirement {
        FINGERPRINT_PERMISSION,
        FINGERPRINT_SENSOR,
        FINGERPRINT_SETUP,
        DEVICE_SECURE
    }

    private Requirement getMissingRequirement() {
        FingerprintManager fingerprintManager =
                (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.USE_FINGERPRINT},
                    REQUEST_PERMISSION);
            return Requirement.FINGERPRINT_PERMISSION;
        } else if (!fingerprintManager.isHardwareDetected()) {
            return Requirement.FINGERPRINT_SENSOR;
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            return Requirement.FINGERPRINT_SETUP;
        } else if (!((KeyguardManager) getSystemService(KEYGUARD_SERVICE)).isDeviceSecure()) {
            return Requirement.DEVICE_SECURE;
        } else {
            return null;
        }
    }

    private void init() {
        Requirement error = getMissingRequirement();
        String exception = null;
        if (error == null) {
            try {
                CipherUtil.init();
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                exception = e.getMessage();
            }
        } else if (error.equals(Requirement.FINGERPRINT_PERMISSION)) {
            // ignore
            return;
        }
        if (error != null || exception != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (exception != null) {
                builder.setMessage("Error creating/loading cryptographic key: " + exception);
            } else {
                switch (error) {
                    case FINGERPRINT_SENSOR:
                        builder.setMessage("No fingerprint reader found");
                        break;
                    case FINGERPRINT_SETUP:
                        builder.setMessage("No fingerprints registered");
                        builder.setPositiveButton("Register finger",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialogInterface, int i) {
                                        startActivity(
                                                new Intent(Settings.ACTION_SECURITY_SETTINGS));
                                        dialogInterface.cancel();
                                    }
                                });
                    case DEVICE_SECURE:
                        builder.setMessage("No secure lockscreen found");
                        builder.setPositiveButton("Setup lockscreen",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialogInterface, int i) {
                                        startActivity(
                                                new Intent(Settings.ACTION_SECURITY_SETTINGS));
                                        dialogInterface.cancel();
                                    }
                                });
                        break;
                }
            }
            builder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(final DialogInterface dialogInterface) {
                    dialogInterface.dismiss();
                    finish();
                }
            }).create().show();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, REQUEST_INPUT);
            }
        });
        init();
        saveTask = new SaveTask(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, final String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            switch (requestCode) {
                case REQUEST_INPUT:
                    try {
                        input = getContentResolver().openInputStream(uri);
                        String inputType = getContentResolver().getType(uri);
                        // The query, since it only applies to a single document, will only return
                        // one row. There's no need to filter, sort, or select fields, since we want
                        // all fields for one document.
                        Cursor cursor =
                                getContentResolver().query(uri, null, null, null, null, null);

                        try {
                            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
                            // "if there's anything to look at, look at it" conditionals.
                            if (cursor != null && cursor.moveToFirst()) {
                                inputName = cursor.getString(
                                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                                if (!cursor.isNull(sizeIndex)) {
                                    inputSize = cursor.getInt(sizeIndex);
                                } else {
                                    // size is only required for the progress dialog
                                    inputSize = -1;
                                }
                            }
                        } finally {
                            cursor.close();
                        }
                        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType(inputType);
                        String outputName;
                        if (inputName.contains(".")) {
                            int index = inputName.lastIndexOf(".");
                            outputName = inputName.substring(0, index) + "_2" +
                                    inputName.substring(index);
                        } else {
                            outputName = inputName;
                        }
                        intent.putExtra(Intent.EXTRA_TITLE, outputName);
                        startActivityForResult(intent, REQUEST_OUTPUT);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;
                case REQUEST_OUTPUT:
                    try {
                        output = getContentResolver().openOutputStream(uri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    new AlertDialog.Builder(this)
                            .setPositiveButton("Encrypt", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialogInterface, int button) {
                                    CipherUtil.getCipher(MainActivity.this, null,
                                            new CipherUtil.CipherResultCallback() {
                                                @Override
                                                public void cipherAvailable(final Cipher c) {
                                                    try {
                                                        byte[] iv = c.getParameters()
                                                                .getParameterSpec(
                                                                        IvParameterSpec.class)
                                                                .getIV();
                                                        output.write(iv.length);
                                                        output.write(iv);
                                                        CipherOutputStream outputStream =
                                                                new CipherOutputStream(output, c);
                                                        saveTask.execute(
                                                                new SaveTask.Parameters(input,
                                                                        outputStream, inputSize));
                                                    } catch (IOException | InvalidParameterSpecException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            });
                                }
                            }).setNeutralButton("Decrypt", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, int button) {
                            try {
                                int ivLength = input.read();
                                byte[] iv = new byte[ivLength];
                                input.read(iv);
                                CipherUtil.getCipher(MainActivity.this, iv,
                                        new CipherUtil.CipherResultCallback() {
                                            @Override
                                            public void cipherAvailable(final Cipher c) {
                                                CipherInputStream inputStream =
                                                        new CipherInputStream(input, c);
                                                saveTask.execute(
                                                        new SaveTask.Parameters(inputStream, output,
                                                                inputSize));
                                            }
                                        });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).create().show();
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
