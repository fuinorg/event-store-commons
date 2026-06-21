package org.fuin.esc.api;

import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

/**
 * Creates the bases types that are only known as interfaces in this module.
 * All implementations are expected to be thread safe.
 */
@ThreadSafe
public interface IBaseTypeFactory {

    /**
     * Constructor with binary data that will be Base64 encoded.
     *
     * @param binaryData Binary data.
     */
    IBase64Data createBase64Data(byte[] binaryData);


    /**
     * Constructor with all data.
     *
     * @param dataType        Type of the data.
     * @param dataContentType Type of the data.
     * @param metaType        Unique name of the meta ata type if available.
     * @param metaContentType Type of the metadata if metadata is available.
     * @param meta            Meta data object if available.
     * @param tenantId        Optional unique tenant identifier.
     */
    IEscMeta createEscMeta(final String dataType,
                           final EnhancedMimeType dataContentType,
                           @Nullable final String metaType,
                           @Nullable final EnhancedMimeType metaContentType,
                           @Nullable final Object meta,
                           @Nullable final TenantId tenantId);

}
