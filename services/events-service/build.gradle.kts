
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.ticketflow"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.statusPages)
    implementation(libs.logback.classic)

    // Exposed ORM / SQL framework
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")

    // PostgreSQL JDBC Driver
    implementation("org.postgresql:postgresql:42.7.7")

    // Connection Pool
    implementation("com.zaxxer:HikariCP:6.3.0")

    // Database migrations
    implementation("org.flywaydb:flyway-core:11.13.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.13.0")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
