package com.dreamfirestudios.dreamconfig.SaveableObjects;

import com.dreamfirestudios.dreamconfig.DeSerializer.DreamConfigDeSerializer;
import com.dreamfirestudios.dreamconfig.Enum.StorageType;
import com.dreamfirestudios.dreamconfig.Interface.IPulseClass;
import com.dreamfirestudios.dreamconfig.Serializer.SerializerHelpers;
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
        for (Object raw : configData) {
            if (IPulseClass.class.isAssignableFrom(classType)) {
                IPulseClass pulse = (IPulseClass) SerializerHelpers.CreateClassInstanceBlank(classType);
                if (pulse == null) continue;
                pulse.BeforeLoadConfig();

                Object populated = null;
                if (saveableType == StorageType.CONFIG) {
                    populated = DreamConfigDeSerializer.ReturnAllClassFields((HashMap<Object, Object>) raw, pulse.getClass(), pulse);
                }

                pulse.AfterLoadConfig();
                if (populated != null) arrayList.add((E) populated);
            } else {
                if (saveableType == StorageType.CONFIG) {
                    Object value = DreamConfigDeSerializer.LoadConfigSingle(classType, raw, raw);
                    if (value != null) arrayList.add((E) value);
                }
            }
        }
    }
}