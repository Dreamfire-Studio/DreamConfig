package com.dreamfirestudios.dreamconfig.API;

import com.dreamfirestudios.dreamconfig.DeSerializer.DreamConfigDeSerializer;
import com.dreamfirestudios.dreamconfig.Interface.IDreamConfig;
import com.dreamfirestudios.dreamconfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamconfig.Interface.SaveName;
import com.dreamfirestudios.dreamconfig.Interface.StoragePath;
import com.dreamfirestudios.dreamconfig.Object.DreamConfigObject;
import com.dreamfirestudios.dreamconfig.Interface.ICustomVariable;
import com.dreamfirestudios.dreamconfig.SaveableObjects.SaveableArrayList;
import com.dreamfirestudios.dreamconfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamconfig.SaveableObjects.SaveableLinkedHashMap;
import com.dreamfirestudios.dreamconfig.Serializer.DreamConfigSerializer;
import com.dreamfirestudios.dreamconfig.Serializer.SerializerHelpers;
import com.dreamfirestudios.dreamconfig.Versioning.VersioningUtil;
import com.dreamfirestudios.dreamcore.DreamChat.DreamChat;
import com.dreamfirestudios.dreamcore.DreamChat.DreamMessageSettings;
import com.dreamfirestudios.dreamcore.DreamFile.DreamDir;
import com.dreamfirestudios.dreamcore.DreamVariable.DreamVariableTestAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DreamConfigAPI {

    public static CompletableFuture<HashMap<String, IDreamConfig>> ReturnAllConfigDocuments(JavaPlugin javaPlugin, IDreamConfig templateInstance) {
        return CompletableFuture.supplyAsync(() -> {
            final HashMap<String, IDreamConfig> data = new HashMap<>();
            final File root = new File(GetDreamConfigPath(templateInstance));
            final File[] files = root.listFiles();

            if (files == null || files.length == 0) return data;

            for (File file : files) {
                try {
                    if (file == null || !file.isFile()) continue;
                    final String name = file.getName();
                    if (!name.endsWith(".yml")) continue;

                    final String fileName = name.substring(0, name.length() - 4);
                    final Object instance = SerializerHelpers.CreateInstanceWithID(fileName, templateInstance.getClass());
                    if (!(instance instanceof IDreamConfig cfg)) continue;
                    LoadDreamConfig(javaPlugin, cfg, loaded -> {}).join();
                    data.put(fileName, cfg);
                } catch (Throwable t) {
                    continue;
                }
            }
            return data;
        });
    }

    public static <T extends IDreamConfig> void SaveDreamConfig(JavaPlugin javaPlugin, T iDreamConfig, Consumer<T> onSuccessSave){
        CompletableFuture.runAsync(() -> {
            final String dreamConfigPath = GetDreamConfigPath(iDreamConfig);
            final String documentID = iDreamConfig.documentID();
            final DreamConfigObject dreamConfigObject = new DreamConfigObject(javaPlugin, dreamConfigPath, documentID);
            try (com.dreamfirestudios.dreamconfig.Metrics.MetricsTimer t = com.dreamfirestudios.dreamconfig.Metrics.MetricsTimer.start("dreamconfig.save.duration")) {
                if (dreamConfigObject.IsFirstLoad()) iDreamConfig.FirstLoadConfig();
                DreamConfigSerializer.SaveDreamConfig(iDreamConfig, dreamConfigObject);
                int target = VersioningUtil.targetVersion(iDreamConfig.getClass());
                VersioningUtil.writeStoredVersion(dreamConfigObject, documentID, target);
                onSuccessSave.accept(iDreamConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                com.dreamfirestudios.dreamconfig.Metrics.MetricsRegistry.get().inc("dreamconfig.save.count", 1);
            }
        });
    }

    public static <T extends IDreamConfig> CompletableFuture<Void> LoadDreamConfig(JavaPlugin javaPlugin, T iDreamConfig, Consumer<T> onSuccess) {
        return CompletableFuture.runAsync(() -> {
            final String dreamConfigPath = GetDreamConfigPath(iDreamConfig);
            final String documentID = iDreamConfig.documentID();
            final DreamConfigObject dreamConfigObject = new DreamConfigObject(javaPlugin, dreamConfigPath, documentID);

            try (com.dreamfirestudios.dreamconfig.Metrics.MetricsTimer t = com.dreamfirestudios.dreamconfig.Metrics.MetricsTimer.start("dreamconfig.load.duration")) {
                if (dreamConfigObject.IsFirstLoad()) {
                    iDreamConfig.FirstLoadConfig();
                    com.dreamfirestudios.dreamconfig.Serializer.DreamConfigSerializer.SaveDreamConfig(iDreamConfig, dreamConfigObject);

                    int target = com.dreamfirestudios.dreamconfig.Versioning.VersioningUtil.targetVersion(iDreamConfig.getClass());
                    com.dreamfirestudios.dreamconfig.Versioning.VersioningUtil.writeStoredVersion(dreamConfigObject, documentID, target);

                    onSuccess.accept(iDreamConfig);
                    return;
                }
                com.dreamfirestudios.dreamconfig.DeSerializer.DreamConfigDeSerializer.LoadDreamConfig(iDreamConfig, dreamConfigObject);
                int stored = com.dreamfirestudios.dreamconfig.Versioning.VersioningUtil.readStoredVersion(dreamConfigObject, documentID);
                if (stored < 0) stored = 1; // fallback if missing
                int after = com.dreamfirestudios.dreamconfig.Versioning.MigratorRegistry.applyAll(iDreamConfig, (Class<T>) iDreamConfig.getClass(), stored);
                if (after != stored) {
                    com.dreamfirestudios.dreamconfig.Versioning.VersioningUtil.writeStoredVersion(dreamConfigObject, documentID, after);
                    com.dreamfirestudios.dreamconfig.Serializer.DreamConfigSerializer.SaveDreamConfig(iDreamConfig, dreamConfigObject);
                }
                onSuccess.accept(iDreamConfig);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load DreamConfig: " + documentID, e);
            } finally {
                com.dreamfirestudios.dreamconfig.Metrics.MetricsRegistry.get().inc("dreamconfig.load.count", 1);
            }
        });
    }

    public static <T extends IDreamConfig> void DisplayDreamConfig(T iDreamConfig, Consumer<T> onSuccess, DreamMessageSettings dreamMessageSettings) {
        CompletableFuture.runAsync(() -> {
            try {
                var stringBuilder = new StringBuilder(String.format("==========[%s / PULSE CONFIG]==========\n{\n", iDreamConfig.documentID()));
                var dataFields = SerializerHelpers.ReturnAllFields(iDreamConfig.getClass(), iDreamConfig);
                for (var field : dataFields.keySet()) {
                    var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
                    var fieldValue = ConsoleOutputSingle(dataFields.get(field), 2);
                    if (fieldValue == null) continue;
                    stringBuilder.append(String.format("%s%s:%s\n", ReturnIndent(1), fieldName, fieldValue.toString()));
                }
                stringBuilder.append("}\n==========[END]==========");
                DreamChat.SendMessageToConsole(stringBuilder.toString(), dreamMessageSettings);
                onSuccess.accept(iDreamConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static <T extends IDreamConfig> CompletableFuture<Void> DeleteDreamConfig(JavaPlugin javaPlugin, T iDreamConfig, Consumer<T> onSuccess) {
        return CompletableFuture.runAsync(() -> {
            final String dreamConfigPath = GetDreamConfigPath(iDreamConfig);
            final String documentID = iDreamConfig.documentID();
            final DreamConfigObject dreamConfigObject = new DreamConfigObject(javaPlugin, dreamConfigPath, documentID);
            try (com.dreamfirestudios.dreamconfig.Metrics.MetricsTimer t = com.dreamfirestudios.dreamconfig.Metrics.MetricsTimer.start("dreamconfig.delete.duration")) {
                dreamConfigObject.DeleteConfig();
                onSuccess.accept(iDreamConfig);
            } finally {
                com.dreamfirestudios.dreamconfig.Metrics.MetricsRegistry.get().inc("dreamconfig.delete.count", 1);
            }
        });
    }

    private static String GetDreamConfigPath(IDreamConfig iDreamConfig){
        if(iDreamConfig.getClass().isAnnotationPresent(StoragePath.class)) return iDreamConfig.getClass().getAnnotation(StoragePath.class).value();
        return iDreamConfig.useSubFolder() ? iDreamConfig.documentID() : "";
    }

    private static String ConsoleOutputSingle(Object storedData, int indent) throws Exception {
        if (storedData == null) return null;

        var variableTest = DreamVariableTestAPI.returnTestFromType(storedData.getClass());

        if (storedData instanceof IPulseClass pulseClass) {
            return ConsoleOutputPulseClass(pulseClass, indent + 1);
        } else if (storedData instanceof SaveableHashmap<?, ?> saveableHashmap) {
            return ConsoleOutputSaveableHashmap(saveableHashmap.getHashMap(), indent + 1);
        } else if (storedData instanceof SaveableLinkedHashMap<?, ?> saveableLinkedHashMap) {
            return ConsoleOutputSaveableLinkedHashMap(saveableLinkedHashMap.getHashMap(), indent + 1);
        } else if (storedData instanceof SaveableArrayList<?> saveableArrayList) {
            return ConsoleOutputSaveableArrayList(saveableArrayList, indent + 1);
        } else if (storedData instanceof ICustomVariable customVariable) {
            return ConsoleOutputSaveableLinkedHashMap(customVariable.SerializeData(), indent + 1);
        } else if (storedData instanceof Date date) {
            return date.toString();
        } else if (variableTest != null) {
            Object out = variableTest.SerializeData(storedData);
            return String.format("%s%s", ReturnIndent(indent), String.valueOf(out));
        } else {
            return String.format("%s%s", ReturnIndent(indent), storedData.toString());
        }
    }

    private static String ConsoleOutputPulseClass(IPulseClass pulseClass, int indent) throws Exception {
        var stringBuilder = new StringBuilder("<");
        var dataFields = SerializerHelpers.ReturnAllFields(pulseClass.getClass(), pulseClass);
        for(var field : dataFields.keySet()){
            var fieldName = field.isAnnotationPresent(SaveName.class) ? field.getAnnotation(SaveName.class).value() : field.getName();
            var fieldValue = ConsoleOutputSingle(dataFields.get(field), 0);
            if(fieldValue == null) continue;
            stringBuilder.append(String.format("\n%s%s:%s", ReturnIndent(indent + 1), fieldName, fieldValue));
        }
        stringBuilder.append(String.format("\n%s>", ReturnIndent(indent - 1)));
        return stringBuilder.toString();
    }

    /// <summary>
    /// Pretty-prints any map as JSON-like lines with indentation.
    /// </summary>
    /// <param name="map">Any Map (keys/values of any type)</param>
    /// <param name="indent">Indent level (spaces)</param>
    /// <returns>Formatted string</returns>
    /// <remarks>Accepts wildcard generics to avoid capture cast errors.</remarks>
    private static String ConsoleOutputSaveableHashmap(java.util.Map<?, ?> map, int indent) throws Exception {
        if (map == null || map.isEmpty()) return "{}";
        var sb = new StringBuilder("{");
        for (var key : map.keySet()) {
            var skey  = ConsoleOutputSingle(key, indent);
            var sVal  = ConsoleOutputSingle(map.get(key), indent);
            sb.append(String.format("\n%s%s:%s", ReturnIndent(indent), skey, sVal));
        }
        sb.append(String.format("\n%s}", ReturnIndent(Math.max(0, indent - 1))));
        return sb.toString();
    }

    /// <summary>
    /// Pretty-prints a linked/insertion-ordered map; accepts wildcard generics.
    /// </summary>
    private static String ConsoleOutputSaveableLinkedHashMap(java.util.Map<?, ?> map, int indent) throws Exception {
        if (map == null || map.isEmpty()) return "{}";
        var sb = new StringBuilder("{");
        for (var key : map.keySet()) {
            var skey  = ConsoleOutputSingle(key, indent);
            var sVal  = ConsoleOutputSingle(map.get(key), indent);
            sb.append(String.format("\n%s%s:%s", ReturnIndent(indent), skey, sVal));
        }
        sb.append(String.format("\n%s}", ReturnIndent(Math.max(0, indent - 1))));
        return sb.toString();
    }

    /// <summary>
    /// Pretty-prints a SaveableArrayList; accepts wildcard element type.
    /// </summary>
    private static String ConsoleOutputSaveableArrayList(SaveableArrayList<?> saveableArrayList, int indent) throws Exception {
        if (saveableArrayList == null || saveableArrayList.getArrayList().isEmpty()) return "[]";
        var sb = new StringBuilder("[");
        for (var value : saveableArrayList.getArrayList()) {
            var sValue = ConsoleOutputSingle(value, indent);
            sb.append(String.format("\n%s%s", ReturnIndent(indent), sValue));
        }
        sb.append(String.format("\n%s]", ReturnIndent(Math.max(0, indent - 1))));
        return sb.toString();
    }

    private static String ReturnIndent(int indent){
        return " ".repeat(Math.max(0, indent));
    }
}