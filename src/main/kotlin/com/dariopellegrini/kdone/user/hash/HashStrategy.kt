package com.dariopellegrini.kdone.user.hash

data class HashStrategy(val hash: (String) -> String, val verify: (String, String) -> Boolean)