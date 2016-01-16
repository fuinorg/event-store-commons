/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.jpa;

import static org.fuin.units4j.JandexAssert.assertThat;

import java.io.File;
import java.util.List;

import org.fuin.units4j.AssertCoverage;
import org.fuin.units4j.Units4JUtils;
import org.jboss.jandex.Index;
import org.junit.Test;

/**
 * General tests for all classes.
 */
// CHECKSTYLE:OFF Test code
public class BaseTest {

    @Test
    public final void testCoverage() {
        // Make sure all classes have a test
        AssertCoverage.assertEveryClassHasATest(new File("src/main/java"), new AssertCoverage.ClassFilter() {
            @Override
            public boolean isIncludeClass(final Class<?> clasz) {
                // Add exclusions here...
                return true;
            }
        });
    }

    @Test
    public final void testJandex() {
        
     // Collect all class files
        final File dir = new File("target/classes");
        final List<File> classFiles = Units4JUtils.findAllClasses(dir);
        final Index index = Units4JUtils.indexAllClasses(classFiles);

        // Verify that all classes annotated with @Entity observe the rules for JPA entities 
        // (Class not final + No final methods + ...).
        assertThat(index).hasOnlyValidJpaEntities();        
        
    }
    
}
// CHECKSTYLE:ON
