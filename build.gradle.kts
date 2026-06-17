// Root build — version catalog-free, plugin versions declared once here (apply false),
// then applied without version in each module. Keeps the dependency direction explicit.
plugins {
    kotlin("plugin.spring") version "1.9.25" apply false
    id("org.springframework.boot") version "3.5.15" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.4.0"
}

allprojects {
    group = "lemuel.com"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(8)
}