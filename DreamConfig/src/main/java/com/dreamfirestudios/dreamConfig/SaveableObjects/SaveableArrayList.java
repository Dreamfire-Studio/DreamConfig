package com.dreamfirestudios.dreamConfig.SaveableObjects;

import com.dreamfirestudios.dreamConfig.DeSerializer.DreamConfigDeSerializer;
import com.dreamfirestudios.dreamConfig.Enum.StorageType;
import com.dreamfirestudios.dreamConfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class SaveableArrayList<E> {
    @Getter
    private List<E> arrayList = new ArrayList<E>();
    private final Class<?> classType;

    public SaveableArrayList(Class<?> classType){
        this.classType = classType;
    }

    public ArrayList<Object> Serialize(Function<Object, Object> saveConfigSingle){
        var result = new ArrayList<Object>();
        for(var key : arrayList) result.add(saveConfigSingle.apply(key));
        return result;
    }

    public void DeSerialiseData(StorageType saveableType, List<Object> configData) throws Exception {
        for(var configObject : configData){
            if(IPulseClass.class.isAssignableFrom(classType)){
                var pulseClas = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(classType);
                pulseClas.BeforeLoadConfig();
                Object deSerialised = null;
                if(saveableType == StorageType.CONFIG) deSerialised = DreamConfigDeSerializer.ReturnAllClassFields((HashMap<Object, Object>) configObject, pulseClas.getClass(), pulseClas);
                arrayList.add((E) deSerialised);
                pulseClas.AfterLoadConfig();
            }else{
                if(saveableType == StorageType.CONFIG) arrayList.add((E) DreamConfigDeSerializer.LoadConfigSingle(classType, configObject, configObject));
            }
        }
    }


}
