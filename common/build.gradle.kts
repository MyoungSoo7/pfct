// common: 순수 코틀린 금융 원시 타입(Money, AnnualInterestRate, DomainEvent).
// Spring / JPA 의존성이 단 하나도 없다는 것 자체가 "도메인은 프레임워크를 모른다"의 증명.
plugins {
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
