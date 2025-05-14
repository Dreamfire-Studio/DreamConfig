package com.dreamfirestudios.dreamConfig.Object;

import com.dreamfirestudios.dreamConfig.DreamConfig;
import com.dreamfirestudios.dreamConfig.Interface.DreamConfigFooter;
import com.dreamfirestudios.dreamConfig.Interface.DreamConfigHeader;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class DreamConfigObject {
    private final File file;
    private final FileConfiguration fileConfiguration;
    private final boolean isFirstLoad;

    public DreamConfigObject(JavaPlugin javaPlugin, String configPath, String fileName){
        if(javaPlugin == null) javaPlugin = DreamConfig.GetDreamConfig();
        var dir = new File(javaPlugin.getDataFolder(), configPath);
        if(!dir.exists()){
            if(!dir.mkdir()) javaPlugin.getLogger().severe(String.format("Could not create directory: %s", dir.getPath()));
        }
        file = new File(dir, fileName + ".yml");
        isFirstLoad = !file.exists();
        HandleFirstLoad(javaPlugin, isFirstLoad);
        fileConfiguration = YamlConfiguration.loadConfiguration(file);
        if(isFirstLoad) saveConfig();
    }

    public HashMap<Object, Object> HashMap(String path, boolean deepDive){
        var data = new HashMap<Object, Object>();
        var section = fileConfiguration.getConfigurationSection(path);
        if(section == null) return data;
        for(var key : section.getKeys(false)){
            var fullPath = String.format("%s.%s", path, key);
            if(!fileConfiguration.isConfigurationSection(fullPath)) data.put(key, Get(fullPath));
            else if(fileConfiguration.isConfigurationSection(fullPath) && deepDive) data.put(key, HashMap(fullPath, deepDive));
        }
        return data;
    }

    public Object Get(String path){
        return fileConfiguration.get(path);
    }

    public boolean IsFirstLoad(){
        return isFirstLoad;
    }

    public void saveConfig() {
        try {fileConfiguration.save(file);}
        catch (IOException e) {throw new RuntimeException(e);}
    }

    public void DeleteConfig(){
        file.delete();
    }

    public void setHeader(DreamConfigHeader dreamConfigHeader){
        var data = new ArrayList<String>();
        data.add("# +----------------------------------------------------+ #");
        data.addAll(Arrays.asList(dreamConfigHeader.value()));
        data.add("# +----------------------------------------------------+ #");
        fileConfiguration.options().setHeader(data);
        saveConfig();
    }

    public void setFooter(DreamConfigFooter dreamConfigFooter){
        var data = new ArrayList<String>();
        data.add("# +----------------------------------------------------+ #");
        data.addAll(Arrays.asList(dreamConfigFooter.value()));
        data.add("# +----------------------------------------------------+ #");
        fileConfiguration.options().setFooter(data);
        saveConfig();
    }

    public void set(String path, Object value){
        fileConfiguration.set(path, value);
        saveConfig();
    }

    private void HandleFirstLoad(JavaPlugin javaPlugin, boolean isFirstLoad){
        if(isFirstLoad){
            Bukkit.getConsoleSender().sendMessage(String.format("Creating config for first time: %s", file.getPath()));
            try {
                if(!file.createNewFile()) javaPlugin.getLogger().severe(String.format("Failed to create config for: %s", file.getPath()));
            }catch (IOException e) {
                javaPlugin.getLogger().severe(String.format("Error creating file: %s because of error %s", file.getPath(), e));
            }
        }
    }
}
