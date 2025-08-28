package com.dreamfirestudios.dreamconfig.Versioning;

/**
 * <summary>Internal metadata keys persisted alongside a config document.</summary>
 */
public final class MetadataKeys {
    private MetadataKeys() {}

    /// <summary>Path under the document root used to store the schema version.</summary>
    public static final String META_VERSION = "__meta.version";
}