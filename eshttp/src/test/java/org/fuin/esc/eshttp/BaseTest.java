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
package org.fuin.esc.eshttp;

import java.io.File;

import org.apache.http.impl.nio.client.ESHttpAsyncClients;
import org.fuin.units4j.AssertCoverage;
import org.junit.Test;

/**
 * General tests for all classes.
 */
// CHECKSTYLE:OFF Test code
public class BaseTest {

    @Test
    public final void testCoverage() {
        // Make sure all classes have a test
        AssertCoverage.assertEveryClassHasATest(new File("src/main/java"), clasz -> {
            if (clasz == BasicCustomScheme.class) {
                // Almost a 1:1 copy of the Apache code
                return false;
            }
            if (clasz == BasicCustomSchemeFactory.class) {
                // Almost a 1:1 copy of the Apache code
                return false;
            }
            // Add more ignored classes here...
            return true;
        });
    }

}
// CHECKSTYLE:ON
