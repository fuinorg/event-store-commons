package org.fuin.esc.apt.demo;

import org.fuin.esc.api.HasSerializedDataTypeConstant;
import org.fuin.esc.api.SerializedDataType;

@HasSerializedDataTypeConstant("THE_TYPE")
public class DedicatedValueTestClass {

    public static final SerializedDataType THE_TYPE = new SerializedDataType("XYZ");

}
