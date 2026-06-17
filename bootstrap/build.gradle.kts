// bootstrap: Spring Boot 실행 + 의존성 조립 모듈. 모든 컨텍스트를 여기서 조립한다.
// Spring Boot 플러그인은 오직 이 모듈에만 적용 → 라이브러리 모듈은 프레임워크에 오염되지 않는다.
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":modules:ledger"))
    implementation(project(":modules:investment"))
    implementation(project(":modules:lending"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
