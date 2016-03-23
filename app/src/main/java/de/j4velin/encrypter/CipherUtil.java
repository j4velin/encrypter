package de.j4velin.encrypter;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.widget.Toast;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class CipherUtil {

    public final static int IV_LENGTH = 16;
    private final static SecureRandom random = new SecureRandom();

    /**
     * Alias for our key in the Android Key Store
     */
    private static final String KEY_NAME = "my_key";

    private KeyStore mKeyStore;
    private Cipher encrypt;
    private Cipher decrypt;
    private KeyGenerator mKeyGenerator;

    public byte[] getNewIV() {
        byte[] re = new byte[IV_LENGTH];
        random.nextBytes(re);
        return re;
    }

    void init() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        try {
            mKeyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            encrypt = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" +
                            KeyProperties.ENCRYPTION_PADDING_PKCS7);
            decrypt = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" +
                            KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        if (!hasKey()) {
            createKey();
        }
    }


    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with fingerprint.
     */
    private void createKey() {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        try {
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
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasKey() {
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
     * Initialize the {@link Cipher} instance with the created key in the {@link #createKey()}
     * method.
     *
     * @return {@code true} if initialization is successful, {@code false} if the lock screen has
     * been disabled or reset after the key was generated, or if a fingerprint got enrolled after
     * the key was generated.
     */
    void getCipher(final Context context, boolean doEncrypt, byte[] iv,
                   final CipherResultCallback callback) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            if (doEncrypt) {
                encrypt.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            } else {
                decrypt.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            }
        } catch (InvalidAlgorithmParameterException | KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to init Decipher", e);
        }
        auth(doEncrypt ? this.encrypt : decrypt, context, callback);
    }

    private void auth(final Cipher c, final Context context, final CipherResultCallback callback) {
        FingerprintManager.CryptoObject mCryptoObject = new FingerprintManager.CryptoObject(c);
        CancellationSignal mCancellationSignal = new CancellationSignal();
        Toast.makeText(context, "Place finger", Toast.LENGTH_SHORT).show();
        //noinspection ResourceType
        ((FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE))
                .authenticate(mCryptoObject, mCancellationSignal, 0 /* flags */,
                        new FingerprintManager.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(
                                    FingerprintManager.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                callback.cipherAvailable(c);
                            }
                        }, null);
    }

    interface CipherResultCallback {
        void cipherAvailable(final Cipher c);
    }
}
