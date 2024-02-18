package org.fuin.esc.jaxb;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBaseTypeFactory;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscMeta;

/**
 * Creates necessary implementations in the JAX-B module.
 */
public final class BaseTypeFactory implements IBaseTypeFactory {

    @Override
    public IBase64Data createBase64Data(@NotNull byte[] binaryData) {
        return new Base64Data(binaryData);
    }

    @Override
    public IEscMeta createEscMeta(@NotNull String dataType,
                                  @NotNull EnhancedMimeType dataContentType,
                                  @Nullable String metaType,
                                  @Nullable EnhancedMimeType metaContentType,
                                  @Nullable Object meta) {
        return new EscMeta(dataType, dataContentType, metaType, metaContentType, meta);
    }

}
