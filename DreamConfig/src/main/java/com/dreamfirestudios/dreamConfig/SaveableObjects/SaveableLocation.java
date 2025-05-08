package com.dreamfirestudios.dreamConfig.SaveableObjects;

import com.dreamfirestudios.dreamConfig.Enum.SaveableLocationKeys;
import com.dreamfirestudios.dreamConfig.Interface.ICustomVariable;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.UUID;

public class SaveableLocation implements ICustomVariable {

    public Location location;

    public SaveableLocation(){}
    public SaveableLocation(Location location){
        this.location = location;
    }

    @Override
    public SaveableHashmap<Object, Object> SerializeData() {
        var data = new SaveableHashmap<>(Object.class, Object.class);
        data.getHashMap().put(SaveableLocationKeys.WORLD_UUID.name(), location.getWorld().getUID().toString());
        data.getHashMap().put(SaveableLocationKeys.WORLD_X.name(), location.getX());
        data.getHashMap().put(SaveableLocationKeys.WORLD_Y.name(), location.getY());
        data.getHashMap().put(SaveableLocationKeys.WORLD_Z.name(), location.getZ());
        return data;
    }

    @Override
    public void DeSerializeData(HashMap<Object, Object> configData) {
        var worldUUID = UUID.fromString(configData.get(SaveableLocationKeys.WORLD_UUID.name()).toString());
        var x = (double) configData.get(SaveableLocationKeys.WORLD_X.name());
        var y = (double) configData.get(SaveableLocationKeys.WORLD_Y.name());
        var z = (double) configData.get(SaveableLocationKeys.WORLD_Z.name());
        var world = Bukkit.getWorld(worldUUID);
        location = new Location(world, x, y, z);
    }

    @Override
    public String toString() {
        return String.format("%s:%f:%f:%f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }
}
