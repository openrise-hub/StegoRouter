plugins {
    kotlin("jvm") version "1.9.25"
    application
    id("org.graalvm.buildtools.native") version "0.10.2"
}

group = "io.openrise.stegorouter"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.openrise.stegorouter.MainKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("stegorouter")
            mainClass.set("io.openrise.stegorouter.MainKt")
            buildArgs.add("--no-fallback")
            buildArgs.add("--enable-url-protocols=http,https")
        }
    }
}
