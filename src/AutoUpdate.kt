package com.cout970

import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlin.streams.toList

suspend fun updateServer(): String = execute("scripts/update.sh")

private val processContext = newSingleThreadContext("External command execution")

@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun execute(command: String): String = withContext(processContext) {
    val process = ProcessBuilder(command).start()

    val output = process.inputStream
        .bufferedReader()
        .lines()
        .map { println("[Command output] $it"); it }
        .toList()
        .joinToString("\n")

    val errorOutput = process.errorStream
        .bufferedReader()
        .lines()
        .map { println("[Command error output] $it"); it }
        .toList()
        .joinToString("\n")

    val code = process.waitFor()

    return@withContext if (code != 0) {
        "Error executing command: $command, return code: $code\noutput:\n$output\n---\nerrorOutput: \n$errorOutput\n---"
    } else {
        output
    }
}