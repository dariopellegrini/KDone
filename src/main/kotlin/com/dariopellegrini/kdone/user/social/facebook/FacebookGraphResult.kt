package com.dariopellegrini.kdone.user.social.facebook

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

class FacebookGraphResult {
    lateinit var data: FacebookGraphResultData
}

@JsonIgnoreProperties(ignoreUnknown = true)
class FacebookGraphResultData(var app_id: String?,
                              var type: String?,
                              var application: String?,
                              var is_valid: Boolean,
                              var user_id: String?,
                              val error: FacebookGraphError? = null)

@JsonIgnoreProperties(ignoreUnknown=true)
class FacebookGraphError(val code: Int, val message: String)