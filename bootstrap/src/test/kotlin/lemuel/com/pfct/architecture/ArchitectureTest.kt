package lemuel.com.pfct.architecture

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * 아키텍처 규칙을 테스트로 강제한다(ADR-0002). 문서로만 적힌 규칙은 시간이 지나면 깨지기 쉬우므로,
 * "도메인은 프레임워크를 모른다", "의존성 방향은 adapter → application → domain" 을 컴파일된 클래스로 검증한다.
 */
@AnalyzeClasses(
    packages = ["lemuel.com.pfct"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureTest {

    @ArchTest
    val domainHasNoFrameworkDependency: ArchRule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..",
        )
        .because("도메인은 어떤 프레임워크에도 의존하면 안 된다")

    @ArchTest
    val domainDependsOnNothingInner: ArchRule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")
        .because("도메인은 application/adapter 를 향해 의존하면 안 된다(의존성 역전)")

    @ArchTest
    val applicationDoesNotDependOnAdapter: ArchRule = noClasses()
        .that().resideInAPackage("..application..")
        .should().dependOnClassesThat().resideInAPackage("..adapter..")
        .because("application 은 adapter(인프라 구현)를 알면 안 된다(포트로만 통신)")
}
