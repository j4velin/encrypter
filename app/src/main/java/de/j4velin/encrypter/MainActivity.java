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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "Encrypter";

    public final static String CRYPTO_COMPLETE_ACTION = "crypto_complete";
    public final static String EXTRA_RESULT_FILE = "result_file";
    public final static String EXTRA_ORIGINAL_FILE = "original_file";

    private final static int REQUEST_INPUT = 1;
    private final static int REQUEST_PERMISSION = 2;

    private CoordinatorLayout coordinatorLayout;
    private View plaintextHeadline, plaintextView;

    private enum Requirement {
        FINGERPRINT_PERMISSION,
        FINGERPRINT_SENSOR,
        FINGERPRINT_SETUP,
        DEVICE_SECURE
    }

    private final BroadcastReceiver cryptoCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(CRYPTO_COMPLETE_ACTION)) {
                final File resultFile = intent.getParcelableExtra(EXTRA_RESULT_FILE);
                if (!resultFile.isEncrypted) {
                    Snackbar.make(coordinatorLayout,
                            getString(R.string.file_decrypted, resultFile.name),
                            Snackbar.LENGTH_LONG)
                            .setActionTextColor(getResources().getColor(R.color.colorPrimary, null))
                            .setAction(R.string.open_file, new View.OnClickListener() {
                                @Override
                                public void onClick(final View view) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(resultFile.uri, resultFile.mime);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(intent);
                                }
                            }).show();
                }
            }
        }
    };

    CoordinatorLayout getCoordinatorLayout() {
        return coordinatorLayout;
    }

    void showPlaintextLayout(boolean show) {
        plaintextHeadline.setVisibility(show ? View.VISIBLE : View.GONE);
        plaintextView.setVisibility(show ? View.VISIBLE : View.GONE);
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
                if (CipherUtil.init()) {
                    new AlertDialog.Builder(this).setTitle(R.string.key_generated)
                            .setMessage(R.string.new_key_warning)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(final DialogInterface dialogInterface,
                                                            int i) {
                                            dialogInterface.dismiss();
                                        }
                                    }).create().show();
                }
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
                builder.setMessage(getString(R.string.error_load_key, exception));
            } else {
                switch (error) {
                    case FINGERPRINT_SENSOR:
                        builder.setMessage(R.string.error_no_fingerprint_hardware);
                        break;
                    case FINGERPRINT_SETUP:
                        builder.setMessage(R.string.error_no_fingerprints);
                        builder.setPositiveButton(R.string.register_finger,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialogInterface,
                                                        int i) {
                                        startActivity(
                                                new Intent(Settings.ACTION_SECURITY_SETTINGS));
                                        dialogInterface.cancel();
                                    }
                                });
                        break;
                    case DEVICE_SECURE:
                        builder.setMessage(R.string.error_no_lockscreen);
                        builder.setPositiveButton(R.string.setup_lockscreen,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialogInterface,
                                                        int i) {
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
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
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
        plaintextHeadline = findViewById(R.id.plainheadline);
        plaintextView = findViewById(R.id.plain);
        showPlaintextLayout(false);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(cryptoCompleteReceiver, new IntentFilter(MainActivity.CRYPTO_COMPLETE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(cryptoCompleteReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull final String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == REQUEST_INPUT && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String inputName = null;
            int inputSize = -1;
            String inputType = getContentResolver().getType(uri);
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    inputName =
                            cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (!cursor.isNull(sizeIndex)) {
                        inputSize = cursor.getInt(sizeIndex);
                    }
                }
            }
            File input = new File(-1, inputName, inputType, uri, inputSize, false);
            try {
                CryptoUtil.encrypt(MainActivity.this, input);
            } catch (GeneralSecurityException e) {
                Snackbar.make(coordinatorLayout, getString(R.string.error_security, e.getMessage()),
                        Snackbar.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                Snackbar.make(coordinatorLayout, R.string.error_file_not_found,
                        Snackbar.LENGTH_LONG).show();
            } catch (IOException e) {
                Snackbar.make(coordinatorLayout, getString(R.string.error_io, e.getMessage()),
                        Snackbar.LENGTH_LONG).show();
            }
        } else

        {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }
}
