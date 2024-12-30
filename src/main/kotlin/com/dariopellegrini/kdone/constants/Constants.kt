package com.dariopellegrini.kdone.constants

const val usersTokensCollection = "users_token"
const val usersConfirmationsCollection = "users_confirmations"
const val passwordsRecoveryCollection = "passwords_recovery"
const val otpTokensCollection = "otp_tokens"
const val queryParameter = "_query"
const val sortParameter = "_sort"
const val descendParameter = "_descend"
const val limitParameter = "_limit"
const val skipParameter = "_skip"
const val lookupParameter = "_lookup"
val forbiddenUserParameters = listOf("confirmed", "facebookId", "appleId", "googleId", "otp")
const val ignoreSoftDeletedParameter = "_ignoreSoftDeleted"
const val forceDeleteParameter = "_forceDelete"