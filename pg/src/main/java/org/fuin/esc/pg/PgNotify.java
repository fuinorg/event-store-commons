/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.pg;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;

import java.util.regex.Pattern;

/**
 * Generates the PostgreSQL DDL that makes an events table fire a {@code NOTIFY} on insert, so a
 * {@link PgListenNotifyWakeupSource} listening on the same channel is woken with "read now" latency. The
 * trigger is statement-level ({@code FOR EACH STATEMENT}), so a bulk insert coalesces into a single
 * notification (the payload is empty - the wake-up only signals "there may be new events", the reader then
 * catches up from its checkpoint).
 */
@ThreadSafe
public final class PgNotify {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private PgNotify() {
        throw new UnsupportedOperationException("It is not allowed to create an instance of a utility class");
    }

    /**
     * Returns the DDL that installs (or replaces) the notify trigger on the given table. Run it once against
     * the database (e.g. as part of a migration). The generated function and trigger names are derived from
     * the table name.
     *
     * @param table   Name of the events table to attach the trigger to. Must be a simple SQL identifier.
     * @param channel Notification channel to signal (must match the {@code PgListenNotifyWakeupSource}
     *                channel). Must be a simple SQL identifier.
     * @return Executable DDL (function + trigger).
     */
    public static String installTriggerSql(final String table, final String channel) {
        requireIdentifier("table", table);
        requireIdentifier("channel", channel);
        final String function = table + "_esc_notify";
        final String trigger = table + "_esc_notify_trg";
        return "CREATE OR REPLACE FUNCTION \"" + function + "\"() RETURNS trigger LANGUAGE plpgsql AS $$\n"
                + "BEGIN\n"
                + "  PERFORM pg_notify('" + channel + "', '');\n"
                + "  RETURN NULL;\n"
                + "END;\n"
                + "$$;\n"
                + "DROP TRIGGER IF EXISTS \"" + trigger + "\" ON \"" + table + "\";\n"
                + "CREATE TRIGGER \"" + trigger + "\" AFTER INSERT ON \"" + table + "\"\n"
                + "FOR EACH STATEMENT EXECUTE FUNCTION \"" + function + "\"();";
    }

    /**
     * Verifies that the given value is a simple SQL identifier ({@code [A-Za-z_][A-Za-z0-9_]*}) and can be
     * safely embedded in DDL as a quoted identifier / string literal.
     *
     * @param name  Argument name (for the error message).
     * @param value Value to check.
     * @throws IllegalArgumentException The value is not a simple identifier.
     */
    static void requireIdentifier(final String name, final String value) {
        Contract.requireArgNotNull(name, value);
        if (!IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("The argument '" + name
                    + "' must be a simple SQL identifier [A-Za-z_][A-Za-z0-9_]*, but was: '" + value + "'");
        }
    }

}
