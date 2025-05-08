package com.dreamfirestudios.dreamConfig.Interface;

public interface IPulseClass {
    default void FirstLoadConfig(){}
    default void BeforeLoadConfig(){}
    default void AfterLoadConfig(){}
    default void BeforeSaveConfig(){}
    default void AfterSaveConfig(){}
}
