package com.tradernet.api;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.Stateless;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.fail;

class ArchitectureBoundaryTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.tradernet");

    private static final Set<String> SERVICE_MODULES = Set.of(
        "order",
        "portfolio",
        "trade",
        "user",
        "marketai",
        "currencyconversion"
    );

    @Test
    void apiDoesNotDependOnPersistenceOrMarketInternals() {
        noClasses()
            .that().resideInAPackage("com.tradernet.api..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.tradernet.jpa..",
                "com.tradernet.marketai.engine..",
                "com.tradernet.marketai.stream.."
            )
            .check(CLASSES);
    }

    @Test
    void localContractsDoNotExposePersistenceTypes() {
        noClasses()
            .that().areAnnotatedWith(Local.class)
            .should().dependOnClassesThat().resideInAPackage("com.tradernet.jpa..")
            .check(CLASSES);
    }

    @Test
    void serviceBeansDoNotExposeBroadNoInterfaceViews() {
        noClasses()
            .that().resideInAnyPackage(
                "com.tradernet.order..",
                "com.tradernet.portfolio..",
                "com.tradernet.trade..",
                "com.tradernet.user..",
                "com.tradernet.marketai..",
                "com.tradernet.currencyconversion.."
            )
            .should().beAnnotatedWith(LocalBean.class)
            .check(CLASSES);
    }

    @Test
    void apiInjectsCrossModuleEjbsThroughLocalInterfaces() {
        final List<String> violations = new ArrayList<>();
        for (JavaClass javaClass : CLASSES) {
            if (!javaClass.getPackageName().startsWith("com.tradernet.api")) {
                continue;
            }
            for (JavaField field : javaClass.getFields()) {
                if (!field.isAnnotatedWith(EJB.class)) {
                    continue;
                }
                final JavaClass fieldType = field.getRawType();
                if (fieldType.getPackageName().startsWith("com.tradernet.api")) {
                    continue;
                }
                if (!fieldType.isInterface() || !fieldType.isAnnotatedWith(Local.class)) {
                    violations.add(field.getFullName() + " must use an explicit @Local interface");
                }
            }
        }
        failOnViolations(violations);
    }

    @Test
    void modulesDoNotDependOnOtherServiceImplementations() {
        final List<String> violations = new ArrayList<>();
        for (JavaClass origin : CLASSES) {
            final String originModule = module(origin);
            if (originModule == null && !origin.getPackageName().startsWith("com.tradernet.api")) {
                continue;
            }
            for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                final JavaClass target = dependency.getTargetClass();
                final String targetModule = module(target);
                if (targetModule == null || targetModule.equals(originModule)) {
                    continue;
                }
                if (target.isAnnotatedWith(Stateless.class) || target.isAnnotatedWith(Singleton.class)) {
                    violations.add(origin.getName() + " depends on service implementation " + target.getName());
                }
            }
        }
        failOnViolations(violations);
    }

    private static String module(JavaClass javaClass) {
        final String packageName = javaClass.getPackageName();
        if (!packageName.startsWith("com.tradernet.")) {
            return null;
        }
        final String remainder = packageName.substring("com.tradernet.".length());
        final int separator = remainder.indexOf('.');
        final String candidate = separator < 0 ? remainder : remainder.substring(0, separator);
        return SERVICE_MODULES.contains(candidate) ? candidate : null;
    }

    private static void failOnViolations(List<String> violations) {
        if (!violations.isEmpty()) {
            fail(String.join(System.lineSeparator(), violations));
        }
    }
}
