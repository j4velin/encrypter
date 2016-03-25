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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Background task to save the encrypted/decrypted file to the file system
 */
public class SaveTask extends AsyncTask<SaveTask.Streams, Integer, Void> {

    private final ProgressDialog dialog;
    private final static int UPDATE_PERCENT = 5;

    private final File resultFile;
    private final EncryptCallback callback;

    public SaveTask(final Context context, final EncryptCallback callback, final File resultFile) {
        this.resultFile = resultFile;
        this.callback = callback;
        dialog = new ProgressDialog(context);
        dialog.setCancelable(false);
        dialog.setProgressStyle(resultFile.size > 0 ? ProgressDialog.STYLE_HORIZONTAL :
                ProgressDialog.STYLE_SPINNER);
        dialog.setMax(resultFile.size);
        dialog.setProgressNumberFormat("%1d/%2d Bytes");
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        dialog.dismiss();
        if (callback != null) {
            callback.encryptionComplete(resultFile);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        dialog.setProgress(values[0]);
    }

    @Override
    protected Void doInBackground(final Streams... parameters) {
        int bytesRead = 0;
        int max = resultFile.size;
        int percent = (int) (max / (100f / UPDATE_PERCENT));
        InputStream in = parameters[0].input;
        OutputStream out = parameters[0].output;
        int read;
        try {
            while ((read = in.read()) != -1) {
                out.write(read);
                if (max > 0) {
                    bytesRead++;
                    if (bytesRead % percent == 0) {
                        publishProgress(bytesRead);
                    }
                }
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static class Streams {
        private final InputStream input;
        private final OutputStream output;

        public Streams(final InputStream input, final OutputStream output) {
            this.input = input;
            this.output = output;
        }
    }
}
