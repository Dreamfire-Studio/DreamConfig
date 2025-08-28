package com.dreamfirestudios.dreamconfig.Object;

import com.dreamfirestudios.dreamconfig.DreamConfig;
import com.dreamfirestudios.dreamconfig.Interface.DreamConfigFooter;
import com.dreamfirestudios.dreamconfig.Interface.DreamConfigHeader;
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

    public DreamConfigObject(JavaPlugin javaPlugin, String configPath, String fileName) {
        if (javaPlugin == null) throw new IllegalArgumentException("javaPlugin cannot be null");

        final File dataFolder = javaPlugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            javaPlugin.getLogger().severe("Could not create plugin data folder: " + dataFolder.getPath());
        }

        final File dir = (configPath == null || configPath.isEmpty())
                ? dataFolder
                : new File(dataFolder, configPath);

        if (!dir.exists() && !dir.mkdirs()) {
            javaPlugin.getLogger().severe("Could not create directory: " + dir.getPath());
        }

        this.file = new File(dir, fileName + ".yml");
        this.isFirstLoad = !file.exists();
        handleFirstLoad(javaPlugin, isFirstLoad);

        this.fileConfiguration = YamlConfiguration.loadConfiguration(file);
        if (isFirstLoad) saveConfig();
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

    private void handleFirstLoad(JavaPlugin javaPlugin, boolean first) {
        if (!first) return;
        Bukkit.getConsoleSender().sendMessage("Creating config for first time: " + file.getPath());
        try {
            final File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!file.createNewFile()) {
                javaPlugin.getLogger().severe("Failed to create config: " + file.getPath());
            }
        } catch (IOException e) {
            javaPlugin.getLogger().severe("Error creating file: " + file.getPath() + " because of error " + e);
        }
    }
}