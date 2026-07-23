package com.cozypomo.app.data.auth

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object AuthPreferencesKeys {
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
    val ACCOUNT_ID = stringPreferencesKey("account_id")
}
