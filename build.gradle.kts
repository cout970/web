import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project

plugins {
    application
    kotlin("jvm") version "1.3.21"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

group = "web"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("https://kotlin.bintray.com/kotlin-js-wrappers") }
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    compile("io.ktor:ktor-server-netty:$ktor_version")
    compile("ch.qos.logback:logback-classic:$logback_version")
    compile("io.ktor:ktor-server-core:$ktor_version")
    compile("io.ktor:ktor-html-builder:$ktor_version")
    compile("org.jetbrains:kotlin-css-jvm:1.0.0-pre.31-kotlin-1.2.41")
    compile("io.ktor:ktor-server-host-common:$ktor_version")
    compile("io.ktor:ktor-locations:$ktor_version")
    compile("io.ktor:ktor-metrics:$ktor_version")
    compile("io.ktor:ktor-server-sessions:$ktor_version")
    compile("io.ktor:ktor-gson:$ktor_version")
    compile("org.jetbrains.exposed:exposed:$exposed_version")
    compile("mysql:mysql-connector-java:8.0.15")

    testCompile("io.ktor:ktor-server-tests:$ktor_version")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

val shadowJar by tasks.getting(ShadowJar::class) {
    archiveName = "web.jar"
    manifest {
        attributes["Main-Class"] = "io.ktor.server.netty.EngineMain"
    }
}
