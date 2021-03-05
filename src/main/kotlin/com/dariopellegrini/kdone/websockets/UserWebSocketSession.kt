package com.dariopellegrini.kdone.websockets

import com.dariopellegrini.kdone.auth.UserAuth
import io.ktor.http.cio.websocket.*

class UserWebSocketSession(val connection: DefaultWebSocketSession, val userAuth: UserAuth?)