package com.cout970

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.request.receiveParameters
import io.ktor.request.uri
import io.ktor.response.respondFile
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngineEnvironment
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.slf4j.event.Level
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

data class SessionKey(val key: String)

class AuthenticationException : RuntimeException()

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    Database.connect(
        "jdbc:mysql://localhost:3306/web",
        user = System.getenv("ktor_db_user"),
        password = System.getenv("ktor_db_pass"),
        driver = "com.mysql.cj.jdbc.Driver"
    )

    transaction {
        SchemaUtils.create(BloquedIPs)
        SchemaUtils.create(LoginAttempts)
    }

    install()

    routing {
        static("/static") {
            files("resources/static")
        }

        get("/") {
            call.respondText(includeWrapperTemplate("page.html", "index.html"), contentType = ContentType.Text.Html)
        }

        get("/projects") {
            call.respondText(includeWrapperTemplate("page.html", "projects.html"), contentType = ContentType.Text.Html)
        }

        get("/notebook") {
            call.respondText(includeWrapperTemplate("page.html", "notebook.html"), contentType = ContentType.Text.Html)
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

        get("/login") {
            call.respondText(
                includeWrapperTemplate("page.html", "login.html", env("")),
                contentType = ContentType.Text.Html
            )
        }
        get("/wp/login") {
            call.respondText(
                includeWrapperTemplate("page.html", "login.html", env("")),
                contentType = ContentType.Text.Html
            )
        }

        post("/login") {
            val params = call.receiveParameters()

            LoginAttempts.registerAttempt(
                call.request.local.remoteHost,
                params["username"] ?: "",
                params["password"] ?: "",
                params["extra"] ?: ""
            )

            val msg = """<h3 class="error">Invalid credentials</h3>"""
            call.respondText(
                includeWrapperTemplate("page.html", "login.html", env(msg)),
                contentType = ContentType.Text.Html
            )
        }
        get("/admin_page") {
            assertAccess(call)
            call.respondText(includeWrapperTemplate("page.html", "admin.html"), contentType = ContentType.Text.Html)
        }

        get("/blocked-ips") {
            assertAccess(call)
            call.respondText(
                includeWrapperTemplate("page.html", "table.html", blockedIpsPage()),
                contentType = ContentType.Text.Html
            )
        }
        get("/login-attempts") {
            assertAccess(call)
            call.respondText(
                includeWrapperTemplate("page.html", "table.html", loginAttemptsPage()),
                contentType = ContentType.Text.Html
            )
        }

        get("/voidpixel") {
            call.respondText(
                "<html><meta http-equiv=\"refresh\" content=\"0; url=https://www.youtube.com/watch?v=dQw4w9WgXcQ\"></html>",
                contentType = ContentType.Text.Html
            )
        }

        get("/log") {
            call.respondFile(File("/var/log/web.log"))
        }

        get("/log/clear") {
            val src = File("/var/log/web.log")
            val dst = File("/var/log/web.old.log")
            dst.appendBytes(src.readBytes())
            src.writeText("")
            call.respondText("Log cleared")
        }

        get("/run/restart") {
            assertAccess(call)
            this@module.log.info("Stopping server...")

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
        exception<IOException> { cause ->
            log.error("IOException: ${cause.message}")
            call.respondText(
                includeWrapperTemplate("page.html", "error.html", mapOf("msg" to "Internal error")),
                contentType = ContentType.Text.Html,
                status = HttpStatusCode.InternalServerError
            )
        }

        status(HttpStatusCode.NotFound) {
            // Throttle down the petitions to missing pages
            delay(1000)
            if (call.request.uri.endsWith(".php")) {
                val blockedIP = call.request.local.remoteHost

                log.info("Banning IP: $blockedIP")
                if (BloquedIPs.isIpBloqued(blockedIP)) {
                    transaction {
                        BloquedIPs.insert { query ->
                            query[ip] = blockedIP
                            query[created] = DateTime.now()
                            query[redirects] = 0
                        }
                    }
                }

                call.redirectRandom()
            } else {
                call.respondText(
                    includeWrapperTemplate("page.html", "error.html", mapOf("msg" to "Not found")),
                    contentType = ContentType.Text.Html,
                    status = HttpStatusCode.NotFound
                )
            }
        }
    }
}

suspend fun ApplicationCall.redirectRandom() {
    // Retrieve a random blocked IP to redirect to.
    val redirectIP = transaction {
        val row = BloquedIPs.selectAll()
            .map { it to Math.random() }
            .sortedBy { it.second }
            .first()
            .first

        // Update the redirect count
        BloquedIPs.update({ BloquedIPs.ip eq row[BloquedIPs.ip] }) {
            it[BloquedIPs.redirects] = row[BloquedIPs.redirects] + 1
        }

        row[BloquedIPs.ip]
    }
    respondRedirect("http://$redirectIP", true)
}

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)