package com.cout970

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.log
import io.ktor.sessions.get
import io.ktor.sessions.sessions

fun Application.assertAccess(call: ApplicationCall) {
    if (!checkAccess(call)) {
        log.warn("(IP: ${call.request.local.remoteHost}) Attempt to access with invalid key: ${call.sessions.get<SessionKey>()}")
        throw AuthenticationException()
    }
}

fun checkAccess(call: ApplicationCall): Boolean {
    val session = call.sessions.get<SessionKey>() ?: SessionKey("")
    return session.key == System.getenv("ktor_private_key")
}