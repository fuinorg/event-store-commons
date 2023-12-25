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

import org.fuin.objects4j.vo.AbstractStringValueObject;
import org.fuin.objects4j.vo.ValueObjectWithBaseType;
import org.fuin.units4j.AssertCoverage;
import org.fuin.units4j.Units4JUtils;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.fuin.units4j.JandexAssert.assertThat;

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

    @Disabled("TODO Fix failing test with new units4j version")
    @Test
    public final void testNullability() {

        // Collect all class files
        final File dir = new File("target/classes");
        final List<File> classFiles = Units4JUtils.findAllClasses(dir);
        final Indexer indexer = new Indexer();
        Units4JUtils.indexAllClasses(indexer, classFiles);
        Units4JUtils.index(indexer, this.getClass().getClassLoader(), ValueObjectWithBaseType.class.getName());
        Units4JUtils.index(indexer, this.getClass().getClassLoader(), AbstractStringValueObject.class.getName());        
        final Index index = indexer.complete();

        // Verify that all methods make a statement if null is allowed or not
        assertThat(index).hasNullabilityInfoOnAllMethods();

    }

}
// CHECKSTYLE:ON
