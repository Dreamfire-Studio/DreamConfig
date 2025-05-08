package com.dreamfirestudios.dreamConfig.Interface;

import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;

import java.util.HashMap;

public interface ICustomVariable {
    SaveableHashmap<Object, Object> SerializeData();
    void DeSerializeData(HashMap<Object, Object> configData);
    default void BeforeLoad(){}
    default void AfterLoad(){}
    default void BeforeSave(){}
    default void AfterSave(){}
}
