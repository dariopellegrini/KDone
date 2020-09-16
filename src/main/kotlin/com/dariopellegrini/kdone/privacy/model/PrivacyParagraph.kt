package com.dariopellegrini.kdone.privacy.model

data class PrivacyParagraph(val title: String,
                            val content: String,
                            val key: String,
                            var selectable: Boolean = false, // This paragraph is selectable. Otherwise is only a readable paragraph.
                            var mustBeAccepted: Boolean = false, // If selectable is true paragraph must be accepted by user
                            var mustBeAnswered: Boolean = true) // if selectable is true paragraph needs a response, either accepted or denied

fun paragraph(title: String,
              content: String,
              key: String,
              init: (PrivacyParagraph.() -> Unit)? = null): PrivacyParagraph {
    val privacyParagraph = PrivacyParagraph(title, content, key)
    if (init != null) {
        privacyParagraph.init()
    }
    return privacyParagraph
}