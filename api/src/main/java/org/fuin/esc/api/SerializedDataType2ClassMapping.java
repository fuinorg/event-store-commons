package org.fuin.esc.api;

/**
 * Maps a type to a class.
 *
 * @param type  Type that is used as unique name for the class.
 * @param clasz Class that is represented by the type.
 */
public record SerializedDataType2ClassMapping(SerializedDataType type, Class<?> clasz) {
}
