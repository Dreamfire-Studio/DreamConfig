package com.dreamfirestudios.dreamconfig.Interface;

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