package com.dariopellegrini.kdone.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OptionsEndpoint(val endpoint: String, val method: String, val parameters: List<OptionsParameter>?) {
    data class OptionsParameter(val name: String, val type: String, val optional: Boolean)
}