package de.j4velin.encrypter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_INPUT = 1;
    private final static int REQUEST_OUTPUT = 2;
    private InputStream input;
    private OutputStream output;
    private String inputName;
    private int inputSize;
    private final CipherUtil util = new CipherUtil();
    private final SaveTask saveTask = new SaveTask(this);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        util.init();

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
                        intent.putExtra(Intent.EXTRA_TITLE, inputName);
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
                                public void onClick(final DialogInterface dialogInterface,
                                                    int button) {
                                    byte[] iv = util.getNewIV();
                                    try {
                                        output.write(iv);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    util.getCipher(MainActivity.this, true, iv,
                                            new CipherUtil.CipherResultCallback() {
                                                @Override
                                                public void cipherAvailable(final Cipher c) {
                                                    CipherOutputStream outputStream =
                                                            new CipherOutputStream(output, c);
                                                    saveTask.execute(new SaveTask.Parameters(input,
                                                            outputStream, inputSize));
                                                }
                                            });
                                }
                            }).setNeutralButton("Decrypt", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialogInterface, int button) {
                            byte[] iv = new byte[CipherUtil.IV_LENGTH];
                            try {
                                input.read(iv);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            util.getCipher(MainActivity.this, false, iv,
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
