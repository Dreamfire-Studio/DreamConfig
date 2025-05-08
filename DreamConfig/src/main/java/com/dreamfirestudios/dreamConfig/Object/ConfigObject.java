package com.dreamfirestudios.dreamConfig.Object;

import com.dreamfirestudios.dreamConfig.Interface.ConfigFooter;
import com.dreamfirestudios.dreamConfig.Interface.ConfigHeader;
import com.dreamfirestudios.dreamCore.DreamfireFile.DreamfireDir;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ConfigObject {
    private boolean saveFlag;
    private FileConfiguration fileConfiguration;
    private File file;

    public ConfigObject(String configPath, String fileName){
        com.dreamfirestudios.dreamCore.DreamfireFile.DreamfireDir.createDirectory(new File(configPath));
        var filePath = String.format("%s/%s.yml", configPath, fileName);
        file = new File(filePath);
        saveFlag = file.exists();
        fileConfiguration = YamlConfiguration.loadConfiguration(file);
        SaveConfig();
    }

    public void Set(String path, Object value){
        fileConfiguration.set(path, value);
        SaveConfig();
    }

    public Object Get(String path){
        return fileConfiguration.get(path);
    }

    public boolean FirstSave(){
        return !saveFlag;
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

    public void SetHeader(ConfigHeader configHeader){
        var data = new ArrayList<String>();
        data.add("# +----------------------------------------------------+ #");
        data.addAll(Arrays.asList(configHeader.value()));
        data.add("# +----------------------------------------------------+ #");
        fileConfiguration.options().setHeader(data);
        SaveConfig();
    }

    public void SetFooter(ConfigFooter configFooter){
        var data = new ArrayList<String>();
        data.add("# +----------------------------------------------------+ #");
        data.addAll(Arrays.asList(configFooter.value()));
        data.add("# +----------------------------------------------------+ #");
        fileConfiguration.options().setFooter(data);
        SaveConfig();
    }

    public void SaveConfig(){
        try { fileConfiguration.save(file); }
        catch (IOException e) { e.printStackTrace();}
    }

    public void DeleteConfig(){
        file.delete();
    }

    public void ClearConfig(){
        for(var s : fileConfiguration.getKeys(false)) Set(s, null);
    }
}
