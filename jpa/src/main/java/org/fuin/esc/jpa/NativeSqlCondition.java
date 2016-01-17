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
package org.fuin.esc.jpa;

import org.fuin.objects4j.common.Contract;

/**
 * Defines a native SQL 'where' condition.
 */
public final class NativeSqlCondition {

    private final String table;

    private final String column;

    private final String operator;

    private final Object value;

    /**
     * Constructor with all mandatory data.
     * 
     * @param column
     *            Name of the DB column.
     * @param operator
     *            Operator for comparing the value.
     * @param value
     *            Value of the attribute.
     */
    public NativeSqlCondition(final String column, final String operator, final Object value) {
        this(null, column, operator, value);
    }

    /**
     * Constructor with all possible data.
     * 
     * @param table
     *            Optional table prefix.
     * @param column
     *            Name of the DB column.
     * @param operator
     *            Operator for comparing the value.
     * @param value
     *            Value of the attribute.
     */
    public NativeSqlCondition(final String table, final String column, final String operator,
            final Object value) {
        Contract.requireArgNotNull("column", column);
        Contract.requireArgNotNull("operator", operator);
        Contract.requireArgNotNull("value", value);
        this.table = table;
        this.column = column;
        this.operator = operator;
        this.value = value;
    }
    
    /**
     * Returns the table name.
     * 
     * @return Optional table name or prefix.
     */
    public final String getTable() {
        return table;
    }

    /**
     * Returns the name of the table column.
     * 
     * @return Column name.
     */
    public final String getColumn() {
        return column;
    }

/**
     * Returns the operator for comparing the value.
     * 
     * @return Operator like '=' or '&lt;'.
     */
    public final String getOperator() {
        return operator;
    }

    /**
     * Returns the value to compare with.
     * 
     * @return Value.
     */
    public final Object getValue() {
        return value;
    }

    /**
     * Returns the 'where' condition with a parameter.
     * 
     * @return Native SQL 'where' with column name as parameter.
     */
    public final String asWhereConditionWithParam() {
        if (table == null) {
            return column + operator + ":" + column;
        }
        return table + "." + column + operator + ":" + column;
    }

    @Override
    public final String toString() {
        if (table == null) {
            return column + operator + column;
        }
        return table + "." + column + operator + value;
    }

}
