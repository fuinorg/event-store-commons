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
package org.fuin.esc.api;

import jakarta.activation.MimeTypeParseException;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Enhances the {@link jakarta.activation.MimeType} class with convenience methods for accessing version and
 * encoding parameters. Equals and hash code are base on the {@link #toString()} method.
 */
public final class EnhancedMimeType extends jakarta.activation.MimeType {

    /** Version parameter name. */
    public static final String VERSION = "version";

    /** Encoding parameter name. */
    public static final String ENCODING = "encoding";

    /**
     * Default constructor for de-serialization.
     */
    public EnhancedMimeType() {
        super();
    }

    /**
     * Constructor with all data.
     *
     * @param str
     *            Contains base type, subtype and parameters.
     *
     * @throws MimeTypeParseException
     *             If the string is not valid.
     */
    public EnhancedMimeType(@NotNull final String str) throws MimeTypeParseException {
        super(str);
    }

    /**
     * Constructor with primary and subtype (no parameters).
     *
     * @param primary
     *            Primary type.
     * @param sub
     *            Subtype.
     *
     * @throws MimeTypeParseException
     *             If the primary type or subtype is not a valid token
     */
    public EnhancedMimeType(@NotNull final String primary, @NotNull final String sub)
            throws MimeTypeParseException {
        super(primary, sub);
    }

    /**
     * Constructor with primary, subtype and encoding.
     *
     * @param primary
     *            Primary type.
     * @param sub
     *            Subtype.
     * @param encoding
     *            Encoding.
     *
     * @throws MimeTypeParseException
     *             If the primary type or subtype is not a valid token
     */
    public EnhancedMimeType(@NotNull final String primary, @NotNull final String sub,
                            @Nullable final Charset encoding) throws MimeTypeParseException {
        super(primary, sub);
        if (encoding != null) {
            super.setParameter(ENCODING, encoding.name());
        }
    }

    /**
     * Constructor with primary, subtype, encoding and version.
     *
     * @param primary
     *            Primary type.
     * @param sub
     *            Subtype.
     * @param encoding
     *            Encoding.
     * @param version
     *            Version.
     *
     * @throws MimeTypeParseException
     *             If the primary type or subtype is not a valid token
     */
    public EnhancedMimeType(@NotNull final String primary, @NotNull final String sub,
                            @Nullable final Charset encoding, @Nullable final String version) throws MimeTypeParseException {
        this(primary, sub, encoding, version, null);
    }

    /**
     * Constructor with all data.
     *
     * @param primary
     *            Primary type.
     * @param sub
     *            Subtype.
     * @param encoding
     *            Encoding.
     * @param version
     *            Version.
     * @param params
     *            Other parameters than version and encoding.
     *
     * @throws MimeTypeParseException
     *             If the primary type or subtype is not a valid token
     */
    public EnhancedMimeType(@NotNull final String primary, @NotNull final String sub,
                            @Nullable final Charset encoding, @Nullable final String version,
                            @Nullable final Map<String, String> params) throws MimeTypeParseException {
        super(primary, sub);
        if (encoding != null) {
            super.setParameter(ENCODING, encoding.name());
        }
        if (version != null) {
            super.setParameter(VERSION, version);
        }
        if (params != null) {
            final Iterator<String> it = params.keySet().iterator();
            while (it.hasNext()) {
                final String key = it.next();
                final String value = params.get(key);
                if (key.equals(ENCODING)) {
                    throw new IllegalArgumentException(
                            "Setting encoding with the parameters is not allowed. "
                                    + "Use the 'encoding' argument instead.");
                }
                if (key.equals(VERSION)) {
                    throw new IllegalArgumentException("Setting version with the parameters is not allowed. "
                            + "Use the 'version' argument instead.");
                }
                super.setParameter(key, value);
            }
        }
    }

    /**
     * Returns the version from the parameters.
     *
     * @return Version or <code>null</code> if not available.
     */
    @Nullable
    public String getVersion() {
        return getParameter(VERSION);
    }

    /**
     * Returns the encoding from the parameters.
     *
     * @return Encoding or <code>null</code> as default if not available.
     */
    @Nullable
    public Charset getEncoding() {
        final String parameter = getParameter(ENCODING);
        if (parameter == null) {
            return null;
        }
        return Charset.forName(parameter);
    }

    /**
     * Returns the information if the base type is "application/json".
     *
     * @return TRUE if it's JSON content, else FALSE:
     */
    public boolean isJson() {
        return getBaseType().equals("application/json");
    }

    /**
     * Returns the information if the base type is "application/xml".
     *
     * @return TRUE if it's XML content, else FALSE:
     */
    public boolean isXml() {
        return getBaseType().equals("application/xml");
    }

    /**
     * Determine if the primary, subtype and encoding of this object is the same as what is in the given
     * type.
     *
     * @param other
     *            The MimeType object to compare with.
     *
     * @return True if they match.
     */
    public boolean matchEncoding(final EnhancedMimeType other) {
        return match(other) && Objects.equals(getEncoding(), other.getEncoding());

    }

    /**
     * Creates an instance with all data. Exceptions are wrapped to runtime exceptions.
     *
     * @param str
     *            Contains base type, subtype, version and parameters.
     *
     * @return New instance.
     */
    @Nullable
    public static EnhancedMimeType create(@Nullable final String str) {
        if (str == null) {
            return null;
        }
        try {
            return new EnhancedMimeType(str);
        } catch (final MimeTypeParseException ex) {
            throw new RuntimeException("Failed to create versioned mime type: " + str, ex);
        }
    }

    /**
     * Creates an instance with primary and subtype (no version, no parameters). Exceptions are wrapped to
     * runtime exceptions.
     *
     * @param primary
     *            Primary type.
     * @param sub
     *            Subtype.
     *
     * @return New instance.
     */
    @NotNull
    public static EnhancedMimeType create(@NotNull final String primary, @NotNull final String sub) {
        return create(primary, sub, null, null, null);
    }

    /**
     * Creates an instance with primary, subtype and encoding (no version). Exceptions are wrapped to runtime
     * exceptions.
     *
     * @param primary
     *            Primary type.
     * @param sub
     *            Subtype.
     * @param encoding
     *            Encoding.
     *
     * @return New instance.
     */
    @NotNull
    public static EnhancedMimeType create(@NotNull final String primary, @NotNull final String sub,
                                          final Charset encoding) {
        return create(primary, sub, encoding, null, null);
    }

    /**
     * Creates an instance with primary, subtype, encoding and version. Exceptions are wrapped to runtime
     * exceptions.
     *
     * @param primary
     *            Primary type.
     * @param sub
     *            Subtype.
     * @param encoding
     *            Encoding.
     * @param version
     *            Version.
     *
     * @return New instance.
     */
    @NotNull
    public static EnhancedMimeType create(@NotNull final String primary, @NotNull final String sub,
                                          final Charset encoding, final String version) {
        return create(primary, sub, encoding, version, new HashMap<>());
    }

    /**
     * Creates an instance with all data and exceptions wrapped to runtime exceptions.
     *
     * @param primary
     *            Primary type.
     * @param sub
     *            Subtype.
     * @param encoding
     *            Encoding.
     * @param version
     *            Version.
     * @param parameters
     *            Additional parameters.
     *
     * @return New instance.
     */
    @NotNull
    public static EnhancedMimeType create(@NotNull final String primary, @NotNull final String sub,
                                          final Charset encoding, final String version, final Map<String, String> parameters) {
        try {
            return new EnhancedMimeType(primary, sub, encoding, version, parameters);
        } catch (final MimeTypeParseException ex) {
            throw new RuntimeException("Failed to create versioned mime type: " + primary + "/" + sub, ex);
        }
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EnhancedMimeType)) {
            return false;
        }
        final EnhancedMimeType other = (EnhancedMimeType) obj;
        return toString().equals(other.toString());
    }

}
