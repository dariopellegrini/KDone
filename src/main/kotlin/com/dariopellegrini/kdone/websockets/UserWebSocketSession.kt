package com.dariopellegrini.kdone.websockets

import com.dariopellegrini.kdone.auth.UserAuth
import io.ktor.websocket.*

class UserWebSocketSession(val connection: DefaultWebSocketSession, val userAuth: UserAuth?)