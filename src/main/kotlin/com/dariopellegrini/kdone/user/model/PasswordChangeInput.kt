package com.dariopellegrini.kdone.user.model

class PasswordChangeInput(val currentPassword: String,
                          val newPassword: String,
                          val invalidateOtherSessions: Boolean? = null)