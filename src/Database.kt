package com.cout970

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object BloquedIPs : Table() {
    val ip = varchar("ip", length = 50).primaryKey()
    val created = datetime("created")
    val redirects = integer("redirects")

    fun isIpBloqued(blockedIP: String): Boolean = transaction {
        BloquedIPs.select { BloquedIPs.ip eq blockedIP }.count() == 0
    }
}

object LoginAttempts : Table() {
    val line = integer("line").autoIncrement().primaryKey()
    val ip = varchar("ip", length = 50)
    val timestamp = datetime("timestamp")
    val username = varchar("username", length = 50)
    val password = varchar("password", length = 50)
    val captcha = varchar("captcha", length = 80)

    fun registerAttempt(param_ip: String, param_username: String, param_password: String, param_extra: String) {
        transaction {
            LoginAttempts.insert { query ->
                query[ip] = param_ip
                query[timestamp] = DateTime.now()
                query[username] = param_username
                query[password] = param_password
                query[captcha] = param_extra
            }
        }
    }
}