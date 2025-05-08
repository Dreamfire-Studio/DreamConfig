package com.dreamfirestudios.dreamConfig.Abstract;

import com.dreamfirestudios.dreamConfig.Interface.DontDefault;
import com.dreamfirestudios.dreamConfig.Interface.StorageComment;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableHashmap;
import com.dreamfirestudios.dreamConfig.SaveableObjects.SaveableLinkedHashMap;
import org.bukkit.inventory.ItemStack;

public abstract class StaticEnumPulseConfig<T extends StaticEnumPulseConfig<T, k, v>, k extends Enum<k>, v> extends StaticPulseConfig<T>{
    protected abstract Class<k> getKeyClass();
    protected abstract Class<v> getValueClass();
    protected abstract v getDefaultValueFor(k key);

    public SaveableLinkedHashMap<k, v> saveableHashmap = new SaveableLinkedHashMap<>(getKeyClass(), getValueClass());

    public v GetValue(k key){
        var storedValue = saveableHashmap.getHashMap().getOrDefault(key, null);
        return storedValue == null ? getDefaultValueFor(key) : storedValue;
    }

    public StaticEnumPulseConfig(){
        for (k enumValue : (getKeyClass()).getEnumConstants()) {
            if (!saveableHashmap.getHashMap().containsKey(enumValue)){
                saveableHashmap.getHashMap().put(enumValue, getDefaultValueFor(enumValue));
            }
        }
    }

    @Override
    public void FirstLoadConfig() {
        for (k enumValue : (getKeyClass()).getEnumConstants()) {
            if (!saveableHashmap.getHashMap().containsKey(enumValue)){
                saveableHashmap.getHashMap().put(enumValue, getDefaultValueFor(enumValue));
            }
        }
    }

    @Override
    public void AfterLoadConfig() {
        for (k enumValue : (getKeyClass()).getEnumConstants()) {
            if (!saveableHashmap.getHashMap().containsKey(enumValue)){
                saveableHashmap.getHashMap().put(enumValue, getDefaultValueFor(enumValue));
            }
        }
        SaveConfig(iPulseConfig -> {}, Throwable::printStackTrace);
    }

    @Override
    public void BeforeSaveConfig() {
        for (k enumValue : (getKeyClass()).getEnumConstants()) {
            if (!saveableHashmap.getHashMap().containsKey(enumValue)){
                saveableHashmap.getHashMap().put(enumValue, getDefaultValueFor(enumValue));
            }
        }
    }
}
