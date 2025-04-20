# Release Notes

## 0.9.0
- Added new `findAll()` method to [SerializedDataTypeRegistry](api/src/main/java/org/fuin/esc/api/SerializedDataTypeRegistry.java)
- **Incompagible** The [ESGrpcEventStore](esgrpc/src/main/java/org/fuin/esc/esgrpc/ESGrpcEventStore.java) does no longer shutdown the `KurrentDBClient` to allow sharing the client between multiple instances as it is thread-safe.

## 0.8.0

### General
- Dependency updates
- **Incompatible** refactoring of the module structure
  - Several classes were moved to new modules/packages
  - There are multiple new interfaces extending [IBaseType](api/src/main/java/org/fuin/esc/api/IBaseType.java) to support different serialization libraries like JAX-B, JSON-B or Jackson.
- New [HasSerializedDataTypeConstant](api/src/main/java/org/fuin/esc/api/HasSerializedDataTypeConstant.java) that is validated by [HasSerializedDataTypeConstantValidator](api/src/main/java/org/fuin/esc/api/HasSerializedDataTypeConstantValidator.java).
  It allows marking a class that has a public static constant of type [SerializedDataType](api/src/main/java/org/fuin/esc/api/SerializedDataType.java).
  This allows finding these types by the annotation and putting them automated into a [SerializedDataTypeRegistry](api/src/main/java/org/fuin/esc/api/SerializedDataTypeRegistry.java).
- New [SerializedDataType2ClassMapping](api/src/main/java/org/fuin/esc/api/SerializedDataTypesRegistrationRequest.java) that provides a list of classes that are annotated
  with {@link HasSerializedDataTypeConstant} and should be included in the applications {@link SerializedDataTypeRegistry}. 
  
### Jandex
- New [JandexSerializedDataTypeRegistry](client/src/main/java/org/fuin/esc/client/JandexSerializedDataTypeRegistry.java) that provides a registry based on a Jandex index that is searched for [HasSerializedDataTypeConstant](api/src/main/java/org/fuin/esc/api/HasSerializedDataTypeConstant.java) annotations.
