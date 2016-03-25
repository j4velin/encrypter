package de.j4velin.encrypter;

import android.content.Context;

public interface EncryptCallback {
    Context getContext();

    void encryptionComplete(final File encryptedFile);
}
