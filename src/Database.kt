package com.cout970

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BloquedIPs : Table() {
    val ip = varchar("ip", length = 50).primaryKey()
    val created = datetime("created")
    val redirects = integer("redirects")

    fun isIpBloqued(blockedIP: String): Boolean = transaction {
        BloquedIPs.select { BloquedIPs.ip eq blockedIP }.count() == 0
    }
}