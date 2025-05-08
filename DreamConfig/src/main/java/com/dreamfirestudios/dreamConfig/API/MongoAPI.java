package com.dreamfirestudios.dreamConfig.API;

import com.dreamfirestudios.dreamConfig.DeSerializer.MongoDeSerializer;
import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.IPulseMongo;
import com.dreamfirestudios.dreamConfig.Object.MongoConnection;
import com.dreamfirestudios.dreamConfig.Serializer.MongoSerializer;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class MongoAPI {

    public static <T extends IPulseMongo> CompletableFuture<HashMap<String, T>> ReturnAllMongoDocumentsAsync(T iPulseMongo) {
        var data = new HashMap<String, T>();
        var mongoConnection = DreamConfig.GetDreamfireConfig().GetMongoConnection(iPulseMongo.databaseName());
        CompletableFuture<HashMap<String, T>> future = new CompletableFuture<>();
        mongoConnection.getAll(iPulseMongo.collectionName(),
                documents -> {
                    List<CompletableFuture<Void>> loadFutures = new ArrayList<>();
                    for (var document : documents) {
                        if (!document.containsKey(MongoConnection.DefaultKey))  continue;
                        var fileName = document.get(MongoConnection.DefaultKey).toString();
                        var newInstance = SerializerHelpers.CreateInstanceWithID(fileName, iPulseMongo.getClass());
                        if (newInstance == null)  continue;
                        var instance = (T) newInstance;
                        CompletableFuture<Void> loadFuture = new CompletableFuture<>();
                        loadFutures.add(loadFuture);
                        Load(false, instance,
                                loadedInstance -> {
                                    try {
                                        data.put(fileName, loadedInstance);
                                        loadFuture.complete(null);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        loadFuture.completeExceptionally(e);
                                    }
                                },
                                throwable -> {
                                    throwable.printStackTrace();
                                    loadFuture.completeExceptionally(throwable);
                                }
                        );
                    }

                    CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]))
                            .thenRun(() -> {
                                future.complete(data);
                            })
                            .exceptionally(throwable -> {
                                throwable.printStackTrace();
                                future.completeExceptionally(throwable);
                                return null;
                            });
                },
                throwable -> {
                    throwable.printStackTrace();
                    future.completeExceptionally(throwable);
                });

        return future;
    }

    public static <T extends IPulseMongo> void Save(boolean replace, T iPulseMongo, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        var mongoConnection = DreamConfig.GetDreamfireConfig().GetMongoConnection(iPulseMongo.databaseName());
        mongoConnection.countDocuments(iPulseMongo.collectionName(), null, iPulseMongo.documentID(),
                documentCount -> {
                    if (documentCount == 0) {
                        iPulseMongo.FirstLoadMongo();
                        try {
                            MongoSerializer.SaveMongo(iPulseMongo, mongoConnection, onSuccess, onError);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else if (replace) {
                        try {
                            MongoSerializer.SaveMongo(iPulseMongo, mongoConnection, onSuccess, onError);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                },
                onError
        );
    }

    public static <T extends IPulseMongo> void Load(boolean replace, T iPulseMongo, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        var mongoConnection = DreamConfig.GetDreamfireConfig().GetMongoConnection(iPulseMongo.databaseName());
        mongoConnection.countDocuments(iPulseMongo.collectionName(), null, iPulseMongo.documentID(),
                documentCount -> {
                    if (documentCount == 0) {
                        iPulseMongo.FirstLoadMongo();
                        Save(replace, iPulseMongo, onSuccess, onError);
                    } else {
                        MongoDeSerializer.LoadMongo(iPulseMongo, mongoConnection, onSuccess, onError);
                    }
                },
                onError
        );
    }

    public static <T extends IPulseMongo> void Delete(T iPulseMongo, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        var mongoConnection = DreamConfig.GetDreamfireConfig().GetMongoConnection(iPulseMongo.databaseName());
        mongoConnection.delete(iPulseMongo.collectionName(), null, iPulseMongo.documentID(),
                () -> {
                    DreamConfig.GetDreamfireConfig().DeleteDynamicPulseMongo(iPulseMongo.documentID());
                    DreamConfig.GetDreamfireConfig().DeleteStaticPulseMongo(iPulseMongo.getClass().getSimpleName());
                    onSuccess.accept(iPulseMongo);
                },
                onError
        );
    }
}