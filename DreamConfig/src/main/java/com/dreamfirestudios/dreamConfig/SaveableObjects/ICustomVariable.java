package com.dreamfirestudios.dreamConfig.SaveableObjects;

import java.util.HashMap;
import java.util.LinkedHashMap;

public interface ICustomVariable {
    LinkedHashMap<Object, Object> SerializeData();
    void DeSerializeData(LinkedHashMap<Object, Object> configData);
    default void BeforeLoad(){}
    default void AfterLoad(){}
    default void BeforeSave(){}
    default void AfterSave(){}
}
