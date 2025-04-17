/**
 * Copyright (C) 2013 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.jsonb;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.fuin.esc.api.EscApiUtils;
import org.fuin.esc.spi.EscSpiUtils;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.DependencyRules.NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;

@AnalyzeClasses(packagesOf = ArchitectureTest.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String API_PACKAGE = EscApiUtils.class.getPackageName();

    private static final String SPI_PACKAGE = EscSpiUtils.class.getPackageName();

    private static final String THIS_PACKAGE = ArchitectureTest.class.getPackageName();

    @ArchTest
    static final ArchRule no_accesses_to_upper_package = NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;

    @ArchTest
    static final ArchRule common_access_only_to_defined_packages = classes()
            .that()
            .resideInAPackage(THIS_PACKAGE)
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(THIS_PACKAGE, API_PACKAGE, SPI_PACKAGE,
                    "java..",
                    "jakarta.annotation..",
                    "jakarta.validation..",
                    "javax.annotation.concurrent..",
                    "org.fuin.objects4j.common..",
                    "org.fuin.objects4j.core..",
                    "org.slf4j..",
                    "jakarta.json..",
                    "org.apache.commons.lang3..",
                    "org.fuin.utils4j..",
                    "com.google.auto.service.."
            );


}

