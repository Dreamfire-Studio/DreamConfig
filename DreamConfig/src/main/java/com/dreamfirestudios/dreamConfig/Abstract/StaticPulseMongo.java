package com.dreamfirestudios.dreamConfig.Abstract;

import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.IPulseMongo;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public abstract class StaticPulseMongo<T extends StaticPulseMongo<T>> implements IPulseMongo {

    public static <T extends StaticPulseMongo<T>> T ReturnStatic(Class<T> clazz, Consumer<IPulseMongo> onSuccess , Consumer<Throwable> onError){
        var stored = (T) DreamConfig.GetDreamfireConfig().GetStaticPulseMongo(clazz.getSimpleName());
        if (stored != null) return stored;
        try {
            T newInstance = clazz.getDeclaredConstructor().newInstance();
            newInstance.SaveMongo(true, onSuccess, onError);
            DreamConfig.GetDreamfireConfig().SetStaticPulseMongo(clazz.getSimpleName(), newInstance);
            return newInstance;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String documentID() {return getClass().getSimpleName();}

    @Override
    public String collectionName() {
        return StaticPulseMongo.class.getSimpleName();
    }
}
