package com.cout970

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respondFile
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import kotlinx.coroutines.delay
import org.slf4j.event.Level
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

data class SessionKey(val key: String)

class AuthenticationException : RuntimeException()

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install()

    routing {
        static("/static") {
            files("resources/static")
        }

        get("/") {
            call.respondText(includeWrapperTemplate("page.html", "index.html"), contentType = ContentType.Text.Html)
        }
        get("/full-editor") {
            call.respondText(includeWrapperTemplate("page.html", "full_editor.html"), contentType = ContentType.Text.Html)
        }
        get("/js-editor") {
            call.respondText(includeWrapperTemplate("page.html", "js_editor.html"), contentType = ContentType.Text.Html)
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
            call.respondText(includeWrapperTemplate("page.html", "admin.html"), contentType = ContentType.Text.Html)
        }

        get("/log") {
            call.respondFile(File("/var/log/web.log"))
        }

        get("/run/restart") {
            assertAccess(call)
            this@module.log.info("Stopping server...")
            call.respondText("Restating...", status = HttpStatusCode.Gone)

            delay(1000)
            environment.monitor.raise(ApplicationStopPreparing, environment)

            if (environment is ApplicationEngineEnvironment) {
                (environment as ApplicationEngineEnvironment).stop()
            } else {
                application.dispose()
            }

            delay(1000)
            exitProcess(0)
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
        header("X-Engine", "Ktor")
    }

    install(ContentNegotiation) {
        gson()
    }

    install(StatusPages) {
        exception<AuthenticationException> {
            call.respondText(
                includeWrapperTemplate("page.html", "error.html", mapOf("msg" to "No authorized")),
                contentType = ContentType.Text.Html,
                status = HttpStatusCode.Unauthorized
            )
        }
        exception<IOException> {
            call.respondText(
                includeWrapperTemplate("page.html", "error.html", mapOf("msg" to "Internal error")),
                contentType = ContentType.Text.Html,
                status = HttpStatusCode.InternalServerError
            )
        }

        status(HttpStatusCode.NotFound) {
            // Throttle down the petitions to missing pages
            delay(1000)
            call.respondText(
                includeWrapperTemplate("page.html", "error.html", mapOf("msg" to "Not found")),
                contentType = ContentType.Text.Html,
                status = HttpStatusCode.NotFound
            )
        }
    }
}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)