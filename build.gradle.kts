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
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent {
            snapshotsOnly()
        }
    }
}

dependencies {
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    
    implementation("dev.tamboui:tamboui-tui:0.3.0")
    implementation("dev.tamboui:tamboui-toolkit:0.3.0")
    implementation("dev.tamboui:tamboui-jline3-backend:0.3.0")
    implementation("dev.tamboui:tamboui-picocli:0.3.0")
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
    
    implementation("com.google.code.gson:gson:2.10.1")
    
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.openrise.stegorouter.ui.StegoRouterAppKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("stegorouter")
            mainClass.set("io.openrise.stegorouter.ui.StegoRouterAppKt")
            buildArgs.add("--no-fallback")
            buildArgs.add("--enable-url-protocols=http,https")
        }
    }
}
