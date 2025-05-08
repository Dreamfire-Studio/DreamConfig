package com.dreamfirestudios.dreamConfig.SaveableObjects;

import com.dreamfirestudios.dreamConfig.DeSerializer.ConfigDeSerializer;
import com.dreamfirestudios.dreamConfig.DeSerializer.MongoDeSerializer;
import com.dreamfirestudios.dreamConfig.Enum.StorageType;
import com.dreamfirestudios.dreamConfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamConfig.Serializer.SerializerHelpers;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SaveableArrayList<E> {
    @Getter
    private List<E> arrayList = new ArrayList<E>();
    private final Class<?> classType;
    public SaveableArrayList(Class<?> classType){
        this.classType = classType;
    }

    public void DeSerialiseData(StorageType saveableType, List<Object> configData) throws Exception {
        for(var configObject : configData){
            if(IPulseClass.class.isAssignableFrom(classType)){
                var pulseClas = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(classType);
                pulseClas.BeforeLoadConfig();
                Object deSerialised = null;
                if(saveableType == StorageType.CONFIG) deSerialised = ConfigDeSerializer.ReturnClassFields((HashMap<Object, Object>) configObject, pulseClas.getClass(), pulseClas);
                else if(saveableType == StorageType.MONGO) deSerialised = MongoDeSerializer.ReturnClassFieldsMap((Document) configObject, pulseClas.getClass(), pulseClas);
                arrayList.add((E) deSerialised);
                pulseClas.AfterLoadConfig();
            }else{
                if(saveableType == StorageType.CONFIG) arrayList.add((E) ConfigDeSerializer.LoadConfigSingle(classType, configObject, configObject));
                else if(saveableType == StorageType.MONGO) arrayList.add((E) MongoDeSerializer.LoadMongoSingle(classType, configObject, configObject));
            }
        }
    }
}
