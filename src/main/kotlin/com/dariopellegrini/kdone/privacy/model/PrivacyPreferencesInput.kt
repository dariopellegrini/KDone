package com.dariopellegrini.kdone.privacy.model

data class PrivacyPreferencesInput(val preferences: List<PrivacyPreferenceInput>)
data class PrivacyPreferenceInput(val key: String, val accepted: Boolean?)