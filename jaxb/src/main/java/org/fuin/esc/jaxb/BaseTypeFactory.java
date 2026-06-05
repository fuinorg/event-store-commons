package org.fuin.esc.jaxb;

import org.jspecify.annotations.Nullable;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IBaseTypeFactory;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.TenantId;

/**
 * Creates necessary implementations in the JAX-B module.
 */
public final class BaseTypeFactory implements IBaseTypeFactory {

    @Override
    public IBase64Data createBase64Data(byte[] binaryData) {
        return new Base64Data(binaryData);
    }

    @Override
    public IEscMeta createEscMeta(String dataType,
                                  EnhancedMimeType dataContentType,
                                  @Nullable String metaType,
                                  @Nullable EnhancedMimeType metaContentType,
                                  @Nullable Object meta,
                                  @Nullable TenantId tenantId) {
        return new EscMeta(dataType, dataContentType, metaType, metaContentType, meta, tenantId);
    }

}
