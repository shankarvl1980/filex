package svl.kadatha.filex.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * API 21+: AES key is generated randomly and stored wrapped by an RSA keypair in AndroidKeyStore.
 * Token strings are encrypted with AES-GCM and stored as Base64 TEXT:  version:iv:ciphertext
 */
public final class TokenCrypto {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String RSA_ALIAS = "filex_oauth_rsa_v1";

    private static final String PREFS = "filex_token_crypto";
    private static final String PREF_WRAPPED_AES = "wrapped_aes_v1";

    private static final int AES_BITS = 256;           // falls back if device blocks 256
    private static final int GCM_IV_BYTES = 12;        // recommended
    private static final int GCM_TAG_BITS = 128;

    private static final String ENC_VERSION = "v1";

    private TokenCrypto() {
    }

    // -------------------- Public API --------------------

    /**
     * Encrypts plaintext into Base64 TEXT payload. Returns null if input null/empty.
     */
    @Nullable
    public static String encrypt(@NonNull Context ctx, @Nullable String plaintext) throws Exception {
        if (plaintext == null || plaintext.isEmpty()) return null;

        SecretKey aes = getOrCreateAesKey(ctx);

        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, aes, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // payload = v1:base64(iv):base64(ciphertext)
        return ENC_VERSION + ":"
                + Base64.encodeToString(iv, Base64.NO_WRAP) + ":"
                + Base64.encodeToString(ct, Base64.NO_WRAP);
    }

    /**
     * Decrypts Base64 TEXT payload. Returns null if input null/empty.
     */
    @Nullable
    public static String decrypt(@NonNull Context ctx, @Nullable String payload) throws Exception {
        if (payload == null || payload.isEmpty()) return null;

        String[] parts = payload.split(":");
        if (parts.length != 3) {
            // Not encrypted (legacy plaintext) -> return as-is (so you can migrate)
            return payload;
        }
        if (!ENC_VERSION.equals(parts[0])) {
            throw new IllegalStateException("Unknown token payload version: " + parts[0]);
        }

        byte[] iv = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] ct = Base64.decode(parts[2], Base64.NO_WRAP);

        SecretKey aes = getOrCreateAesKey(ctx);

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, aes, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] pt = c.doFinal(ct);
        return new String(pt, StandardCharsets.UTF_8);
    }

    /**
     * If you ever want to force reset (logout all): deletes wrapped AES key.
     */
    public static void clear(@NonNull Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().remove(PREF_WRAPPED_AES).apply();
        // NOTE: we keep RSA keypair. You *can* delete it too, but not required.
    }

    // -------------------- Internals --------------------

    private static SecretKey getOrCreateAesKey(@NonNull Context ctx) throws Exception {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String wrappedB64 = sp.getString(PREF_WRAPPED_AES, null);

        ensureRsaKeypair(ctx);

        if (wrappedB64 == null) {
            SecretKey aes = generateAes();
            byte[] wrapped = rsaWrap(getRsaPublicKey(), aes.getEncoded());
            sp.edit().putString(PREF_WRAPPED_AES, Base64.encodeToString(wrapped, Base64.NO_WRAP)).apply();
            return aes;
        }

        byte[] wrapped = Base64.decode(wrappedB64, Base64.NO_WRAP);
        byte[] rawAes = rsaUnwrap(getRsaPrivateKey(), wrapped);
        return new SecretKeySpec(rawAes, "AES");
    }

    private static SecretKey generateAes() throws Exception {
        // Some old devices might not like 256-bit AES depending on providers; fall back to 128 if needed.
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(AES_BITS);
            return kg.generateKey();
        } catch (Throwable t) {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            return kg.generateKey();
        }
    }

    private static void ensureRsaKeypair(@NonNull Context ctx) throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
        ks.load(null);

        if (ks.containsAlias(RSA_ALIAS)) return;

        if (Build.VERSION.SDK_INT >= 23) {
            // API 23+ can use KeyGenParameterSpec, but we keep it simple and API21-friendly.
            // We'll still use the pre-23 spec approach to be consistent.
        }

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 30);

        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(ctx)
                .setAlias(RSA_ALIAS)
                .setSubject(new X500Principal("CN=" + RSA_ALIAS))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", ANDROID_KEY_STORE);
        kpg.initialize(spec);
        KeyPair kp = kpg.generateKeyPair();
        // kp not used directly; stored in keystore
    }

    private static PublicKey getRsaPublicKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
        ks.load(null);
        Certificate cert = ks.getCertificate(RSA_ALIAS);
        if (cert == null)
            throw new IllegalStateException("RSA cert missing for alias " + RSA_ALIAS);
        return cert.getPublicKey();
    }

    private static PrivateKey getRsaPrivateKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
        ks.load(null);
        KeyStore.Entry entry = ks.getEntry(RSA_ALIAS, null);
        if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
            throw new IllegalStateException("RSA private key missing for alias " + RSA_ALIAS);
        }
        return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
    }

    private static byte[] rsaWrap(@NonNull PublicKey pub, @NonNull byte[] data) throws Exception {
        // RSA/ECB/PKCS1Padding is the common API21-compatible wrap mode.
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.ENCRYPT_MODE, pub);
        return c.doFinal(data);
    }

    private static byte[] rsaUnwrap(@NonNull PrivateKey priv, @NonNull byte[] wrapped) throws Exception {
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.DECRYPT_MODE, priv);
        return c.doFinal(wrapped);
    }
}
