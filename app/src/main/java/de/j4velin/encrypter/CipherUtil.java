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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Utility class to deal with the cryptographic ciphers.
 *
 * @see <a href="https://github.com/googlesamples/android-FingerprintDialog">Based on Google's FingerprintSample</a>
 */
class CipherUtil {

    private CipherUtil() {
    }

    /**
     * Alias for our key in the Android Key Store
     */
    private static final String KEY_NAME = "my_key";

    private static KeyStore mKeyStore;
    private static Cipher encrypt;
    private static Cipher decrypt;
    private static KeyGenerator mKeyGenerator;

    /**
     * Initializes the keystore and the ciphers and creates the key if necessary
     *
     * @throws GeneralSecurityException
     * @throws IOException
     */
    static void init() throws GeneralSecurityException, IOException {
        mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        mKeyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        encrypt = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" +
                        KeyProperties.ENCRYPTION_PADDING_PKCS7);
        decrypt = Cipher.getInstance(
                KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" +
                        KeyProperties.ENCRYPTION_PADDING_PKCS7);
        if (!hasKey()) {
            createKey();
        }
    }


    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     */
    private static void createKey() throws CertificateException, NoSuchAlgorithmException,
            IOException, InvalidAlgorithmParameterException {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        mKeyStore.load(null);
        // Set the alias of the entry in Android KeyStore where the key will appear
        // and the constrains (purposes) in the constructor of the Builder
        mKeyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(
                KeyProperties.BLOCK_MODE_CBC)
                // Require the user to authenticate with a fingerprint to authorize every use
                // of the key
                .setUserAuthenticationRequired(true)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7).build());
        mKeyGenerator.generateKey();
    }

    /**
     * Checks if a key has already been create
     *
     * @return true, if a key is already created
     */
    private static boolean hasKey() {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            return key != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Request a cipher
     *
     * @param context  the context
     * @param iv       the initialization vector for CBC mode or null, to request encryption cipher
     * @param callback the callback which will be notified once the cipher is ready
     */
    static void getCipher(final Context context, final byte[] iv,
                          final CipherResultCallback callback) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            if (iv == null) {
                encrypt.init(Cipher.ENCRYPT_MODE, key);
            } else {
                decrypt.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            }
        } catch (InvalidAlgorithmParameterException | KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to init Decipher", e);
        }
        auth(iv == null ? encrypt : decrypt, context, callback);
    }

    private static void auth(final Cipher c, final Context context,
                             final CipherResultCallback callback) {
        FingerprintManager.CryptoObject mCryptoObject = new FingerprintManager.CryptoObject(c);
        final CancellationSignal mCancellationSignal = new CancellationSignal();
        final Dialog dialog = new AlertDialog.Builder(context).setView(R.layout.fingerprint_dialog)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface dialogInterface) {
                        mCancellationSignal.cancel();
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, int which) {
                        dialogInterface.cancel();
                    }
                }).create();
        dialog.show();
        //noinspection ResourceType
        ((FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE))
                .authenticate(mCryptoObject, mCancellationSignal, 0 /* flags */,
                        new FingerprintManager.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(
                                    FingerprintManager.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                dialog.dismiss();
                                callback.cipherAvailable(c);
                            }
                        }, null);
    }

    interface CipherResultCallback {
        /**
         * A cipher is now ready for use
         *
         * @param c the cipher
         */
        void cipherAvailable(final Cipher c);
    }
}
