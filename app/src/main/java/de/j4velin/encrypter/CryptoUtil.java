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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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

/**
 * Utility class to deal with encryption and decryption
 */
public class CryptoUtil {

    private CryptoUtil() {
    }

    /**
     * Encrypts the given file
     *
     * @param context       the context
     * @param callback      callback to be called once the encryption is complete
     * @param plaintextFile the plaintext file
     * @throws FileNotFoundException
     */
    public static void encrypt(final Context context, final CryptoCallback callback,
                               final File plaintextFile) throws FileNotFoundException {
        java.io.File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            dir = context.getFilesDir();
        }
        java.io.File encryptedFile = new java.io.File(dir, plaintextFile.name + ".enc");
        int tries = 2;
        while (encryptedFile.exists()) {
            encryptedFile = new java.io.File(dir, plaintextFile.name + "_" + tries + ".enc");
            tries++;
        }
        Uri uri = Uri.fromFile(encryptedFile);
        final File resultFile =
                new File(-1, plaintextFile.name, plaintextFile.mime, uri, plaintextFile.size, true);
        final OutputStream output = new BufferedOutputStream(new FileOutputStream(encryptedFile));
        final InputStream input = new BufferedInputStream(
                context.getContentResolver().openInputStream(plaintextFile.uri));
        CipherUtil.getCipher(context, null, new CipherUtil.CipherResultCallback() {
            @Override
            public void cipherAvailable(final Cipher c) {
                try {
                    byte[] iv = c.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
                    output.write(iv.length);
                    output.write(iv);
                    CipherOutputStream outputStream = new CipherOutputStream(output, c);
                    new SaveTask(context, callback, resultFile)
                            .execute(new SaveTask.Streams(input, outputStream));
                } catch (IOException | InvalidParameterSpecException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Decrypts the given file
     *
     * @param context       the context
     * @param callback      callback to be called once the decryption is complete
     * @param encryptedFile the encrypted file
     * @param out           the output uri to write the plaintext file to
     * @throws IOException
     */
    public static void decrypt(final Context context, final CryptoCallback callback,
                               final File encryptedFile, final Uri out) throws IOException {
        final InputStream input = new BufferedInputStream(
                context.getContentResolver().openInputStream(encryptedFile.uri));
        final OutputStream output =
                new BufferedOutputStream(context.getContentResolver().openOutputStream(out));
        int ivLength = input.read();
        byte[] iv = new byte[ivLength];
        input.read(iv);
        final File resultFile =
                new File(-1, encryptedFile.name, encryptedFile.mime, out, encryptedFile.size,
                        false);
        CipherUtil.getCipher(context, iv, new CipherUtil.CipherResultCallback() {
            @Override
            public void cipherAvailable(final Cipher c) {
                CipherInputStream inputStream = new CipherInputStream(input, c);
                new SaveTask(context, callback, resultFile)
                        .execute(new SaveTask.Streams(inputStream, output));
            }
        });
    }
}
