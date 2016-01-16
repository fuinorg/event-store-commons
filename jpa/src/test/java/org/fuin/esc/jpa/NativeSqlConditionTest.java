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

import static org.assertj.core.api.Assertions.assertThat;
import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;

// CHECKSTYLE:OFF
public final class NativeSqlConditionTest {

    
    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(NativeSqlCondition.class).verify();
    }
    
    @Test
    public void testGetter() {

        // PREPARE
        final NativeSqlCondition testee = new NativeSqlCondition("a", "=", 4711L);
        
        // TEST
        assertThat(testee.getColumn()).isEqualTo("a");
        assertThat(testee.getOperator()).isEqualTo("=");
        assertThat(testee.getValue()).isEqualTo(4711L);

    }
    
    @Test
    public void testAsWhereConditionWithParam() {
        
        // PREPARE
        final NativeSqlCondition testee1 = new NativeSqlCondition("a", "=", 4711L);
        final NativeSqlCondition testee2 = new NativeSqlCondition("t", "a", "=", 4711L);
        
        // TEST
        final String whereSql1 = testee1.asWhereConditionWithParam();
        final String whereSql2 = testee2.asWhereConditionWithParam();
        
        // VERIFY
        assertThat(whereSql1).isEqualTo("a=:a");
        assertThat(whereSql2).isEqualTo("t.a=:a");
        
    }

}
// CHECKSTYLE:ON
