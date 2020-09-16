package com.dariopellegrini.kdone.privacy.model

import com.dariopellegrini.kdone.model.DateModel
import com.dariopellegrini.kdone.model.Identifiable
import com.dariopellegrini.kdone.user.model.KDoneUser
import org.litote.kmongo.Id
import java.util.*

data class UserPrivacy(val userId: Id<KDoneUser>,
                       var preferences: List<UserPrivacyPreference>,
                       override var dateCreated: Date = Date(),
                       override var dateUpdated: Date = Date()): Identifiable(), DateModel

data class UserPrivacyPreference(val key: String,
                                 var accepted: Boolean?)