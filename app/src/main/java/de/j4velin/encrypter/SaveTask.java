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

public class SaveTask extends AsyncTask<SaveTask.Parameters, Integer, Void> {

    private final ProgressDialog dialog;
    private final static int UPDATE_PERCENT = 5;

    public SaveTask(final Context c) {
        dialog = new ProgressDialog(c);
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(100);
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
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values[0] < 0) {
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        } else {
            dialog.incrementProgressBy(UPDATE_PERCENT);
        }
    }

    @Override
    protected Void doInBackground(final Parameters... parameters) {
        int bytesRead = 0;
        int max = parameters[0].size;
        int percent = (int) (max / (100f / UPDATE_PERCENT));
        if (max < 0) {
            publishProgress(-1);
        }
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

    public static class Parameters {
        private final InputStream input;
        private final OutputStream output;
        private final int size;

        public Parameters(final InputStream input, final OutputStream output, int size) {
            this.input = input;
            this.output = output;
            this.size = size;
        }
    }
}
