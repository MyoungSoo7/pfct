// Root build — 플러그인 버전을 여기서 한 번만 선언(apply false)하고, 각 모듈은 버전 없이 적용한다.
// 전 모듈 Kotlin 1.9.25 / Java 21 / Spring Boot 3.5.15 로 통일.
plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    kotlin("plugin.jpa") version "1.9.25" apply false
    id("org.springframework.boot") version "3.5.15" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "lemuel.com"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
