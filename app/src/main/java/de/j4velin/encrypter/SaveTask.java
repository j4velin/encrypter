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
 * Background task to save the isEncrypted/decrypted file to the file system
 */
class SaveTask extends AsyncTask<SaveTask.Streams, Integer, Void> {

    private final ProgressDialog dialog;
    private final static int UPDATE_PERCENT = 5;
    private final static int BUFFER_SIZE = 8192;

    private final Context context;
    private final File resultFile;
    private final CryptoCallback callback;

    SaveTask(final Context context, final CryptoCallback callback, final File resultFile) {
        this.context = context;
        this.resultFile = resultFile;
        this.callback = callback;
        dialog = new ProgressDialog(context);
        dialog.setCancelable(false);
        dialog.setProgressStyle(resultFile.size > 0 ? ProgressDialog.STYLE_HORIZONTAL :
                ProgressDialog.STYLE_SPINNER);
        dialog.setMax(resultFile.size);
        dialog.setProgressNumberFormat("%1$,d / %2$,d Bytes");
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
        Database db = new Database(context);
        resultFile.id = db.addFile(resultFile);
        db.close();
        if (callback != null) {
            callback.operationComplete(resultFile);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        dialog.setProgress(values[0]);
    }

    @Override
    protected Void doInBackground(final Streams... parameters) {
        int bytesRead = 0;
        int percentage = (int) (resultFile.size * (UPDATE_PERCENT / 100f));
        InputStream in = parameters[0].input;
        OutputStream out = parameters[0].output;
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        int nextUpdate = percentage;
        try {
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
                bytesRead += read;
                if (bytesRead > nextUpdate) {
                    publishProgress(bytesRead);
                    nextUpdate = bytesRead + percentage;
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

    static class Streams {
        private final InputStream input;
        private final OutputStream output;

        Streams(final InputStream input, final OutputStream output) {
            this.input = input;
            this.output = output;
        }
    }
}
