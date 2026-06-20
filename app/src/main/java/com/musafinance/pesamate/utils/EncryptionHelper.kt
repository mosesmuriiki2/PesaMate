package com.musafinance.pesamate.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object EncryptionHelper {
    
    private const val PREFS_NAME = "encrypted_pesamate_prefs"

    fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // For pass-through or simple obfuscation if needed for Firestore fields, 
    // although Firestore usually relies on rules and TLS.
    // If strict field-level encryption is needed, we could use standard AES here.
}
