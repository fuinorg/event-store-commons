package org.fuin.esc.api;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * Creates the bases types that are only known as interfaces in this module.
 */
public interface IBaseTypeFactory {

    /**
     * Constructor with binary data that will be Base64 encoded.
     *
     * @param binaryData Binary data.
     */
    IBase64Data createBase64Data(@NotNull byte[] binaryData);


    /**
     * Constructor with all data.
     *
     * @param dataType        Type of the data.
     * @param dataContentType Type of the data.
     * @param metaType        Unique name of the meta data type if available.
     * @param metaContentType Type of the meta data if meta data is available.
     * @param meta            Meta data object if available.
     */
    IEscMeta createEscMeta(@NotNull final String dataType,
                           @NotNull final EnhancedMimeType dataContentType,
                           @Nullable final String metaType,
                           @Nullable final EnhancedMimeType metaContentType,
                           @Nullable final Object meta);

}
