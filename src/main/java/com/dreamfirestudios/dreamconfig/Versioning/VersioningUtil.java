package com.dreamfirestudios.dreamconfig.Versioning;

import com.dreamfirestudios.dreamconfig.Interface.ConfigVersion;
import com.dreamfirestudios.dreamconfig.Object.DreamConfigObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <summary>Utilities for reading/writing version metadata and running migrations.</summary>
 */
public final class VersioningUtil {
    private VersioningUtil() {}

    /**
     * <summary>Return the target version for a config class (default 1 if no annotation).</summary>
     */
    public static int targetVersion(Class<?> configClass) {
        ConfigVersion cv = configClass.getAnnotation(ConfigVersion.class);
        return (cv == null) ? 1 : Math.max(1, cv.value());
    }

    /**
     * <summary>Read stored version from the backing YAML: {@code &lt;docId&gt;.__meta.version}.</summary>
     */
    public static int readStoredVersion(DreamConfigObject cfg, String docId) {
        Object v = cfg.Get(docId + "." + MetadataKeys.META_VERSION);
        if (v instanceof Number n) return n.intValue();
        try {
            return v != null ? Integer.parseInt(String.valueOf(v)) : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * <summary>
     * Write stored version without clobbering the entire document.
     * </summary>
     * <remarks>
     * Avoid dotted-path {@code set()} (which replaces the whole {@code docId} section).
     * Instead: load the section map, merge {@code __meta.version}, then set the whole section back.
     * </remarks>
     */
    @SuppressWarnings("unchecked")
    public static void writeStoredVersion(DreamConfigObject cfg, String docId, int version) {
        // Split "__meta.version" into ["__meta","version"] so we can merge maps safely
        final String metaPath = MetadataKeys.META_VERSION;
        final int dot = metaPath.lastIndexOf('.');
        final String metaRoot = (dot > 0) ? metaPath.substring(0, dot) : "__meta";
        final String metaKey  = (dot > 0) ? metaPath.substring(dot + 1) : metaPath;

        // Get existing document section
        Object sectionObj = cfg.Get(docId);
        LinkedHashMap<String, Object> section = new LinkedHashMap<>();

        if (sectionObj instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                section.put(String.valueOf(e.getKey()), e.getValue());
            }
        }

        // Get or create __meta map
        Object metaObj = section.get(metaRoot);
        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        if (metaObj instanceof Map<?, ?> mm) {
            for (Map.Entry<?, ?> e : mm.entrySet()) {
                meta.put(String.valueOf(e.getKey()), e.getValue());
            }
        }

        // Update version
        meta.put(metaKey, version);
        section.put(metaRoot, meta);

        // Write the WHOLE section back (no dotted path) to preserve other keys
        cfg.set(docId, section);
    }
}