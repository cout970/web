package com.cout970

import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger("Template")

fun includeWrapperTemplate(wrapper: String, name: String, env: Map<String, String> = emptyMap()): String {
    return includeTemplate(wrapper, mapOf("$0" to includeTemplate(name, env)))
}

fun includeTemplate(name: String, env: Map<String, String> = emptyMap()): String {
    val file = File("resources/templates/$name")
    if (file.exists()) {
        return processTemplate(file.readText(), env)
    }
    log.warn("File $name not found")
    return "File $name not found"
}

private fun processTemplate(input: String, env: Map<String, String>): String {

    val firstLine = input.lineSequence().first().trim()

    return if (firstLine.startsWith("<!--") && firstLine.endsWith("-->")) {
        val newEnv = parseTemplateHeader(input.substring(0, input.indexOf('\n')), env)
        val content = input.substring(input.indexOf('\n'))

        processTemplateContent(content, newEnv)
    } else {

        processTemplateContent(input, env)
    }
}

private fun processTemplateContent(input: String, env: Map<String, String>): String {
    val template = StringBuilder()
    val code = StringBuilder()

    var inCode = false
    var dollarRead = false
    var brackets = 0

    for (c in input) {
        if (inCode) {
            if (c == '{') brackets++
            if (c == '}' && brackets == 0) {
                inCode = false
                template.append(processCode(code.toString(), env))
                code.clear()
            } else {
                code.append(c)
            }
            if (c == '}') brackets--
        } else {
            if (c == '$') {
                dollarRead = true
            } else {
                when {
                    dollarRead && c == '{' -> {
                        inCode = true
                        brackets = 0
                    }
                    dollarRead && c != '{' -> template.append('$').also { template.append(c) }
                    else -> template.append(c)
                }
                dollarRead = false
            }
        }
    }
    return template.toString()
}

private fun processCode(code: String, env: Map<String, String>): String {
    val function = """(\w[\w|\d]*)\(([^)]*)\)""".toRegex()
    val variable = """((\$|\w)[\w|\d]*)""".toRegex()

    function.matchEntire(code)?.let { result ->
        val func = result.groupValues[1]
        val args = if (result.groupValues[2].isBlank()) {
            emptyList()
        } else {
            result.groupValues[2]
                .split(",")
                .map { it.trim() }
                .map { parseArgument(it, env) }
        }

        if (func == "include") {
            val arguments = args.drop(1).mapIndexed { index, value -> "\$$index" to value }.toMap()
            return includeTemplate(args[0], arguments)
        }

        if (!env.containsKey(func)) {
            log.warn("Attempt to call undefined function: '$func'")
            return ""
        }

        val arguments = args.mapIndexed { index, value -> "\$$index" to value }.toMap()

        return includeTemplate(env.getValue(func), arguments)
    }

    variable.matchEntire(code)?.let { result ->
        return env[result.value] ?: "null"
    }

    log.warn("Unable to process code: '$code'")
    return "Unable to process code: '$code'"
}

private fun parseArgument(code: String, env: Map<String, String>): String {
    val variable = """((\$|\w)[\w|\d]*)""".toRegex()

    if (code.startsWith("\"\"\"") && code.endsWith("\"\"\"")) {
        return processTemplateContent(code.substring(3, code.length - 3), env)
    }
    variable.matchEntire(code)?.let { result ->
        return env[result.value] ?: "null"
    }

    log.warn("Unable to parse argument: '$code'")
    return "Unable to parse argument: '$code'"
}

private fun parseTemplateHeader(header: String, env: Map<String, String>): Map<String, String> {
    val args = header.substring(4, header.length - 3).trim()

    if (args.startsWith("args:")) {
        val argNames = args.substring(5)
            .trim()
            .split(",")
            .map { it.trim() }

        val newEnv = env.toMutableMap()
        argNames.forEachIndexed { index, name ->
            if (env.containsKey(name)) return@forEachIndexed
            val value = env["$$index"]

            if (value == null) {
                log.warn("null argument: '$name'")
            } else {
                newEnv[name] = value
            }
        }
        return newEnv
    }

    log.warn("invalid header: '$header'")
    return env
}