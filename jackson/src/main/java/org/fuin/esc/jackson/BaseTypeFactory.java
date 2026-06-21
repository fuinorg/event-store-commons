package org.fuin.esc.jackson;

import org.fuin.esc.api.*;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

/**
 * Creates necessary implementations with Jackson.
 */
@ThreadSafe
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
