/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.api;

import static org.fuin.units4j.JandexAssert.assertThat;

import java.io.File;
import java.util.List;

import org.fuin.units4j.AssertCoverage;
import org.fuin.units4j.Units4JUtils;
import org.jboss.jandex.Index;
import org.junit.Ignore;
import org.junit.Test;

/**
 * General tests for all classes.
 */
// CHECKSTYLE:OFF Test code
public class BaseTest {

    @Test
    public final void testCoverage() {
        // Make sure all classes have a test
        AssertCoverage.assertEveryClassHasATest(new File("src/main/java"));
    }

    @Ignore("TODO Fix failing test with new units4j version")
    @Test
    public final void testNullability() {

        // Collect all class files
        File dir = new File("target/classes");
        List<File> classFiles = Units4JUtils.findAllClasses(dir);
        Index index = Units4JUtils.indexAllClasses(classFiles);

        // Verify that all methods make a statement if null is allowed or not
        assertThat(index).hasNullabilityInfoOnAllMethods();

    }

}
// CHECKSTYLE:ON
