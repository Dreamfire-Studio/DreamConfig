package com.dreamfirestudios.dreamConfig.Object;

import com.dreamfirestudios.dreamConfig.CustomSubscriberAdapter;
import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.IPulseMongo;
import com.dreamfirestudios.dreamConfig.SubscriberAdapter;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.Getter;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

//Create a static create method
public class MongoConnection {

    public static MongoConnection CreateConnection(String mongoIP, String databaseName){
        var mongoConnection = new MongoConnection(mongoIP, databaseName);
        DreamConfig.GetDreamfireConfig().SetMongoConnection(mongoConnection.databaseName, mongoConnection);
        return mongoConnection;
    }

    public static final String DefaultKey = "MongoID";
    private final MongoDatabase mongoDatabase;
    @Getter
    private final String databaseName;

    private MongoConnection(String mongoIP, String databaseName) {
        var mongoClient = MongoClients.create(mongoIP);
        this.mongoDatabase = mongoClient.getDatabase(databaseName);
        this.databaseName = databaseName;
    }

    public void getOne(String collectionName, String key, Object value, Consumer<Document> onSuccess, Consumer<Throwable> onError) {
        if (key == null) key = DefaultKey;

        mongoDatabase.getCollection(collectionName)
                .find(Filters.eq(key, value))
                .first()
                .subscribe(new SubscriberAdapter<>(
                        onSuccess::accept,
                        onError::accept,
                        () -> {
                            onSuccess.accept(null);
                        }
                ));
    }

    public void getAll(String collectionName, Consumer<List<Document>> onSuccess, Consumer<Throwable> onError) {
        var documents = new ArrayList<Document>();
        mongoDatabase.getCollection(collectionName).find()
                .subscribe(new CustomSubscriberAdapter<Document>(
                        documents::add,
                        onError,
                        () -> {
                            onSuccess.accept(documents); // Ensure this is called, even for an empty list
                        }
                ));
    }

    public void countDocuments(String collectionName, String key, String value, Consumer<Long> onSuccess, Consumer<Throwable> onError) {
        if (key == null) key = DefaultKey;

        mongoDatabase.getCollection(collectionName)
                .countDocuments(Filters.eq(key, value))
                .subscribe(new SubscriberAdapter<>(
                        count -> onSuccess.accept(count),
                        throwable -> onError.accept(throwable),
                        () -> {} // No additional action on complete
                ));
    }

    public void insertOrReplace(String collectionName, String key, String value, Document document, Runnable onSuccess, Consumer<Throwable> onError) {
        final String effectiveKey = (key == null) ? DefaultKey : key;

        if (value == null) {
            onError.accept(new IllegalArgumentException("Value cannot be null"));
            return;
        }

        getOne(collectionName, effectiveKey, value,
                existingDocument -> {
                    if (existingDocument != null) {
                        // Document exists, update it
                        Document updatedDocument = new Document(document);
                        updatedDocument.remove("_id"); // Avoid conflicts with MongoDB's _id field
                        replace(collectionName, Filters.eq(effectiveKey, value), updatedDocument, onSuccess, onError);
                    } else {
                        // Document does not exist, insert a new one
                        Document newDocument = new Document(document);
                        newDocument.remove("_id"); // Avoid conflicts with MongoDB's _id field
                        newDocument.append(effectiveKey, value); // Ensure the key-value pair is included
                        insert(collectionName, newDocument, onSuccess, onError);
                    }
                },
                onError
        );
    }

    public void insert(String collectionName, Document document, Runnable onSuccess, Consumer<Throwable> onError) {
        mongoDatabase.getCollection(collectionName).insertOne(document)
                .subscribe(new SubscriberAdapter<>(
                        result -> onSuccess.run(),
                        throwable -> onError.accept(throwable),
                        () -> {} // No additional action on complete
                ));
    }

    public void replace(String collectionName, Bson filter, Document replacement, Runnable onSuccess, Consumer<Throwable> onError) {
        mongoDatabase.getCollection(collectionName).replaceOne(filter, replacement)
                .subscribe(new SubscriberAdapter<>(
                        result -> onSuccess.run(),
                        throwable -> onError.accept(throwable),
                        () -> {} // No additional action on complete
                ));
    }

    public void delete(String collectionName, String key, Object value, Runnable onSuccess, Consumer<Throwable> onError) {
        if (key == null) key = DefaultKey;

        mongoDatabase.getCollection(collectionName).deleteOne(Filters.eq(key, value))
                .subscribe(new SubscriberAdapter<DeleteResult>(
                        result -> onSuccess.run(),
                        throwable -> onError.accept(throwable),
                        () -> {} // No additional action on complete
                ));
    }

    public Document defaultDocument(IPulseMongo pulseMongo) {
        if (pulseMongo == null || pulseMongo.documentID() == null) {
            throw new IllegalArgumentException("PulseMongo or its documentID cannot be null");
        }
        return new Document(DefaultKey, pulseMongo.documentID());
    }
}