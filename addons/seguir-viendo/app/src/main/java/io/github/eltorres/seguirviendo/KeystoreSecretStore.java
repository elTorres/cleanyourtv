package io.github.eltorres.seguirviendo;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class KeystoreSecretStore implements SecretStore {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "seguir-viendo-secrets";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SharedPreferences prefs;

    KeystoreSecretStore(Context context) {
        prefs = context.getSharedPreferences("secrets", Context.MODE_PRIVATE);
    }

    @Override
    public synchronized void put(String key, String value) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] iv = cipher.getIV();
            byte[] sealed = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] blob = ByteBuffer.allocate(iv.length + sealed.length).put(iv).put(sealed).array();
            prefs.edit().putString(key, Base64.encodeToString(blob, Base64.NO_WRAP)).apply();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("keystore unavailable", e);
        }
    }

    @Override
    public synchronized String get(String key) {
        String stored = prefs.getString(key, null);
        if (stored == null) return null;
        try {
            byte[] blob = Base64.decode(stored, Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES));
            return new String(cipher.doFinal(blob, IV_BYTES, blob.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            prefs.edit().remove(key).apply();
            return null;
        }
    }

    @Override
    public synchronized void remove(String key) {
        prefs.edit().remove(key).apply();
    }

    @Override
    public synchronized Collection<String> values() {
        Collection<String> values = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String value = get(entry.getKey());
            if (value != null) values.add(value);
        }
        return values;
    }

    private static SecretKey key() throws GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        try {
            keyStore.load(null);
        } catch (java.io.IOException e) {
            throw new GeneralSecurityException(e);
        }
        if (keyStore.containsAlias(ALIAS)) return ((KeyStore.SecretKeyEntry) keyStore.getEntry(ALIAS, null)).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
