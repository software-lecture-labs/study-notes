package com.lectures.business.design.arch;

import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderStatus;
import com.lectures.business.design.shared.kernel.Address;
import com.lectures.business.design.shared.kernel.DomainEvent;
import com.lectures.business.design.shared.kernel.DomainException;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Optional;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@AnalyzeClasses(
        packages = {"com.lectures.business.design.sales", "com.lectures.business.design.shared"},
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // ADR-0003 permission matrix
    @ArchTest
    static final ArchRule domain_does_not_see_other_layers = noClasses()
            .that().resideInAPackage("..sales.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..sales.api..", "..sales.application..", "..sales.persistence..");

    @ArchTest
    static final ArchRule api_is_a_pure_contract = noClasses()
            .that().resideInAPackage("..sales.api..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..sales.domain..", "..sales.application..", "..sales.persistence..");

    @ArchTest
    static final ArchRule persistence_only_sees_domain = noClasses()
            .that().resideInAPackage("..sales.persistence..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..sales.api..", "..sales.application..");

    @ArchTest
    static final ArchRule shared_kernel_depends_on_no_business_module = noClasses()
            .that().resideInAPackage("..shared.kernel..")
            .should().dependOnClassesThat()
            .resideInAPackage("..sales..");

    //  Domain Conventions Check
    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses()
            .that().resideInAnyPackage("..sales.domain..", "..shared.kernel..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta..", "javax..", "java.sql..");

    @ArchTest
    static final ArchRule domain_has_no_setters = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..sales.domain..")
            .should().haveNameMatching("set[A-Z].*");

    @ArchTest
    static final ArchRule optional_is_not_a_field_type = fields()
            .should().notHaveRawType(Optional.class);

    @ArchTest
    static final ArchRule domain_exceptions_share_one_base = classes()
            .that().resideInAPackage("..sales.domain..")
            .and().haveSimpleNameEndingWith("Exception")
            .should().beAssignableTo(DomainException.class);

    //  Common Coding Rules
    @ArchTest
    static final ArchRule no_standard_streams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule no_dumping_ground_packages = noClasses()
            .should().resideInAnyPackage("..util..", "..common..", "..helper..", "..misc..");

    //  ADR-0006 Composition root and constructor injection
    @ArchTest
    static final ArchRule application_depends_on_contracts_not_implementations = noClasses()
            .that().resideInAPackage("..sales.application..")
            .should().dependOnClassesThat().haveSimpleNameStartingWith("InMemory");

    @ArchTest
    static final ArchRule application_collaborators_are_private_final = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..sales.application..")
            .and().areNotStatic()
            .should().bePrivate().andShould().beFinal();

    @ArchTest
    static final ArchRule no_hidden_clock = noClasses()
            .should().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(Instant.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis");

    // ADR-0007 Domain Events
    @ArchTest
    static final ArchRule domain_events_are_records = classes()
            .that().implement(DomainEvent.class)
            .and().areNotInterfaces()
            .should().beAssignableTo(Record.class);

    @ArchTest
    static final ArchRule domain_events_live_in_the_domain = classes()
            .that().implement(DomainEvent.class)
            .should().resideInAnyPackage("..sales.domain..", "..shared.kernel..");

    @ArchTest
    static final ArchRule only_application_pulls_events = noClasses()
            .that().resideOutsideOfPackage("..sales.application..")
            .should().callMethod(Order.class, "pullEvents");

    //  ADR-0008 Persistence boundary
    @ArchTest
    static final ArchRule only_persistence_touches_jdbc = noClasses()
            .that().resideOutsideOfPackage("..sales.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "javax.sql..");

    @ArchTest
    static final ArchRule only_persistence_reconstitutes = noClasses()
            .that().resideOutsideOfPackage("..sales.persistence..")
            .should().callMethod(Order.class, "reconstitute", OrderId.class, CustomerId.class,
                    LocalDate.class, OrderStatus.class, Address.class, LocalDate.class, List.class);

    @ArchTest
    static final ArchRule repository_contracts_hide_sql_exceptions = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..sales.persistence..")
            .and().arePublic()
            .should().declareThrowableOfType(java.sql.SQLException.class);
}
