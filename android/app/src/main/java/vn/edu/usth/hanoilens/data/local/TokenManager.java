package vn.edu.usth.hanoilens.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class TokenManager {
    private static final String TAG = "TokenManager";
    private static final String PREF_NAME = "hanoilens_secure_prefs";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private static volatile TokenManager instance;
    private SharedPreferences securePrefs;
    private String memoryAccessToken = null;

    private TokenManager(Context context) {
        initSecurePrefs(context);
    }

    private void initSecurePrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePrefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception initialException) {
            Log.w(TAG, "Failed to initialize EncryptedSharedPreferences, resetting corrupted prefs...", initialException);
            try {
                // If keys mismatch or file corrupted, clean and re-create once
                context.deleteSharedPreferences(PREF_NAME);
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                securePrefs = EncryptedSharedPreferences.create(
                        context,
                        PREF_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (Exception fallbackException) {
                Log.e(TAG, "Fatal: unable to initialize secure preferences fallback", fallbackException);
                securePrefs = null;
            }
        }
    }

    public static TokenManager getInstance(Context context) {
        if (instance == null) {
            synchronized (TokenManager.class) {
                if (instance == null) {
                    instance = new TokenManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public synchronized void saveTokens(String accessToken, String refreshToken) {
        this.memoryAccessToken = accessToken;
        if (securePrefs != null && refreshToken != null) {
            securePrefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply();
        }
    }

    public synchronized String getAccessToken() {
        return memoryAccessToken;
    }

    public synchronized String getRefreshToken() {
        if (securePrefs != null) {
            return securePrefs.getString(KEY_REFRESH_TOKEN, null);
        }
        return null;
    }

    public synchronized void clearTokens() {
        memoryAccessToken = null;
        if (securePrefs != null) {
            securePrefs.edit().clear().apply();
        }
    }
}
