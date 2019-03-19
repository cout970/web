package com.cout970

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.LocalFileContent
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ShutDownUrl
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import org.slf4j.event.Level
import java.io.File

data class SessionKey(val key: String)

class AuthenticationException : RuntimeException()

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install()

    routing {
        static("/static") {
            resources("static")
        }

        get("/") {
            call.respond(
                LocalFileContent(
                    File("resources/templates/index.html"),
                    contentType = ContentType.Text.Html
                )
            )
        }

        get("/session") {
            call.sessions.set(SessionKey(call.parameters["key"] ?: ""))
            assertAccess(call)
            call.respondText(
                """
                    Login successfully
                    <a href="/admin_page">Admin page</a>
                    """.trimIndent(),
                contentType = ContentType.Text.Html
            )
        }

        get("/admin_page") {
            assertAccess(call)
            call.respondFile(File("resources/templates/admin.html"))
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world 2"))
        }

        get("/run/update") {
            assertAccess(call)
            this@module.log.info("Starting update checks")
            call.respondText(updateServer(), contentType = ContentType.Text.Plain)
            this@module.log.info("Update done")
        }

        get("/run/restart") {
            assertAccess(call)
            ShutDownUrl("") { 1 }.doShutdown(call)
        }
    }

}

fun Application.install() {
    install(Sessions) {
        cookie<SessionKey>("SESSION_KEY")
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
    }

    install(AutoHeadResponse)

    install(CallLogging) {
        level = Level.DEBUG
    }

    install(ConditionalHeaders)

    install(DataConversion)

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(ContentNegotiation) {
        gson()
    }

    install(StatusPages) {
        exception<AuthenticationException> {
            call.respondText(
                "No authorized",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.Unauthorized
            )
        }
    }
}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)