package com.dreamfirestudios.dreamConfig.SaveableObjects;

import com.dreamfirestudios.dreamConfig.Enum.SaveAbleInventoryKeys;
import com.dreamfirestudios.dreamConfig.Interface.DontSave;
import com.dreamfirestudios.dreamConfig.Interface.ICustomVariable;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SaveAbleInventory implements ICustomVariable {
    @DontSave
    @Getter
    private Inventory inventory;
    private String inventoryTitle;

    public SaveAbleInventory(){}
    public SaveAbleInventory(String inventoryTitle, int inventorySize){
        inventory = Bukkit.createInventory(null, inventorySize, inventoryTitle);
        this.inventoryTitle = inventoryTitle;
    }

    public SaveAbleInventory(Inventory inventory){
        this.inventory = inventory;
        this.inventoryTitle = inventory.getType().getDefaultTitle();
    }

    @Override
    public SaveableHashmap<Object, Object> SerializeData() {
        var data = new SaveableHashmap<Object, Object>(Object.class, Object.class);
        data.getHashMap().put(SaveAbleInventoryKeys.CONTENTS, Arrays.asList(inventory.getContents()));
        data.getHashMap().put(SaveAbleInventoryKeys.SIZE, inventory.getSize());
        data.getHashMap().put(SaveAbleInventoryKeys.TITLE, inventoryTitle);
        return data;
    }

    @Override
    public void DeSerializeData(HashMap<Object, Object> hashMap) {
        inventoryTitle = (String) hashMap.get(SaveAbleInventoryKeys.TITLE);
        inventory = Bukkit.createInventory(null, (Integer) hashMap.get(SaveAbleInventoryKeys.SIZE), inventoryTitle);
        inventory.setContents(((List<ItemStack>) hashMap.get(SaveAbleInventoryKeys.CONTENTS)).toArray(new ItemStack[0]));
    }
}
