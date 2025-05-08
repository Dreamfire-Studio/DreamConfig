package com.dreamfirestudios.dreamConfig.SaveableObjects;

import com.dreamfirestudios.dreamConfig.Enum.SaveabaleItemStackKeys;
import com.dreamfirestudios.dreamConfig.Interface.ICustomVariable;
import com.google.gson.Gson;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SavebaleItemStack implements ICustomVariable {

    public ItemStack itemStack;

    public SavebaleItemStack(){}
    public SavebaleItemStack(ItemStack itemStack){
        this.itemStack = itemStack;
    }

    @Override
    public SaveableHashmap<Object, Object> SerializeData() {
        var data = new SaveableHashmap<>(Object.class, Object.class);
        data.getHashMap().put(SaveabaleItemStackKeys.ITEMSTACK, new Gson().toJson(itemStack.serialize()));
        return data;
    }

    @Override
    public void DeSerializeData(HashMap<Object, Object> configData) {
        var map = new Gson().fromJson(configData.get(SaveabaleItemStackKeys.ITEMSTACK).toString(), Map.class);
        itemStack = org.bukkit.inventory.ItemStack.deserialize(map);
    }
}
