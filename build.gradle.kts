import java.util.concurrent.TimeUnit

plugins {
    id("java")
}

group = "kz.bnk.tests"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("io.appium:java-client:9.3.0")
    testImplementation("org.seleniumhq.selenium:selenium-java:4.25.0")
    testImplementation("org.testng:testng:7.10.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
    testImplementation("io.qameta.allure:allure-testng:2.29.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Removes files older than 3 days from artifact/result folders so they don't pile up.
val cleanOldArtifacts by tasks.registering {
    doLast {
        val cutoff: Long = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
        val targets = listOf("build/test-artifacts", "build/test-results")
        var deleted = 0
        targets.forEach { dir ->
            val root = file(dir)
            if (root.exists()) {
                root.walk()
                    .filter { it.isFile && it.lastModified() < cutoff }
                    .forEach {
                        if (it.delete()) deleted++
                    }
            }
        }
        if (deleted > 0) println("[cleanOldArtifacts] removed $deleted file(s) older than 3 days")
    }
}

tasks.test {
    dependsOn(cleanOldArtifacts)
    useTestNG {
        val platform = System.getProperty("platform", "android")
        // -DsuiteFile=foo.xml lets us run a custom suite (e.g. a regression subset)
        // while keeping -Dplatform=android for driver/properties selection.
        val suiteFile = System.getProperty("suiteFile", "$platform.xml")
        suiteXmlFiles = listOf(file("src/test/resources/suites/$suiteFile"))
    }
    systemProperties = System.getProperties()
        .entries
        .associate { it.key.toString() to it.value }
        .toMutableMap()
    testLogging {
        events("passed", "failed", "skipped", "standardOut", "standardError")
        showStandardStreams = true
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
