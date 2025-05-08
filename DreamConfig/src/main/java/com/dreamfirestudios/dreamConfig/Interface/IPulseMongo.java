package com.dreamfirestudios.dreamConfig.Interface;

import com.dreamfirestudios.dreamConfig.API.MongoAPI;
import com.dreamfirestudios.dreamConfig.Console.MongoConsole;
import com.dreamfirestudios.dreamCore.DreamfireChat.DreamfireChat;

import java.util.function.Consumer;

public interface IPulseMongo {
    String databaseName();
    String documentID();
    default String collectionName(){return getClass().getSimpleName();}
    default void FirstLoadMongo(){}
    default void BeforeLoadMongo(){}
    default void AfterLoadMongo(){}
    default void BeforeSaveMongo(){}
    default void AfterSaveMongo(){}
    default <T extends IPulseMongo> void SaveMongo(boolean replace, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        @SuppressWarnings("unchecked")
        T self = (T) this;
        MongoAPI.Save(replace, self, onSuccess, onError);
    }
    default <T extends IPulseMongo> void LoadMongo(boolean replace, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        @SuppressWarnings("unchecked")
        T self = (T) this; // Cast `this` to T
        MongoAPI.Load(replace, self, onSuccess, onError);
    }

    default <T extends IPulseMongo> void DeleteMongo(Consumer<T> onSuccess, Consumer<Throwable> onError) {
        @SuppressWarnings("unchecked")
        T self = (T) this; // Cast `this` to T
        MongoAPI.Delete(self, onSuccess, onError);
    }

    default <T extends IPulseMongo> void DisplayMongo() throws Exception {
        @SuppressWarnings("unchecked")
        T self = (T) this; // Cast `this` to T
        DreamfireChat.SendMessageToConsole(MongoConsole.ConsoleOutput(self));
    }
}
