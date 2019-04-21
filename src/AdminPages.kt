package com.cout970

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun blockedIpsPage(): Map<String, String> {
    val headers = listOf("IP", "Redirects", "Created")
    val values = mutableListOf<List<String>>()

    transaction {
        BloquedIPs.selectAll().forEach { row ->
            values += listOf(
                row[BloquedIPs.ip].toString(),
                row[BloquedIPs.redirects].toString(),
                row[BloquedIPs.created].toString()
            )
        }
    }

    return mapOf("name" to "Blocked IPs", "table" to createTable(headers, values))
}

fun loginAttemptsPage(): Map<String, String> {
    val headers = listOf("IP", "User", "Password", "Captcha", "Timestamp")
    val values = mutableListOf<List<String>>()

    transaction {
        LoginAttempts.selectAll().forEach { row ->
            values += listOf(
                row[LoginAttempts.ip].toString(),
                row[LoginAttempts.username].toString(),
                row[LoginAttempts.password].toString(),
                row[LoginAttempts.captcha].toString(),
                row[LoginAttempts.timestamp].toString()
            )
        }
    }

    return mapOf("name" to "Login attempts", "table" to createTable(headers, values))
}

fun createTable(headers: List<String>, values: List<List<String>>): String {

    val headerHtml = headers.joinToString("\n") { "<th>$it</th>" }
    val valuesHtml = values.joinToString("\n") {
        "<tr>${it.joinToString("") { cell -> "<td>$cell</td>" }}</tr>"
    }

    return """
        <table>
            <tr>
                $headerHtml
            </tr>
            $valuesHtml
        </table>
    """.trimIndent()
}