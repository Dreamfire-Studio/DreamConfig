package com.dreamfirestudios.dreamConfig.SaveableObjects;

import com.dreamfirestudios.dreamConfig.Interface.ICustomVariable;
import com.google.gson.Gson;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;

public class SaveablePlayerInventory implements ICustomVariable {
    private ItemStack[] inventoryContents = new ItemStack[]{};

    public SaveablePlayerInventory() {}
    public SaveablePlayerInventory(Inventory inventory) {
        if (inventory instanceof PlayerInventory playerInventory) {
            this.inventoryContents = playerInventory.getContents();
        }
    }

    @Override
    public SaveableHashmap<Object, Object> SerializeData() {
        var serializedData = new SaveableHashmap<>(Object.class, Object.class);
        if (inventoryContents != null) {
            serializedData.getHashMap().put("inventorySize", String.valueOf(inventoryContents.length));
            for (int i = 0; i < inventoryContents.length; i++) {
                if (inventoryContents[i] != null) {
                    serializedData.getHashMap().put(String.valueOf(i), new Gson().toJson(inventoryContents[i]));
                }
            }
        }
        return serializedData;
    }

    @Override
    public void DeSerializeData(HashMap<Object, Object> hashMap) {
        if (hashMap.containsKey("inventorySize")) {
            int inventorySize = Integer.parseInt((String) hashMap.get("inventorySize"));
            inventoryContents = new ItemStack[inventorySize];

            for (int i = 0; i < inventorySize; i++) {
                String key = String.valueOf(i);
                if (hashMap.containsKey(key)) {
                    var map = new Gson().fromJson(hashMap.get(key).toString(), Map.class);
                    inventoryContents[i] = org.bukkit.inventory.ItemStack.deserialize(map);
                }
            }
        }
    }

    public void SetContents(Player player) {
        player.getInventory().setContents(inventoryContents);
    }
}
