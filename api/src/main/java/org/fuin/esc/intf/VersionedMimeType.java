package org.fuin.esc.intf;

import java.nio.charset.Charset;

import javax.activation.MimeTypeParseException;

/**
 * Enhances the {@link javax.activation.MimeType} class with equals and hash
 * code based on the base type.
 */
public final class VersionedMimeType extends javax.activation.MimeType {

    private static final String VERSION = "version";

    private static final String DEFAULT_VERSION = "1.0.0";

    private static final String ENCODING = "encoding";

    private static final String DEFAULT_ENCODING = "utf-8";

    /**
     * Constructor with all data.
     * 
     * @param str
     *            Contains base type, sub type and parameters.
     * 
     * @throws MimeTypeParseException
     *             If the string is not valid.
     */
    public VersionedMimeType(final String str) throws MimeTypeParseException {
        super(str);
    }

    /**
     * Constructor with all data.
     * 
     * @param primary
     *            Primary type.
     * @param sub
     *            Sub type.
     * @param encoding
     *            Encoding.
     * @param version
     *            Version.
     * 
     * @throws MimeTypeParseException
     *             If the primary type or sub type is not a valid token
     */
    public VersionedMimeType(final String primary, final String sub,
            final Charset encoding, final String version)
            throws MimeTypeParseException {
        super(primary, sub);
        setParameter(ENCODING, encoding.name());
        setParameter(VERSION, version);
    }

    /**
     * Returns the primary and sub type (if available).
     * 
     * @return Type.
     */
    public final String getType() {
        if (getSubType() == null) {
            return getPrimaryType();
        }
        return getPrimaryType() + "/" + getSubType();
    }

    /**
     * Returns the version from the parameters.
     * 
     * @return Version or '1.0.0' if not available.
     */
    public final String getVersion() {
        final String parameter = getParameter(VERSION);
        if (parameter == null) {
            return DEFAULT_VERSION;
        }
        return parameter;
    }

    /**
     * Returns the encoding from the parameters.
     * 
     * @return Encoding or 'utf-8' as default if not available.
     */
    public final Charset getEncoding() {
        final String parameter = getParameter(ENCODING);
        if (parameter == null) {
            return Charset.forName(DEFAULT_ENCODING);
        }
        return Charset.forName(parameter);
    }

    // CHECKSTYLE:OFF Generated code

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((getBaseType() == null) ? 0 : getBaseType().hashCode());
        result = prime * result
                + ((getSubType() == null) ? 0 : getSubType().hashCode());
        result = prime * result
                + ((getEncoding() == null) ? 0 : getEncoding().hashCode());
        result = prime * result
                + ((getVersion() == null) ? 0 : getVersion().hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof VersionedMimeType))
            return false;
        VersionedMimeType other = (VersionedMimeType) obj;
        if (getBaseType() == null) {
            if (other.getBaseType() != null)
                return false;
        } else if (!getBaseType().equals(other.getBaseType()))
            return false;
        if (getSubType() == null) {
            if (other.getSubType() != null)
                return false;
        } else if (!getSubType().equals(other.getSubType()))
            return false;
        if (getEncoding() == null) {
            if (other.getEncoding() != null)
                return false;
        } else if (!getEncoding().equals(other.getEncoding()))
            return false;
        if (getVersion() == null) {
            if (other.getVersion() != null)
                return false;
        } else if (!getVersion().equals(other.getVersion()))
            return false;
        return true;
    }

    // CHECKSTYLE:ON

}
