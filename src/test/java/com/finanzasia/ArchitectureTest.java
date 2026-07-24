package com.finanzasia;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the hexagonal boundaries documented in CLAUDE.md.
 *
 * <p>These rules exist because the layout was previously honoured by discipline
 * alone. Anything that compiles but crosses a boundary should fail here.
 */
@DisplayName("Hexagonal architecture boundaries")
class ArchitectureTest {

    private static final String BASE = "com.finanzasia";

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("layers only depend inward")
    void layeredArchitectureIsRespected() {
        Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Api").definedBy(BASE + ".api..")
                .layer("Application").definedBy(BASE + ".application..")
                .layer("Domain").definedBy(BASE + ".domain..")
                .layer("Infrastructure").definedBy(BASE + ".infrastructure..")

                // Nothing may depend on the driving adapters.
                .whereLayer("Api").mayNotBeAccessedByAnyLayer()
                // Only the api layer drives use cases.
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Api")
                // Adapters are wired by Spring, never referenced by inner layers.
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()

                .check(productionClasses);
    }

    @Test
    @DisplayName("domain does not depend on any other layer")
    void domainIsIndependent() {
        noClasses()
                .that().resideInAPackage(BASE + ".domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + ".api..",
                        BASE + ".application..",
                        BASE + ".infrastructure..")
                .because("the domain is the centre of the hexagon and must stay framework free")
                .check(productionClasses);
    }

    @Test
    @DisplayName("domain does not depend on Spring, JPA, or Jackson")
    void domainIsFrameworkFree() {
        noClasses()
                .that().resideInAPackage(BASE + ".domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "com.fasterxml.jackson..")
                .because("domain models must be plain Java, not persistence or transport types")
                .check(productionClasses);
    }

    @Test
    @DisplayName("application does not depend on infrastructure or api")
    void applicationDependsOnPortsOnly() {
        noClasses()
                .that().resideInAPackage(BASE + ".application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(BASE + ".infrastructure..", BASE + ".api..")
                .because("services must talk to infrastructure through domain out-ports")
                .check(productionClasses);
    }

    @Test
    @DisplayName("api does not depend on infrastructure")
    void apiDoesNotReachIntoInfrastructure() {
        noClasses()
                .that().resideInAPackage(BASE + ".api..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE + ".infrastructure..")
                .because("controllers must not know how persistence or security is implemented")
                .check(productionClasses);
    }

    @Test
    @DisplayName("api drives use cases, never out-ports")
    void apiDoesNotUseOutPorts() {
        noClasses()
                .that().resideInAPackage(BASE + ".api..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE + ".domain.port.out..")
                .because("driving adapters call in-ports; out-ports belong to the application layer")
                .check(productionClasses);
    }

    @Test
    @DisplayName("JPA entities stay in infrastructure.persistence")
    void entitiesAreConfinedToPersistence() {
        classes()
                .that().haveSimpleNameEndingWith("Entity")
                .should().resideInAPackage(BASE + ".infrastructure.persistence..")
                .because("entities are a persistence detail and must never leak outward")
                .check(productionClasses);
    }

    /**
     * Our own JPA entities. Scoped by package on purpose: matching the "Entity"
     * suffix alone would also catch Spring's ResponseEntity.
     */
    private static final DescribedPredicate<JavaClass> JPA_ENTITIES =
            JavaClass.Predicates.resideInAPackage(BASE + ".infrastructure.persistence..")
                    .and(DescribedPredicate.describe(
                            "with simple name ending in 'Entity'",
                            clazz -> clazz.getSimpleName().endsWith("Entity")));

    @Test
    @DisplayName("controllers do not return JPA entities")
    void controllersReturnDtos() {
        noClasses()
                .that().resideInAPackage(BASE + ".api.controller..")
                .should().dependOnClassesThat(JPA_ENTITIES)
                .because("responses must be DTOs so the persistence model can change freely")
                .check(productionClasses);
    }
}
