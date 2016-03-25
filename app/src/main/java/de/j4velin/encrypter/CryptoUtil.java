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

import android.content.Context;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;

public class CryptoUtil {

    public static void encrypt(final Context context, final EncryptCallback callback, final File plaintextFile) throws
            FileNotFoundException {
        java.io.File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            dir = context.getFilesDir();
        }
        java.io.File encryptedFile = new java.io.File(dir, plaintextFile.name + ".enc");
        Uri uri = Uri.fromFile(encryptedFile);
        final File resultFile =
                new File(plaintextFile.name, plaintextFile.mime, uri, plaintextFile.size);
        final OutputStream output = new FileOutputStream(encryptedFile);
        final InputStream input = context.getContentResolver().openInputStream(plaintextFile.uri);
        CipherUtil.getCipher(context, null, new CipherUtil.CipherResultCallback() {
            @Override
            public void cipherAvailable(final Cipher c) {
                try {
                    byte[] iv = c.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
                    output.write(iv.length);
                    output.write(iv);
                    CipherOutputStream outputStream = new CipherOutputStream(output, c);
                    new SaveTask(context, callback, resultFile).execute(
                            new SaveTask.Streams(input, outputStream));
                } catch (IOException | InvalidParameterSpecException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void decrypt(final Context context, final File encrypted, final OutputStream output) throws
            IOException {
        final InputStream input = context.getContentResolver().openInputStream(encrypted.uri);
        int ivLength = input.read();
        byte[] iv = new byte[ivLength];
        input.read(iv);
        CipherUtil.getCipher(context, iv, new CipherUtil.CipherResultCallback() {
            @Override
            public void cipherAvailable(final Cipher c) {
                CipherInputStream inputStream = new CipherInputStream(input, c);
                new SaveTask(context, null, encrypted)
                        .execute(new SaveTask.Streams(inputStream, output));
            }
        });
    }
}
